package ir.ebb.wallet.app.admin.transformer;

import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletSearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.response.RayanWalletResponseDTO;
import ir.ebb.wallet.app.admin.dto.response.WalletResponseDTO;
import ir.ebb.wallet.dto.WalletSpecificationDTO;
import ir.ebb.wallet.projection.entity.WalletEntity;

public final class WalletTransformer {

    private WalletTransformer() {}

    public static WalletSpecificationDTO adapt(WalletSearchRequestDTO req) {
        return WalletSpecificationDTO.builder()
                .userIds(req.getUserIds())
                .accountNumbers(req.getAccountNumbers())
                .fromFrozen(req.getFromFrozen())
                .toFrozen(req.getToFrozen())
                .fromCredit(req.getFromCredit())
                .toCredit(req.getToCredit())
                .fromT0Balance(req.getFromT0Balance())
                .toT0Balance(req.getToT0Balance())
                .fromT1Balance(req.getFromT1Balance())
                .toT1Balance(req.getToT1Balance())
                .fromT2Balance(req.getFromT2Balance())
                .toT2Balance(req.getToT2Balance())
                .fromInitialCredit(req.getFromInitialCredit())
                .toInitialCredit(req.getToInitialCredit())
                .build();
    }

    /** Flat projection wallet row → admin response (buying power = cumulative tier balances). */
    public static WalletResponseDTO adapt(WalletEntity e) {
        return WalletResponseDTO.builder()
                .userId(null) // the projection wallet carries no user column
                .dbsAccountNumber(e.getAccountNumber())
                .t0Balance(e.getT0Balance())
                .t0BuyingPower(e.getT0Balance())
                .t1Balance(e.getT1Balance())
                .t1BuyingPower(e.getT1Balance() + e.getT0Balance())
                .t2Balance(e.getT2Balance())
                .t2BuyingPower(e.getT2Balance() + e.getT1Balance() + e.getT0Balance())
                .frozen(e.getT0Frozen() + e.getT1Frozen() + e.getT2Frozen())
                .initialCredit(e.getInitialCredit())
                .credit(e.getCredit())
                .creditUsage(e.getInitialCredit() - e.getCredit())
                .separInitialCredit(e.getSeparInitialCredit())
                .separCredit(e.getSeparCredit())
                .separCreditUsage(e.getSeparInitialCredit() - e.getSeparCredit())
                .build();
    }

    public static RayanWalletResponseDTO adaptForAdmin(RayanWalletDTO dto) {
        RayanWalletResponseDTO result = new RayanWalletResponseDTO();
        long absInProgress = Math.abs(dto.inProgress());
        long t0 = dto.financialRemain() - Math.abs(dto.saleT0()) - absInProgress;
        result.setT0BuyingPower(t0);
        result.setT0Frozen(absInProgress);
        long t1 = dto.financialRemain() - absInProgress - t0;
        result.setT1BuyingPower(t1);
        result.setT1Frozen(0L);
        result.setT2BuyingPower(0L);
        result.setT2Frozen(0L);
        result.setCredit(dto.customerCredit());
        return result;
    }
}
