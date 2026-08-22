package ir.ebb.wallet.service;

import ir.ebb.wallet.dto.CreateWalletDTO;
import ir.ebb.wallet.dto.DepositDTO;
import ir.ebb.wallet.dto.FreezeDTO;
import ir.ebb.wallet.dto.SpendDTO;
import ir.ebb.wallet.dto.UnfreezeDTO;
import ir.ebb.wallet.dto.WithdrawDTO;
import org.apache.pekko.Done;

import java.util.concurrent.CompletionStage;

public interface WalletService {

    CompletionStage<Done> createWallet(CreateWalletDTO requestDTO);

    CompletionStage<Done> deposit(DepositDTO requestDTO);

    CompletionStage<Done> withdraw(WithdrawDTO requestDTO);

    CompletionStage<Done> freeze(FreezeDTO requestDTO);

    CompletionStage<Done> unfreeze(UnfreezeDTO requestDTO);

    CompletionStage<Done> spend(SpendDTO requestDTO);
}
