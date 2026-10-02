package ir.ebb.wallet.app.admin.service;

import com.github.f4b6a3.uuid.UuidCreator;
import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.base.security.UserPrincipal;
import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.external.rayan.wallet.dto.RayanInitCreditResponseDTO;
import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;
import ir.ebb.external.rayan.wallet.service.command.RayanWalletCommandService;
import ir.ebb.external.rayan.wallet.service.query.RayanWalletQueryService;
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
import ir.ebb.wallet.dto.AddCreditDTO;
import ir.ebb.wallet.dto.CreateWalletDTO;
import ir.ebb.wallet.projection.entity.CreditHistoryEntity;
import ir.ebb.wallet.service.BlockingExecutor;
import ir.ebb.wallet.service.WalletErrors;
import ir.ebb.wallet.service.WalletService;
import ir.ebb.wallet.service.credit.command.CreditHistoryCommandService;
import ir.ebb.wallet.service.credit.query.CreditHistoryQueryService;
import ir.ebb.wallet.service.query.WalletQueryService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

@Slf4j
@Singleton
public class AdminWalletWebServiceImpl implements AdminWalletWebService {

    private final WalletQueryService walletQueryService;
    private final WalletService walletService;
    private final RayanWalletQueryService rayanWalletQueryService;
    private final RayanWalletCommandService rayanWalletCommandService;
    private final CreditHistoryQueryService creditHistoryQueryService;
    private final CreditHistoryCommandService creditHistoryCommandService;
    private final Executor blockingExecutor;
    private final boolean activeCredit;

    @Inject
    public AdminWalletWebServiceImpl(WalletQueryService walletQueryService,
                                     WalletService walletService,
                                     RayanWalletQueryService rayanWalletQueryService,
                                     RayanWalletCommandService rayanWalletCommandService,
                                     CreditHistoryQueryService creditHistoryQueryService,
                                     CreditHistoryCommandService creditHistoryCommandService,
                                     @BlockingExecutor Executor blockingExecutor,
                                     boolean activeCredit) {
        this.walletQueryService = walletQueryService;
        this.walletService = walletService;
        this.rayanWalletQueryService = rayanWalletQueryService;
        this.rayanWalletCommandService = rayanWalletCommandService;
        this.creditHistoryQueryService = creditHistoryQueryService;
        this.creditHistoryCommandService = creditHistoryCommandService;
        this.blockingExecutor = blockingExecutor;
        this.activeCredit = activeCredit;
    }

    /**
     * Creates the sharded wallet entity. The legacy Rayan seed/reconcile is gone — the read model
     * is fed exclusively by the projection. {@code userId} is accepted for API stability only;
     * the projection wallet carries no user column.
     */
    @Override
    public CompletionStage<Void> create(String userId, Long dbsAccountNumber) {
        return walletQueryService.existsWallet(dbsAccountNumber)
                .thenCompose(exists -> exists
                        ? CompletableFuture.<Void>completedStage(null)
                        : walletService.createWallet(new CreateWalletDTO(dbsAccountNumber)).thenApply(v -> (Void) null))
                .exceptionally(error -> {
                    Throwable cause = WalletErrors.rootCause(error);
                    log.atError().log("create wallet failed for userId={} account={}: {}",
                            userId, dbsAccountNumber, cause.getMessage());
                    throw new BusinessException(cause.getMessage(), 5001);
                });
    }

    @Override
    public CompletionStage<PaginatedResponseDTO<WalletResponseDTO>> searchWallet(WalletSearchRequestDTO request) {
        return walletQueryService.findAll(WalletTransformer.adapt(request), request.toPageRequest())
                .thenApply(page -> new PaginatedResponseDTO<>(page, WalletTransformer::adapt));
    }

    @Override
    public CompletionStage<Void> initCredit(WalletInitCreditRequestDTO request, UserPrincipal principal) {
        if (!activeCredit) {
            throw new BusinessException(ExceptionConstants.NOT_ACCEPTABLE.getMessage(),
                    ExceptionConstants.NOT_ACCEPTABLE.getCode());
        }
        Long accountNumber = request.walletRequestDTO().dbsAccountNumber();
        // The credit-history id doubles as the actor-side AddCredit idempotency key.
        UUID trackingId = UuidCreator.getTimeOrderedEpoch();
        CreditHistoryEntity creditHistory = new CreditHistoryEntity(accountNumber, request.credit(),
                RayanCreditStatus.PENDING, trackingId, principal.name());
        return walletQueryService.getWalletEntity(accountNumber)
                .thenCompose(wallet -> walletService.addCredit(new AddCreditDTO(accountNumber, trackingId, request.credit())))
                .thenCompose(done -> callRayanAndRecord(creditHistory, request.credit(), accountNumber));
    }

    @Override
    public CompletionStage<Void> removeCredit(WalletRequestDTO request, UserPrincipal principal) {
        if (!activeCredit) {
            throw new BusinessException(ExceptionConstants.NOT_ACCEPTABLE.getMessage(),
                    ExceptionConstants.NOT_ACCEPTABLE.getCode());
        }
        Long accountNumber = request.dbsAccountNumber();
        UUID trackingId = UuidCreator.getTimeOrderedEpoch();
        CreditHistoryEntity creditHistory = new CreditHistoryEntity(accountNumber, 0L,
                RayanCreditStatus.PENDING, trackingId, principal.name());
        return walletQueryService.getWalletEntity(accountNumber)
                .thenCompose(wallet -> walletService.addCredit(new AddCreditDTO(accountNumber, trackingId, 0L)))
                .thenCompose(done -> callRayanAndRecord(creditHistory, 0L, accountNumber));
    }

    /**
     * The Rayan client blocks (HTTP + login + up to 3 retries) — it must never run on a Pekko
     * dispatcher or an R2DBC/reactor thread, hence the blocking-dispatcher offload.
     */
    private CompletionStage<Void> callRayanAndRecord(CreditHistoryEntity creditHistory, long credit, long accountNumber) {
        return CompletableFuture
                .supplyAsync(() -> rayanWalletCommandService.initCredit(credit, accountNumber), blockingExecutor)
                .thenCompose((RayanInitCreditResponseDTO response) -> {
                    creditHistory.setStatus(response.isSuccessful() ? RayanCreditStatus.SENT : RayanCreditStatus.ERROR);
                    creditHistory.setErrorMessage(response.getErrorMessage());
                    return creditHistoryCommandService.save(creditHistory);
                });
    }

    @Override
    public CompletionStage<PaginatedResponseDTO<CreditHistoryResponseDTO>> searchCredit(CreditHistorySearchRequestDTO request) {
        return creditHistoryQueryService.findAll(CreditHistoryTransformer.adapt(request), request.toPageRequest())
                .thenApply(page -> new PaginatedResponseDTO<>(page, CreditHistoryTransformer::adapt));
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
