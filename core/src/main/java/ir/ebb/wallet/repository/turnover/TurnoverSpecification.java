package ir.ebb.wallet.repository.turnover;

import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import ir.ebb.wallet.entity.TurnoverEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import static java.util.Optional.ofNullable;

public class TurnoverSpecification {

    private TurnoverSpecification() {}

    public static Specification<TurnoverEntity> search(TurnoverSpecificationDTO req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            ofNullable(req.user()).ifPresent(u -> {
                predicates.add(cb.equal(root.get("user").get("keycloakId"), u.getKeycloakId()));
                predicates.add(cb.equal(root.get("user").get("dbsAccountNumber"), u.getDbsAccountNumber()));
            });
            ofNullable(req.fromCreatedAt()).ifPresent(v ->
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), v)));
            ofNullable(req.toCreatedAt()).ifPresent(v -> {
                var endOfDay = req.toCreatedAt().toLocalDate().atTime(23, 59, 59);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endOfDay));
            });

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
