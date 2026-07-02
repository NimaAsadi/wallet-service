package ir.ebb.userinfo.service.query;

import ir.ebb.common.model.user.User;
import ir.ebb.userinfo.entity.UserEntity;

public interface UserQueryService {

    UserEntity getByUser(User user);

    User getUserByDbsAccountNumber(Long accountNumber);
}
