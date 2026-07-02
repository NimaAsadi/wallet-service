package ir.ebb.wallet.app.admin.service;

import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;

import java.util.Map;

public interface RayanWalletJobService {

    void updateFromRayan(Map<Long, RayanWalletDTO> allRayanWallets) throws ApplicationException;

    void syncWallets(Map<Long, RayanWalletDTO> allRayanWallets);
}
