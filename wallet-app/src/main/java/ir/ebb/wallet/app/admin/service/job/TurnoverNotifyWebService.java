package ir.ebb.wallet.app.admin.service.job;

import java.util.concurrent.CompletionStage;

public interface TurnoverNotifyWebService {

    CompletionStage<Void> aggregateUserTurnover();
}
