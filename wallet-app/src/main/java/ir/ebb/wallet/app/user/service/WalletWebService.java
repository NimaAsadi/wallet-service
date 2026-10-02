package ir.ebb.wallet.app.user.service;

import ir.ebb.base.security.UserPrincipal;
import ir.ebb.wallet.app.user.dto.response.WalletResponseDTO;

import java.util.concurrent.CompletionStage;

public interface WalletWebService {

    CompletionStage<WalletResponseDTO> getWalletDetails(UserPrincipal principal);
}
