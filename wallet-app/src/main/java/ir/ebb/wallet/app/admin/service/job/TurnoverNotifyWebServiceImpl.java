package ir.ebb.wallet.app.admin.service.job;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.wallet.infrastructure.KafkaWalletProducer;
import ir.ebb.wallet.projection.entity.TurnoverEntity;
import ir.ebb.wallet.service.WalletErrors;
import ir.ebb.wallet.service.WalletService;
import ir.ebb.wallet.service.turnover.query.TurnoverQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Async keyset-paging loop: fetch a batch of account numbers → notify each sequentially →
 * recurse past the last account number. One bad account never stops the job (per-account
 * {@code exceptionally}); batches are processed strictly in order to keep Kafka ordering per
 * account and bound concurrency.
 */
@Slf4j
@RequiredArgsConstructor
public class TurnoverNotifyWebServiceImpl implements TurnoverNotifyWebService {

    private static final int BATCH_SIZE = 100;

    private final TurnoverQueryService turnoverQueryService;
    private final WalletService walletService;
    private final KafkaWalletProducer kafkaWalletProducer;
    private final String turnoverNotificationTopic;

    private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

    @Override
    public CompletionStage<Void> aggregateUserTurnover() {
        return page(0L);
    }

    private CompletionStage<Void> page(Long lastAccountNumber) {
        return turnoverQueryService.getAccountNumberBatch(lastAccountNumber, BATCH_SIZE)
                .thenCompose(batch -> {
                    if (ObjectUtils.isEmpty(batch)) return CompletableFuture.completedStage(null);
                    return processBatch(batch)
                            .thenCompose(v -> page(batch.get(batch.size() - 1)));
                });
    }

    private CompletionStage<Void> processBatch(List<Long> accountNumbers) {
        CompletionStage<Void> chain = CompletableFuture.completedStage(null);
        for (Long accountNumber : accountNumbers) {
            chain = chain.thenCompose(v -> notify(accountNumber));
        }
        return chain;
    }

    private CompletionStage<Void> notify(Long accountNumber) {
        return turnoverQueryService.getByAccountNumber(accountNumber)
                .thenCompose(turnovers -> {
                    if (ObjectUtils.isEmpty(turnovers)) return CompletableFuture.completedStage(null);
                    return buildMessage(accountNumber, turnovers).thenApply(message -> {
                        if (StringUtils.isBlank(message)) return null;
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("accountNumber", accountNumber);
                        notification.put("message", message);
                        kafkaWalletProducer.send(turnoverNotificationTopic, accountNumber.toString(), notification);
                        return (Void) null;
                    });
                })
                .exceptionally(error -> {
                    log.atWarn().log("Failed to send turnover notification for accountNumber={}: {}",
                            accountNumber, WalletErrors.rootCause(error).getMessage());
                    return null;
                });
    }

    private CompletionStage<String> buildMessage(Long accountNumber, List<TurnoverEntity> turnovers) {
        StringBuilder message = new StringBuilder(256);

        for (TurnoverEntity turnover : turnovers) {
            formatTurnover(turnover)
                    .ifPresent(formatted -> message.append(formatted).append('\n'));
        }

        return appendBuyingPower(message, accountNumber)
                .thenApply(v -> message.toString().trim());
    }

    private Optional<String> formatTurnover(TurnoverEntity turnover) {
        return switch (turnover.getType()) {
            case DEPOSIT -> Optional.of(formatDeposit(turnover));
            case WITHDRAW -> Optional.of(formatWithdraw(turnover));
            case BUY -> Optional.of(formatBuy(turnover));
            case SELL -> Optional.of(formatSell(turnover));
            default -> Optional.empty();
        };
    }

    private String formatDeposit(TurnoverEntity turnover) {
        return String.format(
                "واریز: %s ریال",
                format(turnover.getCredit())
        );
    }

    private String formatWithdraw(TurnoverEntity turnover) {
        return String.format(
                "برداشت: %s ریال",
                format(turnover.getDebit())
        );
    }

    private String formatBuy(TurnoverEntity turnover) {
        return String.format(
                "خرید: %s سهم %s به قیمت %s ریال",
                format(turnover.getTradedQuantity()),
                safe(turnover.getInstrumentAfcNormName()),
                format(turnover.getTradedPrice())
        );
    }

    private String formatSell(TurnoverEntity turnover) {
        return String.format(
                "فروش: %s سهم %s به قیمت %s ریال",
                format(turnover.getTradedQuantity()),
                safe(turnover.getInstrumentAfcNormName()),
                format(turnover.getTradedPrice())
        );
    }

    /** Buying power is best-effort: a missing wallet (or any failure) only drops the balance line. */
    private CompletionStage<Void> appendBuyingPower(StringBuilder message, Long accountNumber) {
        return walletService.getBuyingPower(accountNumber, SettlementDelay.T_PLUS_2)
                .thenApply(buyingPower -> {
                    if (!message.isEmpty()) {
                        message.append('\n');
                    }
                    message.append("مانده: ")
                            .append(format(buyingPower.sum(false)))
                            .append(" ریال");
                    return (Void) null;
                })
                .exceptionally(error -> {
                    log.atWarn()
                            .setCause(WalletErrors.rootCause(error))
                            .log("Unable to retrieve buying power for accountNumber={}", accountNumber);
                    return null;
                });
    }

    private String format(Number value) {
        return numberFormat.format(value == null ? 0 : value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
