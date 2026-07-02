package ir.ebb.wallet.aggregate;

import ir.ebb.common.model.user.User;
import ir.ebb.wallet.constant.enumeration.WalletOperationType;
import ir.ebb.wallet.constant.enumeration.WalletParameterType;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.entity.WalletTransactionEntity;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class WalletTransaction {

    private User user;
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
        entity.setUser(user);
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
