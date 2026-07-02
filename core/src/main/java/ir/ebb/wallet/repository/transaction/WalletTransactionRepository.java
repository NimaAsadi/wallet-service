package ir.ebb.wallet.repository.transaction;

import ir.ebb.wallet.entity.WalletTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransactionEntity, UUID>, JpaSpecificationExecutor<WalletTransactionEntity> {
}
