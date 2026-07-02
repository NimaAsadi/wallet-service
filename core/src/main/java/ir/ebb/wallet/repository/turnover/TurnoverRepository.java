package ir.ebb.wallet.repository.turnover;

import ir.ebb.wallet.entity.TurnoverEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface TurnoverRepository extends JpaRepository<TurnoverEntity, UUID>, JpaSpecificationExecutor<TurnoverEntity> {

    @Modifying
    @Transactional
    @Query("delete from TurnoverEntity where 1=1")
    void deleteAll();

    @Query("SELECT DISTINCT t.user.dbsAccountNumber FROM TurnoverEntity t " +
           "WHERE t.user.dbsAccountNumber > :lastAccountNumber " +
           "ORDER BY t.user.dbsAccountNumber ASC")
    List<Long> findAccountNumberBatch(@Param("lastAccountNumber") Long lastAccountNumber, Pageable pageable);

    @Query("SELECT t FROM TurnoverEntity t WHERE t.user.dbsAccountNumber = :accountNumber " +
           "AND t.type != ir.ebb.wallet.constant.enumeration.TurnoverOperationType.REMAINING")
    List<TurnoverEntity> findByAccountNumber(@Param("accountNumber") Long accountNumber);
}
