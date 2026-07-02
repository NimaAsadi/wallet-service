package ir.ebb.wallet.app.admin.service;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.common.model.user.User;
import ir.ebb.common.utility.SecurityUtil;
import ir.ebb.external.rayan.wallet.dto.RayanInitCreditResponseDTO;
import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;
import ir.ebb.external.rayan.wallet.service.command.RayanWalletCommandService;
import ir.ebb.external.rayan.wallet.service.query.RayanWalletQueryService;
import ir.ebb.userinfo.entity.UserEntity;
import ir.ebb.userinfo.service.query.UserQueryService;
import ir.ebb.wallet.aggregate.Wallet;
import ir.ebb.wallet.app.admin.dto.request.CreditHistorySearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletInitCreditRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletSearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.response.CreditHistoryResponseDTO;
import ir.ebb.wallet.app.admin.dto.response.RayanWalletResponseDTO;
import ir.ebb.wallet.app.admin.dto.response.WalletResponseDTO;
import ir.ebb.wallet.app.admin.transformer.CreditHistoryTransformer;
import ir.ebb.wallet.app.admin.transformer.WalletTransformer;
import ir.ebb.wallet.constant.enumeration.RayanCreditStatus;
import ir.ebb.wallet.constant.valueobject.Money;
import ir.ebb.wallet.entity.CreditHistoryEntity;
import ir.ebb.wallet.entity.WalletDebtEntity;
import ir.ebb.wallet.entity.WalletEntity;
import ir.ebb.wallet.service.command.WalletCommandService;
import ir.ebb.wallet.service.credit.query.CreditHistoryQueryService;
import ir.ebb.wallet.service.query.WalletQueryService;
import ir.ebb.wallet.service.turnover.command.TurnoverCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class AdminWalletWebServiceImpl implements AdminWalletWebService {

    private final WalletCommandService walletCommandService;
    private final WalletQueryService walletQueryService;
    private final RayanWalletQueryService rayanWalletQueryService;
    private final RayanWalletCommandService rayanWalletCommandService;
    private final CreditHistoryQueryService creditHistoryQueryService;
    private final UserQueryService userQueryService;
    private final TurnoverCommandService turnoverCommandService;

    @Value("${credit.active:false}")
    private boolean activeCredit;

    @Override
    public void create(String userId, Long dbsAccountNumber) {
        try {
            User user = User.of(UUID.fromString(userId), dbsAccountNumber);
            if (!walletQueryService.existsWallet(user)) {
                RayanWalletDTO rayanWalletDTO = rayanWalletQueryService.findUserWallet(dbsAccountNumber);
                WalletEntity walletEntity = WalletTransformer.adapt(rayanWalletDTO);
                walletEntity.setWalletDebtEntity(new WalletDebtEntity(walletEntity));
                walletEntity.setUser(user);
                walletCommandService.save(walletEntity);
                turnoverCommandService.createRemaining(walletEntity);
            }
        } catch (Exception e) {
            log.atError().log("create wallet failed for userId={} account={}: {}", userId, dbsAccountNumber, e.getMessage());
            throw new BusinessException(e.getMessage(), 5001);
        }
    }

    @Override
    public PaginatedResponseDTO<WalletResponseDTO> searchWallet(WalletSearchRequestDTO request) {
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize(), Sort.by(request.getOrderBy()));
        Page<WalletEntity> page = walletQueryService.findAll(WalletTransformer.adapt(request), pageRequest);
        return new PaginatedResponseDTO<>(page, WalletTransformer::adapt);
    }

    @Override
    public void initCredit(WalletInitCreditRequestDTO request) {
        if (!activeCredit) {
            throw new BusinessException(ExceptionConstants.NOT_ACCEPTABLE.getMessage(),
                    ExceptionConstants.NOT_ACCEPTABLE.getCode());
        }
        UUID adminId = SecurityUtil.getAdminAuthDetail().getKeycloakId();
        String adminFullName = SecurityUtil.getAdminAuthDetail().getName();
        Wallet wallet = walletQueryService.getWallet(request.walletRequestDTO().dbsAccountNumber());
        UserEntity userEntity = userQueryService.getByUser(wallet.getUser());
        CreditHistoryEntity creditHistory = new CreditHistoryEntity(userEntity, request.credit(),
                RayanCreditStatus.PENDING, adminId, adminFullName);
        wallet.addCredit(creditHistory.getId(), new Money(request.credit()));
        RayanInitCreditResponseDTO response = rayanWalletCommandService.initCredit(
                request.credit(), request.walletRequestDTO().dbsAccountNumber());
        creditHistory.setStatus(response.isSuccessful() ? RayanCreditStatus.SENT : RayanCreditStatus.ERROR);
        creditHistory.setErrorMessage(response.getErrorMessage());
        walletCommandService.saveForCredit(wallet, creditHistory);
    }

    @Override
    public void removeCredit(WalletRequestDTO request) {
        if (!activeCredit) {
            throw new BusinessException(ExceptionConstants.NOT_ACCEPTABLE.getMessage(),
                    ExceptionConstants.NOT_ACCEPTABLE.getCode());
        }
        UUID adminId = SecurityUtil.getAdminAuthDetail().getKeycloakId();
        String adminFullName = SecurityUtil.getAdminAuthDetail().getName();
        Wallet wallet = walletQueryService.getWallet(request.dbsAccountNumber());
        UserEntity userEntity = userQueryService.getByUser(wallet.getUser());
        CreditHistoryEntity creditHistory = new CreditHistoryEntity(userEntity, 0L,
                RayanCreditStatus.PENDING, adminId, adminFullName);
        wallet.addCredit(creditHistory.getId(), new Money(0L));
        RayanInitCreditResponseDTO response = rayanWalletCommandService.initCredit(0L, request.dbsAccountNumber());
        creditHistory.setStatus(response.isSuccessful() ? RayanCreditStatus.SENT : RayanCreditStatus.ERROR);
        creditHistory.setErrorMessage(response.getErrorMessage());
        walletCommandService.saveForCredit(wallet, creditHistory);
    }

    @Override
    public PaginatedResponseDTO<CreditHistoryResponseDTO> searchCredit(CreditHistorySearchRequestDTO request) {
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize(), Sort.by(request.getOrderBy()));
        Page<CreditHistoryEntity> page = creditHistoryQueryService.findAll(
                CreditHistoryTransformer.adapt(request), pageRequest);
        return new PaginatedResponseDTO<>(page, CreditHistoryTransformer::adapt);
    }

    @Override
    public RayanWalletResponseDTO getRayanWallet(long accountNumber) {
        try {
            RayanWalletDTO dto = rayanWalletQueryService.getUserWallet(accountNumber);
            return WalletTransformer.adaptForAdmin(dto);
        } catch (Exception e) {
            log.atError().log("getRayanWallet failed for account={}: {}", accountNumber, e.getMessage());
            throw new BusinessException(e.getMessage(), 5001);
        }
    }
}
