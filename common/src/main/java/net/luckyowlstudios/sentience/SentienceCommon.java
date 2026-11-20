package net.luckyowlstudios.sentience;

/**
 * Shared entry point. Called by each loader during mod construction.
 */
public final class SentienceCommon {

    private SentienceCommon() {
    }

    public static void init() {
        Constants.LOG.info("Sentience is keeping track of who likes you.");
    }
}
