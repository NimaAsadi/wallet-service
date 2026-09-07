package ir.ebb.wallet.app.admin.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record WalletInitCreditRequestDTO(
        @Valid WalletRequestDTO walletRequestDTO,
        @NotNull @Min(value = 0) @Max(value = Long.MAX_VALUE) Long credit
) {}
