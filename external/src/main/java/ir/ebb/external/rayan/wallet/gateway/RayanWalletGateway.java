package ir.ebb.external.rayan.wallet.gateway;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PutExchange;

import static ir.ebb.external.rayan.configuration.RayanConfig.HEADER_NAME_TOKEN;

public interface RayanWalletGateway {

    @GetExchange("/customers/remainSettlementOfNextBusinessDay")
    String getAllWallets(
            @RequestParam int dsCode,
            @RequestHeader(name = HEADER_NAME_TOKEN) String token);

    @PutExchange("/customers/initialCredit")
    String initCredit(
            @RequestHeader(name = HEADER_NAME_TOKEN) String token,
            @RequestParam long credit,
            @RequestParam int dsName,
            @RequestParam long dbsAccountNumber);
}
