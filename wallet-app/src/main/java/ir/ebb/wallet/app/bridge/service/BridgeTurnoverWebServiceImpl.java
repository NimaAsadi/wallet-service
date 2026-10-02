package ir.ebb.wallet.app.bridge.service;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.bridge.dto.request.BridgeTurnoverSearchRequestDTO;
import ir.ebb.wallet.app.bridge.dto.response.BridgeTurnoverResponseDTO;
import ir.ebb.wallet.app.bridge.transformer.BridgeTurnoverTransformer;
import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import ir.ebb.wallet.service.query.WalletQueryService;
import ir.ebb.wallet.service.turnover.query.TurnoverQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletionStage;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BridgeTurnoverWebServiceImpl implements BridgeTurnoverWebService {

    private final TurnoverQueryService turnoverQueryService;
    private final WalletQueryService walletQueryService;

    @Override
    public CompletionStage<List<BridgeTurnoverResponseDTO>> getTodayTurnover(Long accountNumber) {
        // Existence gate first: an unknown account rejects with WALLET_NOT_EXIST (4001).
        return walletQueryService.getWalletEntity(accountNumber)
                .thenCompose(wallet -> turnoverQueryService.getTodayTurnoverByUser(accountNumber))
                .thenApply(BridgeTurnoverTransformer::adaptList);
    }

    @Override
    public CompletionStage<PaginatedResponseDTO<BridgeTurnoverResponseDTO>> getHistory(Long accountNumber, BridgeTurnoverSearchRequestDTO request) {
        LocalDateTime from = request.getFromCreatedAt() != null
                ? request.getFromCreatedAt().atStartOfDay()
                : LocalDate.now().atStartOfDay();
        LocalDateTime to = request.getToCreatedAt() != null
                ? request.getToCreatedAt().plusDays(1).atStartOfDay()
                : LocalDate.now().plusDays(1).atStartOfDay();

        TurnoverSpecificationDTO spec = TurnoverSpecificationDTO.builder()
                .dbsAccountNumber(accountNumber)
                .fromCreatedAt(from)
                .toCreatedAt(to)
                .build();

        PageRequest pageRequest = request.toPageRequest();
        return walletQueryService.getWalletEntity(accountNumber)
                .thenCompose(wallet -> turnoverQueryService.search(spec, pageRequest))
                .thenApply(page -> new PaginatedResponseDTO<>(page, BridgeTurnoverTransformer::adapt));
    }
}
