package ir.ebb.wallet.wallet;

import java.util.ArrayList;
import java.util.List;

/**
 * Assigns each wallet entity's events to one of {@link #NUM_TAGS} tags based on the
 * account number, so {@code EventsByTag} projections can run a worker per tag in
 * parallel ({@code wallet-0} … {@code wallet-15}). Keeping a wallet's events on a
 * stable tag preserves event ordering for that wallet within a tag stream.
 */
public final class WalletTags {

    /** Number of parallel event streams (tune for cluster size). */
    public static final int NUM_TAGS = 16;

    private WalletTags() {}

    public static String tagFor(long accountNumber) {
        return "wallet-" + Math.floorMod(accountNumber, NUM_TAGS);
    }

    public static List<String> allTags() {
        List<String> tags = new ArrayList<>(NUM_TAGS);
        for (int i = 0; i < NUM_TAGS; i++) {
            tags.add("wallet-" + i);
        }
        return tags;
    }
}
