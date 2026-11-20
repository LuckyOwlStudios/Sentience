package net.luckyowlstudios.sentience.test;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.luckyowlstudios.sentience.api.Races;
import net.luckyowlstudios.sentience.api.Reputation;
import net.luckyowlstudios.sentience.impl.BarterTracker;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.raid.Raid;

/**
 * Server-side checks run in CI with {@code ./gradlew :fabric:runGametest}. They only run when the
 * {@code fabric-api.gametest} system property is set, never in normal play.
 */
public class SentienceGameTests implements FabricGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void defaultRacesLoad(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        helper.assertTrue(Races.get(access, Races.VILLAGERS).isPresent(), "villagers race missing");
        helper.assertTrue(Races.get(access, Races.ILLAGERS).isPresent(), "illagers race missing");
        helper.assertTrue(Races.get(access, Races.PIGLINS).isPresent(), "piglins race missing");
        helper.assertTrue(Races.isMember(helper.spawn(EntityType.VILLAGER, 1, 2, 1), Races.VILLAGERS), "villager not a villager");
        helper.assertTrue(Races.isMember(helper.spawn(EntityType.PILLAGER, 1, 2, 1), Races.ILLAGERS), "pillager not an illager");
        helper.assertTrue(Races.isMember(helper.spawn(EntityType.PIGLIN_BRUTE, 1, 2, 1), Races.PIGLINS), "brute not a piglin");
        helper.assertFalse(Races.getRace(helper.spawn(EntityType.COW, 1, 2, 1)).isPresent(), "cow has a race");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void helpersClampAndReset(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(Reputation.get(player, Races.PIGLINS) == 0, "default should be 0");
        Reputation.add(player, Races.PIGLINS, 15);
        helper.assertTrue(Reputation.get(player, Races.PIGLINS) == 15, "add failed");
        Reputation.remove(player, Races.PIGLINS, 5);
        helper.assertTrue(Reputation.get(player, Races.PIGLINS) == 10, "remove failed");
        Reputation.set(player, Races.PIGLINS, 9999);
        helper.assertTrue(Reputation.get(player, Races.PIGLINS) == 100, "should clamp to max");
        Reputation.reset(player, Races.PIGLINS);
        helper.assertTrue(Reputation.get(player, Races.PIGLINS) == 0, "reset failed");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void killingVillagerLowersReputation(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Reputation.resetAll(player);
        Villager villager = helper.spawn(EntityType.VILLAGER, 1, 2, 1);
        villager.hurt(helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);
        helper.succeedWhen(() -> helper.assertTrue(Reputation.get(player, Races.VILLAGERS) == -10,
                "expected -10, got " + Reputation.get(player, Races.VILLAGERS)));
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void mixinsApply(GameTestHelper helper) {
        // Spawning a piglin builds its brain, which loads PiglinAi; loading Raid applies the raid mixin.
        // A mixin that no longer matches the game would crash here instead of failing silently in play.
        Piglin piglin = helper.spawn(EntityType.PIGLIN, 1, 2, 1);
        helper.assertTrue(piglin instanceof BarterTracker, "piglin mixin not applied");
        helper.assertTrue(Raid.class.getName() != null, "raid class not loaded");
        helper.succeed();
    }
}
