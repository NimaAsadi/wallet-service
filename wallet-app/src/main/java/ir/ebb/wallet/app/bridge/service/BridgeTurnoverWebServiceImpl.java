package ir.ebb.wallet.app.bridge.service;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.bridge.dto.request.BridgeTurnoverSearchRequestDTO;
import ir.ebb.wallet.app.bridge.dto.response.BridgeTurnoverResponseDTO;
import ir.ebb.wallet.app.bridge.transformer.BridgeTurnoverTransformer;
import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import ir.ebb.wallet.entity.WalletEntity;
import ir.ebb.wallet.service.query.WalletQueryService;
import ir.ebb.wallet.service.turnover.query.TurnoverQueryService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class BridgeTurnoverWebServiceImpl implements BridgeTurnoverWebService {

    private final TurnoverQueryService turnoverQueryService;
    private final WalletQueryService walletQueryService;

    @Override
    public List<BridgeTurnoverResponseDTO> getTodayTurnover(Long accountNumber) {
        WalletEntity wallet = walletQueryService.getWalletEntity(accountNumber);
        return BridgeTurnoverTransformer.adaptList(
                turnoverQueryService.getTodayTurnoverByUser(wallet.getUser()));
    }

    @Override
    public PaginatedResponseDTO<BridgeTurnoverResponseDTO> getHistory(Long accountNumber, BridgeTurnoverSearchRequestDTO request) {
        WalletEntity wallet = walletQueryService.getWalletEntity(accountNumber);
        LocalDateTime from = request.getFromCreatedAt() != null
                ? request.getFromCreatedAt().atStartOfDay()
                : LocalDate.now().atStartOfDay();
        LocalDateTime to = request.getToCreatedAt() != null
                ? request.getToCreatedAt().plusDays(1).atStartOfDay()
                : LocalDate.now().plusDays(1).atStartOfDay();

        TurnoverSpecificationDTO spec = TurnoverSpecificationDTO.builder()
                .user(wallet.getUser())
                .fromCreatedAt(from)
                .toCreatedAt(to)
                .build();

        PageRequest pageRequest = request.toPageRequest();

        return new PaginatedResponseDTO<>(
                turnoverQueryService.search(spec, pageRequest),
                BridgeTurnoverTransformer::adapt);
    }
}
