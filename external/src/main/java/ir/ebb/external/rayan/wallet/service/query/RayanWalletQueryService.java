package ir.ebb.external.rayan.wallet.service.query;

import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;

import java.util.Map;

public interface RayanWalletQueryService {

    Map<Long, RayanWalletDTO> getAllWallets() throws ApplicationException;

    RayanWalletDTO getUserWallet(Long dbsAccountNumber) throws ApplicationException;

    RayanWalletDTO findUserWallet(Long dbsAccountNumber);
}
