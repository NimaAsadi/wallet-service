package ir.ebb.wallet.app.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WalletRequestDTO(
        @NotBlank String userId,
        @NotNull Long dbsAccountNumber
) {}
