package ir.ebb.wallet.app.bridge.bidardeposit.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record BidarDepositWalletDepositRequestDTO(
        @NotNull Long dbsAccountNumber,
        @NotNull @PositiveOrZero Long requestAmount,
        @NotNull UUID trackingId
) {}
