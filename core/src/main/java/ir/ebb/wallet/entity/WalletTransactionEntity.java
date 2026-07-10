package ir.ebb.wallet.entity;

import ir.ebb.common.model.base.BaseEntityById;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.constant.enumeration.WalletOperationType;
import ir.ebb.wallet.constant.enumeration.WalletParameterType;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode(callSuper = true)
public class WalletTransactionEntity extends BaseEntityById {

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
}
