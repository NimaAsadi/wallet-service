package ir.ebb.wallet.repository;

import ir.ebb.wallet.dto.WalletSpecificationDTO;
import ir.ebb.wallet.entity.WalletEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import static java.util.Optional.ofNullable;

public class WalletSpecification {

    private WalletSpecification() {}

    public static Specification<WalletEntity> search(WalletSpecificationDTO req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            ofNullable(req.userId()).ifPresent(v ->
                    predicates.add(cb.equal(root.get("user").get("keycloakId"), v)));
            ofNullable(req.dbsAccountNumber()).ifPresent(v ->
                    predicates.add(cb.equal(root.get("user").get("dbsAccountNumber"), v)));
            ofNullable(req.userIds()).ifPresent(v ->
                    predicates.add(root.get("user").get("keycloakId").in(v)));
            ofNullable(req.accountNumbers()).ifPresent(v ->
                    predicates.add(root.get("user").get("dbsAccountNumber").in(v)));
            ofNullable(req.fromT0Balance()).ifPresent(v ->
                    predicates.add(cb.greaterThanOrEqualTo(root.get("t0").get("balance"), v)));
            ofNullable(req.toT0Balance()).ifPresent(v ->
                    predicates.add(cb.lessThanOrEqualTo(root.get("t0").get("balance"), v)));
            ofNullable(req.fromT1Balance()).ifPresent(v ->
                    predicates.add(cb.greaterThanOrEqualTo(root.get("t1").get("balance"), v)));
            ofNullable(req.toT1Balance()).ifPresent(v ->
                    predicates.add(cb.lessThanOrEqualTo(root.get("t1").get("balance"), v)));
            ofNullable(req.fromT2Balance()).ifPresent(v ->
                    predicates.add(cb.greaterThanOrEqualTo(root.get("t2").get("balance"), v)));
            ofNullable(req.toT2Balance()).ifPresent(v ->
                    predicates.add(cb.lessThanOrEqualTo(root.get("t2").get("balance"), v)));
            ofNullable(req.fromInitialCredit()).ifPresent(v ->
                    predicates.add(cb.greaterThanOrEqualTo(root.get("initialCredit"), v)));
            ofNullable(req.toInitialCredit()).ifPresent(v ->
                    predicates.add(cb.lessThanOrEqualTo(root.get("initialCredit"), v)));
            ofNullable(req.fromCredit()).ifPresent(v ->
                    predicates.add(cb.greaterThanOrEqualTo(root.get("credit"), v)));
            ofNullable(req.toCredit()).ifPresent(v ->
                    predicates.add(cb.lessThanOrEqualTo(root.get("credit"), v)));

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
