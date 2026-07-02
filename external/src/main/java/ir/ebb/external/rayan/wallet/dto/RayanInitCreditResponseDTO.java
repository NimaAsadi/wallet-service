package ir.ebb.external.rayan.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RayanInitCreditResponseDTO {

    private boolean isSuccessful;
    private String errorMessage;
}
