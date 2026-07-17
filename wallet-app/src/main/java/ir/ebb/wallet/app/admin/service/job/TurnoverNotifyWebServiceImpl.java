package ir.ebb.wallet.app.admin.service.job;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.app.infra.KafkaWalletProducer;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.entity.TurnoverEntity;
import ir.ebb.wallet.service.query.WalletQueryService;
import ir.ebb.wallet.service.turnover.query.TurnoverQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.util.*;

@Slf4j
public class TurnoverNotifyWebServiceImpl implements TurnoverNotifyWebService {

    private static final int BATCH_SIZE = 100;

    private final TurnoverQueryService turnoverQueryService;
    private final WalletQueryService walletQueryService;
    private final KafkaWalletProducer kafkaWalletProducer;
    private final String turnoverNotificationTopic;

    private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

    public TurnoverNotifyWebServiceImpl(TurnoverQueryService turnoverQueryService,
                                        WalletQueryService walletQueryService,
                                        KafkaWalletProducer kafkaWalletProducer,
                                        String turnoverNotificationTopic) {
        this.turnoverQueryService = turnoverQueryService;
        this.walletQueryService = walletQueryService;
        this.kafkaWalletProducer = kafkaWalletProducer;
        this.turnoverNotificationTopic = turnoverNotificationTopic;
    }

    @Override
    public void aggregateUserTurnover() {
        Long lastAccountNumber = 0L;
        while (true) {
            List<Long> batch = turnoverQueryService.getAccountNumberBatch(lastAccountNumber, BATCH_SIZE);
            if (ObjectUtils.isEmpty(batch)) break;
            processBatch(batch);
            lastAccountNumber = batch.get(batch.size() - 1);
        }
    }

    private void processBatch(List<Long> accountNumbers) {
        for (Long accountNumber : accountNumbers) {
            try {
                List<TurnoverEntity> turnovers = turnoverQueryService.getByAccountNumber(accountNumber);
                if (ObjectUtils.isEmpty(turnovers)) continue;

                String message = buildMessage(accountNumber, turnovers);
                if (StringUtils.isBlank(message)) continue;

                Map<String, Object> notification = new HashMap<>();
                notification.put("accountNumber", accountNumber);
                notification.put("message", message);
                kafkaWalletProducer.send(turnoverNotificationTopic, accountNumber.toString(), notification);
            } catch (Exception e) {
                log.atWarn().log("Failed to send turnover notification for accountNumber={}: {}",
                        accountNumber, e.getMessage());
            }
        }
    }

    private String buildMessage(Long accountNumber, List<TurnoverEntity> turnovers) {
        StringBuilder message = new StringBuilder(256);

        for (TurnoverEntity turnover : turnovers) {
            formatTurnover(turnover)
                    .ifPresent(formatted -> message.append(formatted).append('\n'));
        }

        appendBuyingPower(message, accountNumber);

        return message.toString().trim();
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

    private void appendBuyingPower(StringBuilder message, Long accountNumber) {
        try {
            BuyingPower buyingPower =
                    walletQueryService.getBuyingPower(accountNumber, SettlementDelay.T_PLUS_2);

            if (!message.isEmpty()) {
                message.append('\n');
            }

            message.append("مانده: ")
                    .append(format(buyingPower.sum(false)))
                    .append(" ریال");

        } catch (BusinessException e) {
            log.atWarn()
                    .setCause(e)
                    .log("Unable to retrieve buying power for accountNumber={}", accountNumber);
        }
    }

    private String format(Number value) {
        return numberFormat.format(value == null ? 0 : value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
