package ir.ebb.wallet.repository.credit;

import ir.ebb.wallet.dto.CreditSpecificationDTO;
import ir.ebb.wallet.entity.CreditHistoryEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import static java.util.Optional.ofNullable;

public class CreditHistorySpecification {

    private CreditHistorySpecification() {}

    public static Specification<CreditHistoryEntity> search(CreditSpecificationDTO req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            ofNullable(req.userIds()).ifPresent(v ->
                    predicates.add(root.get("user").get("user").get("keycloakId").in(v)));
            ofNullable(req.fromDate()).ifPresent(v ->
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt").as(java.time.LocalDate.class), v)));
            ofNullable(req.toDate()).ifPresent(v ->
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt").as(java.time.LocalDate.class), v)));
            ofNullable(req.fromAmount()).ifPresent(v ->
                    predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), v)));
            ofNullable(req.toAmount()).ifPresent(v ->
                    predicates.add(cb.lessThanOrEqualTo(root.get("amount"), v)));
            ofNullable(req.status()).ifPresent(v ->
                    predicates.add(cb.equal(root.get("status"), v)));
            ofNullable(req.createdBy()).ifPresent(v ->
                    predicates.add(cb.like(cb.lower(root.get("createdBy")), "%" + v.toLowerCase() + "%")));

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
