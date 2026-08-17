package ir.ebb.wallet.service;

import ir.ebb.wallet.dto.CreateWalletDTO;
import org.apache.pekko.Done;

import java.util.concurrent.CompletionStage;

public interface WalletService {

    CompletionStage<Done> createWallet(CreateWalletDTO requestDTO);
}
