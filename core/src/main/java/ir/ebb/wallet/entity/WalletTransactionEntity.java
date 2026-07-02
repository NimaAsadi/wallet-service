package ir.ebb.wallet.entity;

import ir.ebb.common.model.base.BaseEntityById;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.constant.enumeration.WalletOperationType;
import ir.ebb.wallet.constant.enumeration.WalletParameterType;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity
@Table(name = "wallet_transaction")
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode(callSuper = true)
public class WalletTransactionEntity extends BaseEntityById {

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "keycloakId", column = @Column(name = "user_id", nullable = false)),
            @AttributeOverride(name = "dbsAccountNumber", column = @Column(name = "account_number", nullable = false))
    })
    private User user;

    @Column(nullable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    private WalletOperationType walletOperationType;

    @Enumerated(EnumType.STRING)
    private WalletTransactionType walletTransactionType;

    @Enumerated(EnumType.STRING)
    private WalletParameterType walletParameterType;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(nullable = false)
    private UUID trackingId;

    private Long frozenBefore;
    private Long frozenAfter;
    private Long balanceBefore;
    private Long balanceAfter;
}
