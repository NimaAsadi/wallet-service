package ir.ebb.wallet.projection.repository;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-mapping tests for {@link WalletRepository#selectOneWithDebt} — no DB: feeds hand-rolled
 * name-backed {@link Row} fakes straight into {@code mapRowWithDebt} to guard the joined SELECT's
 * column labels (the {@code debt_*} aliases included) and the LEFT-JOIN fallback for wallets
 * with no {@code wallet_debt} row.
 */
class WalletRepositoryTest {

    private static final UUID WALLET_ID = UUID.randomUUID();

    private final WalletRepository repository = new WalletRepository();

    @Test
    void mapRowWithDebtMapsBothEntitiesFromFullJoinedRow() {
        LocalDateTime walletCreatedAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime walletUpdatedAt = LocalDateTime.of(2026, 1, 2, 11, 30);
        LocalDateTime debtCreatedAt = LocalDateTime.of(2026, 1, 1, 10, 5);
        LocalDateTime debtUpdatedAt = LocalDateTime.of(2026, 1, 2, 11, 35);

        Map<String, Object> values = new HashMap<>();
        values.put("id", WALLET_ID);
        values.put("version", 7L);
        values.put("account_number", 1234567L);
        values.put("t0_balance", 100L);
        values.put("t0_frozen", 40L);
        values.put("t1_balance", 200L);
        values.put("t1_frozen", 0L);
        values.put("t2_balance", 300L);
        values.put("t2_frozen", 60L);
        values.put("credit", 11L);
        values.put("initial_credit", 22L);
        values.put("separ_credit", 33L);
        values.put("separ_initial_credit", 44L);
        values.put("created_at", walletCreatedAt);
        values.put("updated_at", walletUpdatedAt);
        values.put("wallet_id", WALLET_ID);
        values.put("t2_to_t0_debt", 5L);
        values.put("t2_to_t1_debt", 6L);
        values.put("t1_to_t0_debt", 7L);
        values.put("t2_to_credit_debt", 8L);
        values.put("t1_to_credit_debt", 9L);
        values.put("t2_to_separ_credit_debt", 10L);
        values.put("debt_created_at", debtCreatedAt);
        values.put("debt_updated_at", debtUpdatedAt);

        WalletWithDebt pair = repository.mapRowWithDebt(fakeRow(values));

        var wallet = pair.wallet();
        assertThat(wallet.id).isEqualTo(WALLET_ID);
        assertThat(wallet.version).isEqualTo(7L);
        assertThat(wallet.accountNumber).isEqualTo(1234567L);
        assertThat(wallet.t0Balance).isEqualTo(100L);
        assertThat(wallet.t0Frozen).isEqualTo(40L);
        assertThat(wallet.t1Balance).isEqualTo(200L);
        assertThat(wallet.t1Frozen).isZero();
        assertThat(wallet.t2Balance).isEqualTo(300L);
        assertThat(wallet.t2Frozen).isEqualTo(60L);
        assertThat(wallet.credit).isEqualTo(11L);
        assertThat(wallet.initialCredit).isEqualTo(22L);
        assertThat(wallet.separCredit).isEqualTo(33L);
        assertThat(wallet.separInitialCredit).isEqualTo(44L);
        assertThat(wallet.createdAt).isEqualTo(walletCreatedAt);
        assertThat(wallet.updatedAt).isEqualTo(walletUpdatedAt);

        var debt = pair.walletDebt();
        assertThat(debt.walletId).isEqualTo(WALLET_ID);
        assertThat(debt.t2ToT0Debt).isEqualTo(5L);
        assertThat(debt.t2ToT1Debt).isEqualTo(6L);
        assertThat(debt.t1ToT0Debt).isEqualTo(7L);
        assertThat(debt.t2ToCreditDebt).isEqualTo(8L);
        assertThat(debt.t1ToCreditDebt).isEqualTo(9L);
        assertThat(debt.t2ToSeparCreditDebt).isEqualTo(10L);
        assertThat(debt.t1ToSeparCreditDebt).isNull();
        assertThat(debt.createdAt).isEqualTo(debtCreatedAt);
        assertThat(debt.updatedAt).isEqualTo(debtUpdatedAt);
    }

    @Test
    void mapRowWithDebtYieldsZeroedDebtEntityWhenDebtRowMissing() {
        Map<String, Object> values = new HashMap<>();
        values.put("id", WALLET_ID);
        values.put("version", 1L);
        values.put("account_number", 42L);
        values.put("t0_balance", 10L);
        values.put("t0_frozen", 0L);
        values.put("t1_balance", 0L);
        values.put("t1_frozen", 0L);
        values.put("t2_balance", 0L);
        values.put("t2_frozen", 0L);
        values.put("credit", 0L);
        values.put("initial_credit", 0L);
        values.put("separ_credit", 0L);
        values.put("separ_initial_credit", 0L);
        // no wallet_id / debt columns — LEFT JOIN found no wallet_debt row

        WalletWithDebt pair = repository.mapRowWithDebt(fakeRow(values));

        assertThat(pair.wallet().id).isEqualTo(WALLET_ID);
        assertThat(pair.wallet().t0Balance).isEqualTo(10L);

        var debt = pair.walletDebt();
        assertThat(debt.walletId).isEqualTo(WALLET_ID);
        assertThat(debt.t2ToT0Debt).isZero();
        assertThat(debt.t2ToT1Debt).isZero();
        assertThat(debt.t1ToT0Debt).isZero();
        assertThat(debt.t2ToCreditDebt).isZero();
        assertThat(debt.t1ToCreditDebt).isZero();
        assertThat(debt.t2ToSeparCreditDebt).isZero();
        assertThat(debt.t1ToSeparCreditDebt).isZero();
        assertThat(debt.createdAt).isNull();
        assertThat(debt.updatedAt).isNull();
    }

    /**
     * Name-backed {@link Row} fake: r2dbc-spi 1.0.0 has exactly three abstract methods and the
     * mapper only uses the name-based reads; absent keys read as null, like a real driver.
     */
    private static Row fakeRow(Map<String, Object> values) {
        return new Row() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(String name, Class<T> type) {
                return (T) values.get(name);
            }

            @Override
            public <T> T get(int index, Class<T> type) {
                throw new UnsupportedOperationException("name-based access only");
            }

            @Override
            public RowMetadata getMetadata() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
