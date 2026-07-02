package ir.ebb.wallet.app.bridge.bidardeposit.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BidarDepositWalletSpendRequestDTO(
        @NotNull Long dbsAccountNumber,
        @NotNull Long requestAmount,
        @NotNull UUID trackingId
) {}
