package ir.ebb.wallet.wallet;

/**
 * Tags wallet entity events for the slice-based ({@code eventsBySlices}) R2DBC projection.
 *
 * <p>With {@code eventsBySlices} the projection consumes by entity type + slice range — the 1024
 * slices are derived deterministically from each entity's persistence id, so all wallet events
 * share a single tag. Splitting the slices across {@code N} projection workers happens in
 * {@code ProjectionBootstrap} via {@code EventSourcedProvider.sliceRanges}. Unlike the old
 * {@code eventsByTag} scheme, the number of parallel streams can be changed later without
 * re-tagging the journal.
 */
public final class WalletTags {

    /** Single tag applied to every wallet event (the slice, not the tag, drives partitioning). */
    public static final String TAG = "wallet";

    private WalletTags() {}
}
