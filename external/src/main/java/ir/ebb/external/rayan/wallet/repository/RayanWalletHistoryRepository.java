package ir.ebb.external.rayan.wallet.repository;

import ir.ebb.external.rayan.wallet.entity.RayanWalletHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface RayanWalletHistoryRepository extends JpaRepository<RayanWalletHistoryEntity, UUID> {

    @Modifying
    @Transactional
    @Query(value = "delete from rayan_wallet_history where id in " +
            "(SELECT id FROM rayan_wallet_history WHERE created_at < :date LIMIT 10000);",
            nativeQuery = true)
    long deleteByCreatedAtBefore(@Param("date") LocalDateTime localDateTime);

    @Modifying
    @Transactional
    @Query(value = "delete from rayan_wallet_history where id in " +
            "(SELECT id FROM rayan_wallet_history WHERE created_at >= :startDate AND created_at < :endDate LIMIT 10000);",
            nativeQuery = true)
    long deleteAllByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);
}
