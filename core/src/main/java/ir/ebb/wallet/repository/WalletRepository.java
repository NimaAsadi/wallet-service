package ir.ebb.wallet.repository;

import ir.ebb.common.model.user.User;
import ir.ebb.wallet.entity.WalletEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<WalletEntity, UUID>, JpaSpecificationExecutor<WalletEntity>, WalletRepositoryCustom {

    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    Optional<WalletEntity> findFirstByUser(User user);

    Optional<WalletEntity> findByUser_DbsAccountNumber(long userDbsAccountNumber);

    @Query("""
            SELECT CASE WHEN EXISTS (
                SELECT 1 FROM WalletEntity w
                WHERE w.user = :user
                  AND w.separCredit < w.separInitialCredit
            ) THEN TRUE ELSE FALSE END
            FROM WalletEntity w2
            """)
    boolean existsByUserAndSeparCreditLessThanSeparInitialCredit(@Param("user") User user);

    @Modifying
    @Query("UPDATE WalletEntity w SET w.separCredit = w.separCredit - w.separInitialCredit, w.separInitialCredit = 0 WHERE w.user IN (:users)")
    void settleSeparCreditsByUser(@Param("users") Set<User> users);

    @Query("SELECT w FROM WalletEntity w WHERE w.separInitialCredit > 0 OR w.separCredit < w.separInitialCredit")
    List<WalletEntity> findSeparCreditDebtors();
}
