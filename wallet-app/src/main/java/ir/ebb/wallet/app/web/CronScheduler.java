package ir.ebb.wallet.app.web;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.actor.typed.ActorSystem;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Replaces Spring {@code @Scheduled} + ShedLock (single-node, so no distributed
 * lock needed). Schedules a {@link Runnable} at a Spring-style 6-field cron
 * expression, re-arming a one-shot Pekko timer after each fire. A small jitter is
 * added so jobs don't all land on the same instant.
 */
@Slf4j
public class CronScheduler {

    private static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");
    private static final CronParser PARSER =
            new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING));

    private final ActorSystem<?> system;

    public CronScheduler(ActorSystem<?> system) {
        this.system = system;
    }

    public void schedule(String name, String cronExpression, Runnable task) {
        Cron cron = PARSER.parse(cronExpression);
        arm(name, cron, task);
        log.info("Scheduled job '{}' with cron '{}'", name, cronExpression);
    }

    private void arm(String name, Cron cron, Runnable task) {
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        ZonedDateTime now = ZonedDateTime.now(TEHRAN);
        ZonedDateTime next = executionTime.nextExecution(now)
                .orElse(now.plusHours(24));
        long delayMillis = Math.max(1000L, Duration.between(now, next).toMillis())
                + ThreadLocalRandom.current().nextLong(0, 5000);
        system.scheduler().scheduleOnce(
                Duration.ofMillis(delayMillis),
                () -> {
                    try {
                        task.run();
                    } catch (Exception e) {
                        log.error("Job '{}' failed", name, e);
                    }
                    arm(name, cron, task);
                },
                system.executionContext());
    }
}
