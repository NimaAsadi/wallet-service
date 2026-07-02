package ir.ebb.external.rayan.wallet.service.command;

import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.external.rayan.wallet.dto.RayanInitCreditResponseDTO;
import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;

import java.util.Collection;

public interface RayanWalletCommandService {

    void deleteAll();

    void saveAll(Collection<RayanWalletDTO> rayanWalletDTOS) throws ApplicationException;

    RayanInitCreditResponseDTO initCredit(long credit, long dbsAccountNumber);
}
