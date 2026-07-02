package ir.ebb.userinfo.service.query;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.common.exception.handler.NotFoundException;
import ir.ebb.common.model.user.User;
import ir.ebb.userinfo.entity.UserEntity;
import ir.ebb.userinfo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    @Override
    public UserEntity getByUser(User user) {
        return userRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException(ExceptionConstants.USER_NOT_EXISTS));
    }

    @Override
    public User getUserByDbsAccountNumber(Long accountNumber) {
        return userRepository.findByUserDbsAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException(ExceptionConstants.USER_NOT_EXISTS))
                .getUser();
    }
}
