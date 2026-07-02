package ir.ebb.external.rayan.wallet.repository;

import ir.ebb.external.rayan.wallet.entity.RayanWalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RayanWalletRepository extends JpaRepository<RayanWalletEntity, UUID> {

    @Modifying
    @Transactional
    @Query("delete from RayanWalletEntity where 1=1")
    void deleteAll();

    Optional<RayanWalletEntity> findByAccountNumber(Long accountNumber);

    Optional<RayanWalletEntity> findTopByAccountNumberOrderByUpdatedAt(Long accountNumber);
}
