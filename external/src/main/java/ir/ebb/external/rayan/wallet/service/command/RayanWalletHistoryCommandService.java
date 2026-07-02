package ir.ebb.external.rayan.wallet.service.command;

import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;

import java.util.Collection;

public interface RayanWalletHistoryCommandService {

    void saveAll(Collection<RayanWalletDTO> rayanWalletDTOS);

    void deleteByDateBefore(int days);

    void deleteTodayWallets();
}
