package ir.ebb.wallet.service.credit.command;

import ir.ebb.wallet.projection.entity.CreditHistoryEntity;

import java.util.concurrent.CompletionStage;

/**
 * Direct-write audit trail for admin credit ops (the admin metadata is not part of the wallet
 * events) — mirrors {@code TurnoverCommandService}.
 */
public interface CreditHistoryCommandService {

    CompletionStage<Void> save(CreditHistoryEntity entity);
}
