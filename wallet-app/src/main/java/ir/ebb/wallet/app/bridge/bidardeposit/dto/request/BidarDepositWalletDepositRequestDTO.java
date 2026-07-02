package ir.ebb.wallet.app.bridge.bidardeposit.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record BidarDepositWalletDepositRequestDTO(
        @NotNull Long dbsAccountNumber,
        @NotNull @Positive Long requestAmount,
        @NotNull UUID trackingId
) {}
