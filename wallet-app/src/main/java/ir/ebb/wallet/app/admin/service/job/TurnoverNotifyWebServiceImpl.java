package ir.ebb.wallet.app.admin.service.job;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.wallet.app.infra.KafkaWalletProducer;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.entity.TurnoverEntity;
import ir.ebb.wallet.service.query.WalletQueryService;
import ir.ebb.wallet.service.turnover.query.TurnoverQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        StringBuilder sb = new StringBuilder();
        for (TurnoverEntity t : turnovers) {
            String line = switch (t.getType()) {
                case DEPOSIT -> "واریز: " + numberFormat.format(t.getCredit()) + " ریال\n";
                case WITHDRAW -> "برداشت: " + numberFormat.format(t.getDebit()) + " ریال\n";
                case BUY -> "خرید: " + numberFormat.format(t.getTradedQuantity()) + " سهم "
                        + t.getInstrumentAfcNormName() + " به قیمت "
                        + numberFormat.format(t.getTradedPrice()) + " ریال\n";
                case SELL -> "فروش: " + numberFormat.format(t.getTradedQuantity()) + " سهم "
                        + t.getInstrumentAfcNormName() + " به قیمت "
                        + numberFormat.format(t.getTradedPrice()) + " ریال\n";
                default -> null;
            };
            if (StringUtils.isNotEmpty(line)) sb.append(line);
        }

        try {
            BuyingPower buyingPower = walletQueryService.getBuyingPower(accountNumber, SettlementDelay.T_PLUS_2);
            sb.append("مانده: ").append(numberFormat.format(buyingPower.sum(false))).append(" ریال");
        } catch (Exception e) {
            log.atWarn().log("Could not append remaining balance for accountNumber={}", accountNumber);
        }

        return sb.toString();
    }
}
