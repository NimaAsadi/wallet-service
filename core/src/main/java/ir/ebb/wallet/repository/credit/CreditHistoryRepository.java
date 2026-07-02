package ir.ebb.wallet.repository.credit;

import ir.ebb.wallet.entity.CreditHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CreditHistoryRepository extends JpaRepository<CreditHistoryEntity, UUID>, JpaSpecificationExecutor<CreditHistoryEntity> {
}
