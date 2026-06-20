package io.gdx.cdda.bn.nextgen.worldgen.mutable;

/** Weighted piece pick within one mutable special phase (W6 v1). */
public final class MutablePhaseEntry {

    private final String pieceId;
    private final int maxCount;

    public MutablePhaseEntry(final String pieceId, final int maxCount) {
        this.pieceId = pieceId;
        this.maxCount = Math.max(0, maxCount);
    }

    public String getPieceId() {
        return pieceId;
    }

    public int getMaxCount() {
        return maxCount;
    }
}
