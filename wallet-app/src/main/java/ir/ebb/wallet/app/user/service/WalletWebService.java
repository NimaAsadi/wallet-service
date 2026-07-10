package ir.ebb.wallet.app.user.service;

import ir.ebb.base.security.UserPrincipal;
import ir.ebb.wallet.app.user.dto.response.WalletResponseDTO;

public interface WalletWebService {

    WalletResponseDTO getWalletDetails(UserPrincipal principal);
}
