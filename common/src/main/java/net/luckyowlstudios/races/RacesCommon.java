package net.luckyowlstudios.races;

/**
 * Shared entry point. Called by each loader during mod construction.
 */
public final class RacesCommon {

    private RacesCommon() {
    }

    public static void init() {
        Constants.LOG.info("Races is keeping track of who likes you.");
    }
}
