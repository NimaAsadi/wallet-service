package ir.ebb.wallet.app.admin.service;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.base.security.UserPrincipal;
import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.common.model.user.User;
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
import ir.ebb.wallet.entity.CreditHistoryEntity;
import ir.ebb.wallet.entity.WalletEntity;
import ir.ebb.wallet.repository.credit.CreditHistoryRepository;
import ir.ebb.wallet.service.credit.query.CreditHistoryQueryService;
import ir.ebb.wallet.service.query.WalletQueryService;
import ir.ebb.wallet.wallet.WalletFacade;
import ir.ebb.wallet.wallet.WalletState;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
public class AdminWalletWebServiceImpl implements AdminWalletWebService {

    private final WalletFacade walletFacade;
    private final WalletQueryService walletQueryService;
    private final RayanWalletQueryService rayanWalletQueryService;
    private final RayanWalletCommandService rayanWalletCommandService;
    private final CreditHistoryQueryService creditHistoryQueryService;
    private final CreditHistoryRepository creditHistoryRepository;
    private final UserQueryService userQueryService;
    private final boolean activeCredit;

    public AdminWalletWebServiceImpl(WalletFacade walletFacade,
                                     WalletQueryService walletQueryService,
                                     RayanWalletQueryService rayanWalletQueryService,
                                     RayanWalletCommandService rayanWalletCommandService,
                                     CreditHistoryQueryService creditHistoryQueryService,
                                     CreditHistoryRepository creditHistoryRepository,
                                     UserQueryService userQueryService,
                                     boolean activeCredit) {
        this.walletFacade = walletFacade;
        this.walletQueryService = walletQueryService;
        this.rayanWalletQueryService = rayanWalletQueryService;
        this.rayanWalletCommandService = rayanWalletCommandService;
        this.creditHistoryQueryService = creditHistoryQueryService;
        this.creditHistoryRepository = creditHistoryRepository;
        this.userQueryService = userQueryService;
        this.activeCredit = activeCredit;
    }

    @Override
    public void create(String userId, Long dbsAccountNumber) {
        try {
            User user = User.of(UUID.fromString(userId), dbsAccountNumber);
            if (!walletQueryService.existsWallet(user)) {
                UUID walletId = UUID.randomUUID();
                walletFacade.createWallet(walletId, user);
                // Seed initial balances from the authoritative Rayan snapshot (fire-and-forget reconcile).
                RayanWalletDTO rayanWalletDTO = rayanWalletQueryService.findUserWallet(dbsAccountNumber);
                Wallet seed = new Wallet(user);
                seed.setId(walletId);
                seed.setAccountNumber(dbsAccountNumber);
                WalletTransformer.adapt(seed, rayanWalletDTO);
                walletFacade.reconcileFromRayan(user, WalletState.fromAggregate(seed, List.of()));
            }
        } catch (Exception e) {
            log.atError().log("create wallet failed for userId={} account={}: {}", userId, dbsAccountNumber, e.getMessage());
            throw new BusinessException(e.getMessage(), 5001);
        }
    }

    @Override
    public PaginatedResponseDTO<WalletResponseDTO> searchWallet(WalletSearchRequestDTO request) {
        PageRequest pageRequest = request.toPageRequest();
        Page<WalletEntity> page = walletQueryService.findAll(WalletTransformer.adapt(request), pageRequest);
        return new PaginatedResponseDTO<>(page, WalletTransformer::adapt);
    }

    @Override
    public void initCredit(WalletInitCreditRequestDTO request, UserPrincipal principal) {
        if (!activeCredit) {
            throw new BusinessException(ExceptionConstants.NOT_ACCEPTABLE.getMessage(),
                    ExceptionConstants.NOT_ACCEPTABLE.getCode());
        }
        UUID adminId = principal.keycloakId();
        String adminFullName = principal.name();
        Wallet wallet = walletQueryService.getWallet(request.walletRequestDTO().dbsAccountNumber());
        UserEntity userEntity = userQueryService.getByUser(wallet.getUser());
        User user = wallet.getUser();
        CreditHistoryEntity creditHistory = new CreditHistoryEntity(userEntity, request.credit(),
                RayanCreditStatus.PENDING, adminId, adminFullName);
        // Apply the credit ceiling through the event-sourced entity (trackingId = the credit-history id, for idempotency).
        walletFacade.addCredit(creditHistory.getId(), user, request.credit());
        // Call Rayan and record the outcome as a credit-history audit row (not event-sourced).
        RayanInitCreditResponseDTO response = rayanWalletCommandService.initCredit(
                request.credit(), request.walletRequestDTO().dbsAccountNumber());
        creditHistory.setStatus(response.isSuccessful() ? RayanCreditStatus.SENT : RayanCreditStatus.ERROR);
        creditHistory.setErrorMessage(response.getErrorMessage());
        creditHistoryRepository.save(creditHistory);
    }

    @Override
    public void removeCredit(WalletRequestDTO request, UserPrincipal principal) {
        if (!activeCredit) {
            throw new BusinessException(ExceptionConstants.NOT_ACCEPTABLE.getMessage(),
                    ExceptionConstants.NOT_ACCEPTABLE.getCode());
        }
        UUID adminId = principal.keycloakId();
        String adminFullName = principal.name();
        Wallet wallet = walletQueryService.getWallet(request.dbsAccountNumber());
        UserEntity userEntity = userQueryService.getByUser(wallet.getUser());
        User user = wallet.getUser();
        CreditHistoryEntity creditHistory = new CreditHistoryEntity(userEntity, 0L,
                RayanCreditStatus.PENDING, adminId, adminFullName);
        walletFacade.addCredit(creditHistory.getId(), user, 0L);
        RayanInitCreditResponseDTO response = rayanWalletCommandService.initCredit(0L, request.dbsAccountNumber());
        creditHistory.setStatus(response.isSuccessful() ? RayanCreditStatus.SENT : RayanCreditStatus.ERROR);
        creditHistory.setErrorMessage(response.getErrorMessage());
        creditHistoryRepository.save(creditHistory);
    }

    @Override
    public PaginatedResponseDTO<CreditHistoryResponseDTO> searchCredit(CreditHistorySearchRequestDTO request) {
        PageRequest pageRequest = request.toPageRequest();
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
