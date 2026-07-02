package ir.ebb.wallet.repository.transaction;

import ir.ebb.wallet.dto.WalletTransactionSpecificationDTO;
import ir.ebb.wallet.entity.WalletTransactionEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import static java.util.Optional.ofNullable;

public class WalletTransactionSpecification {

    private WalletTransactionSpecification() {}

    public static Specification<WalletTransactionEntity> search(WalletTransactionSpecificationDTO request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            ofNullable(request.userId()).ifPresent(v ->
                    predicates.add(cb.equal(root.get("user").get("keycloakId"), java.util.UUID.fromString(v))));

            ofNullable(request.dbsAccountNumber()).ifPresent(v ->
                    predicates.add(cb.equal(root.get("user").get("dbsAccountNumber"), v)));

            ofNullable(request.type()).ifPresent(v ->
                    predicates.add(cb.equal(root.get("walletTransactionType"), v)));

            ofNullable(request.trackingCode()).ifPresent(v ->
                    predicates.add(cb.equal(root.get("trackingId"), v)));

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
