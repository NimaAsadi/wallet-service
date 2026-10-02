package ir.ebb.wallet.service;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.wallet.actor.WalletSnapshot;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.dto.AddCreditDTO;
import ir.ebb.wallet.dto.CreateWalletDTO;
import ir.ebb.wallet.dto.DepositDTO;
import ir.ebb.wallet.dto.FreezeDTO;
import ir.ebb.wallet.dto.SpendDTO;
import ir.ebb.wallet.dto.UnfreezeDTO;
import ir.ebb.wallet.dto.WithdrawDTO;
import org.apache.pekko.Done;

import java.util.concurrent.CompletionStage;

public interface WalletService {

    /**
     * Strongly-consistent single-wallet read — served by the sharded entity's in-memory state,
     * not the projection-fed read model. Rejects with {@code WALLET_NOT_EXIST} (4001) when the
     * wallet was never created.
     */
    CompletionStage<WalletSnapshot> getWallet(Long dbsAccountNumber);

    /** Strongly-consistent buying-power read for one settlement delay (entity-served). */
    CompletionStage<BuyingPower> getBuyingPower(Long dbsAccountNumber, SettlementDelay settlementDelay);

    CompletionStage<Done> createWallet(CreateWalletDTO requestDTO);

    CompletionStage<Done> deposit(DepositDTO requestDTO);

    CompletionStage<Done> withdraw(WithdrawDTO requestDTO);

    CompletionStage<Done> freeze(FreezeDTO requestDTO);

    CompletionStage<Done> unfreeze(UnfreezeDTO requestDTO);

    CompletionStage<Done> spend(SpendDTO requestDTO);

    CompletionStage<Done> addCredit(AddCreditDTO requestDTO);
}
