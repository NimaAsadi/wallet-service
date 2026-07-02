package ir.ebb.wallet.repository;

import ir.ebb.wallet.aggregate.Wallet;

public interface WalletRepositoryCustom {

    int updateWalletNative(Wallet wallet);

    void updateWalletDebtNative(Wallet wallet);
}
