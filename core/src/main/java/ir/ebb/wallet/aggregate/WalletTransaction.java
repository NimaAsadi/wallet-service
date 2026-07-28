package ir.ebb.wallet.aggregate;

import ir.ebb.common.model.user.User;
import ir.ebb.wallet.constant.enumeration.WalletOperationType;
import ir.ebb.wallet.constant.enumeration.WalletParameterType;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.entity.WalletTransactionEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Audit leg persisted inside {@code WalletEvent.WalletMutated} and projected into the
 * {@code wallet_transaction} read model. Carries {@code @NoArgsConstructor}/{@code @AllArgsConstructor}
 * (alongside {@code @Builder}) so Pekko's Jackson serializer can reconstruct it via the
 * no-arg constructor + setters (records use their canonical constructor; this POJO needs the ctors).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    private long accountNumber;
    private UUID walletId;
    private WalletOperationType walletOperationType;
    private WalletTransactionType walletTransactionType;
    private WalletParameterType walletParameterType;
    private Long amount;
    private UUID trackingId;
    private Long frozenBefore;
    private Long frozenAfter;
    private Long balanceBefore;
    private Long balanceAfter;

    public WalletTransactionEntity adaptToEntity() {
        WalletTransactionEntity entity = new WalletTransactionEntity();
        entity.setAccountNumber(accountNumber);
        entity.setWalletId(walletId);
        entity.setWalletOperationType(walletOperationType);
        entity.setWalletTransactionType(walletTransactionType);
        entity.setWalletParameterType(walletParameterType);
        entity.setAmount(amount);
        entity.setTrackingId(trackingId);
        entity.setFrozenBefore(frozenBefore);
        entity.setFrozenAfter(frozenAfter);
        entity.setBalanceBefore(balanceBefore);
        entity.setBalanceAfter(balanceAfter);
        return entity;
    }
}
