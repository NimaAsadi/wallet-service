package ir.ebb.wallet.app.admin.transformer;

import ir.ebb.wallet.app.admin.dto.request.WalletSearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.response.RayanWalletResponseDTO;
import ir.ebb.wallet.app.admin.dto.response.WalletResponseDTO;
import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;
import ir.ebb.wallet.valueobject.Wallet;
import ir.ebb.wallet.constant.valueobject.WalletParameter;
import ir.ebb.wallet.dto.WalletSpecificationDTO;
import ir.ebb.wallet.entity.WalletEntity;
import ir.ebb.wallet.entity.WalletParameterEmbedded;

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

    public static WalletResponseDTO adapt(WalletEntity e) {
        return WalletResponseDTO.builder()
                .userId(null)
                .dbsAccountNumber(e.getAccountNumber())
                .t0Balance(e.getT0().getBalance())
                .t0BuyingPower(e.getT0().getBalance())
                .t1Balance(e.getT1().getBalance())
                .t1BuyingPower(e.getT1().getBalance() + e.getT0().getBalance())
                .t2Balance(e.getT2().getBalance())
                .t2BuyingPower(e.getT2().getBalance() + e.getT1().getBalance() + e.getT0().getBalance())
                .frozen(e.getT0().getFrozen() + e.getT1().getFrozen() + e.getT2().getFrozen())
                .initialCredit(e.getInitialCredit())
                .credit(e.getCredit())
                .creditUsage(e.getInitialCredit() - e.getCredit())
                .separInitialCredit(e.getSeparInitialCredit())
                .separCredit(e.getSeparCredit())
                .separCreditUsage(e.getSeparInitialCredit() - e.getSeparCredit())
                .build();
    }

    public static WalletEntity adapt(RayanWalletDTO dto) {
        WalletEntity entity = new WalletEntity();
        return adapt(entity, dto);
    }

    public static WalletEntity adapt(WalletEntity entity, RayanWalletDTO dto) {
        long absInProgress = Math.abs(dto.inProgress());
        long t0 = dto.financialRemain() - Math.abs(dto.saleT0()) - absInProgress;
        entity.setT0(new WalletParameterEmbedded(t0, absInProgress));
        long t1 = dto.financialRemain() - absInProgress - t0;
        entity.setT1(new WalletParameterEmbedded(t1, 0L));
        entity.setT2(new WalletParameterEmbedded(0L, 0L));
        entity.setCredit(dto.customerCredit());
        entity.setInitialCredit(dto.customerCredit());
        return entity;
    }

    public static Wallet adapt(Wallet wallet, RayanWalletDTO dto) {
        long absInProgress = Math.abs(dto.inProgress());
        long t0 = dto.financialRemain() - Math.abs(dto.saleT0()) - absInProgress;
        wallet.setT0(new WalletParameter(t0, absInProgress));
        long t1 = dto.financialRemain() - absInProgress - t0;
        wallet.setT1(new WalletParameter(t1, 0L));
        wallet.setT2(new WalletParameter(0L, 0L));
        wallet.setCredit(dto.customerCredit());
        wallet.setInitialCredit(dto.customerCredit());
        wallet.setSeparCredit(0L);
        wallet.setSeparInitialCredit(0L);
        return wallet;
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
