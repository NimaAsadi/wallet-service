package ir.ebb.userinfo.repository;

import ir.ebb.common.model.user.User;
import ir.ebb.userinfo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUser(User user);

    Optional<UserEntity> findByUserDbsAccountNumber(Long dbsAccountNumber);
}
