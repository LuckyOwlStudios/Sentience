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
import net.luckyowlstudios.sentience.village.SentienceVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.DyeColor;
import java.util.Optional;
import net.luckyowlstudios.sentience.kingdom.Kingdom;
import net.luckyowlstudios.sentience.kingdom.Kingdoms;
import net.luckyowlstudios.sentience.kingdom.KingdomData;
import net.luckyowlstudios.sentience.kingdom.KingdomTeams;
import net.luckyowlstudios.sentience.network.CreateKingdomPayload;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.LivingEntity;
import net.luckyowlstudios.sentience.trading.SentienceTrading;
import net.luckyowlstudios.sentience.trading.TradingMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.inventory.MerchantMenu;

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

    @GameTest(template = EMPTY_STRUCTURE)
    public void tradeTablesLoad(GameTestHelper helper) {
        helper.assertTrue(SentienceTrading.tradeTable(helper.spawn(EntityType.PIGLIN, 1, 2, 1)).isPresent(), "piglin trades missing");
        helper.assertTrue(SentienceTrading.tradeTable(helper.spawn(EntityType.PILLAGER, 1, 2, 1)).isPresent(), "pillager trades missing");
        helper.assertTrue(SentienceTrading.tradeTable(helper.spawn(EntityType.VINDICATOR, 1, 2, 1)).isPresent(), "vindicator trades missing");
        helper.assertTrue(SentienceTrading.tradeTable(helper.spawn(EntityType.EVOKER, 1, 2, 1)).isPresent(), "evoker trades missing");
        helper.assertFalse(SentienceTrading.tradeTable(helper.spawn(EntityType.PIGLIN_BRUTE, 1, 2, 1)).isPresent(), "brutes shouldn't trade");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void tradingNeedsReputation(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Pillager pillager = helper.spawn(EntityType.PILLAGER, 1, 2, 1);
        player.moveTo(pillager.getX() + 1, pillager.getY(), pillager.getZ());

        Reputation.set(player, Races.ILLAGERS, 19);
        SentienceTrading.tryOpenTrades(pillager, player);
        helper.assertFalse(player.containerMenu instanceof MerchantMenu, "traded below 20 reputation");

        Reputation.set(player, Races.ILLAGERS, 20);
        pillager.setTarget(null);
        helper.assertTrue(SentienceTrading.tryOpenTrades(pillager, player), "trade click not handled");
        helper.assertTrue(player.containerMenu instanceof MerchantMenu, "trade screen didn't open at 20 reputation");
        helper.assertTrue(((TradingMob) pillager).sentience$getOffers().size() == 4, "pillager should offer 4 trades");

        Reputation.set(player, Races.ILLAGERS, 10);
        SentienceTrading.tickTrading(pillager);
        helper.assertFalse(player.containerMenu instanceof MerchantMenu, "trade screen stayed open after reputation dropped");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void trustedPlayersAreNotTargeted(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Pillager pillager = helper.spawn(EntityType.PILLAGER, 1, 2, 1);

        Reputation.set(player, Races.ILLAGERS, 0);
        pillager.setTarget(player);
        helper.assertTrue(pillager.getTarget() == player, "pillager should target an untrusted player");

        pillager.setTarget(null);
        Reputation.set(player, Races.ILLAGERS, 20);
        pillager.setTarget(player);
        helper.assertTrue(pillager.getTarget() == null, "pillager targeted a trusted player");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void tradesAreSaved(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Pillager pillager = helper.spawn(EntityType.PILLAGER, 1, 2, 1);
        player.moveTo(pillager.getX() + 1, pillager.getY(), pillager.getZ());
        Reputation.set(player, Races.ILLAGERS, 50);
        pillager.setTarget(null);
        SentienceTrading.tryOpenTrades(pillager, player);
        player.closeContainer();

        CompoundTag tag = pillager.saveWithoutId(new CompoundTag());
        Pillager copy = EntityType.PILLAGER.create(helper.getLevel());
        helper.assertTrue(copy != null, "couldn't create pillager");
        copy.load(tag);
        var saved = ((TradingMob) copy).sentience$getOffers();
        helper.assertTrue(saved != null && saved.size() == 4, "trades weren't saved");
        helper.assertTrue(ItemStack.isSameItemSameComponents(saved.get(0).getResult(),
                ((TradingMob) pillager).sentience$getOffers().get(0).getResult()), "saved trades differ");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "cat_witnessed")
    public void hurtingVillageCatNearVillagerCostsReputation(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.spawn(EntityType.VILLAGER, 1, 2, 1);
        Cat cat = helper.spawn(EntityType.CAT, 2, 2, 2);
        Reputation.set(player, Races.VILLAGERS, 0);

        cat.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.assertTrue(Reputation.get(player, Races.VILLAGERS) == -2,
                "hurting a village cat: expected -2, got " + Reputation.get(player, Races.VILLAGERS));

        cat.invulnerableTime = 0;
        cat.hurt(helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);
        helper.assertTrue(Reputation.get(player, Races.VILLAGERS) == -17,
                "killing a village cat: expected -17, got " + Reputation.get(player, Races.VILLAGERS));
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "cat_witnessed")
    public void hurtingATamedCatIsNotAVillageMatter(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.spawn(EntityType.VILLAGER, 1, 2, 1);
        Cat cat = helper.spawn(EntityType.CAT, 2, 2, 2);
        cat.tame(player);
        Reputation.set(player, Races.VILLAGERS, 0);
        cat.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.assertTrue(Reputation.get(player, Races.VILLAGERS) == 0, "hurting your own cat cost reputation");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "cat_alone")
    public void hurtingCatWithNoVillagerAroundIsFree(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Cat cat = helper.spawn(EntityType.CAT, 2, 2, 2);
        // Clear out any villager left over from another test nearby.
        helper.getLevel().getEntitiesOfClass(LivingEntity.class, cat.getBoundingBox().inflate(20),
                entity -> Races.isMember(entity, Races.VILLAGERS)).forEach(LivingEntity::discard);
        Reputation.set(player, Races.VILLAGERS, 0);
        cat.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.assertTrue(Reputation.get(player, Races.VILLAGERS) == 0, "no villager saw it, but reputation changed");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void killingRaidersDuringARaidPleasesVillagers(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Reputation.set(player, Races.VILLAGERS, 0);
        Reputation.set(player, Races.ILLAGERS, 0);

        Pillager raider = helper.spawn(EntityType.PILLAGER, 1, 2, 1);
        raider.setCurrentRaid(new Raid(9999, helper.getLevel(), helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1))));
        helper.assertTrue(raider.hasActiveRaid(), "test raid isn't active");
        raider.hurt(helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);
        helper.assertTrue(Reputation.get(player, Races.VILLAGERS) == 2,
                "killing a raider: expected villagers +2, got " + Reputation.get(player, Races.VILLAGERS));
        helper.assertTrue(Reputation.get(player, Races.ILLAGERS) == -5, "illager kill penalty should still apply");

        Pillager patrol = helper.spawn(EntityType.PILLAGER, 2, 2, 2);
        patrol.hurt(helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);
        helper.assertTrue(Reputation.get(player, Races.VILLAGERS) == 2, "killing a pillager outside a raid shouldn't please villagers");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void townHallIsAJobSite(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, SentienceVillage.townHall.get());
        helper.succeedWhen(() -> helper.assertTrue(helper.getLevel().getPoiManager().getType(helper.absolutePos(pos))
                .map(type -> type.is(SentienceVillage.TOWN_HALL_POI)).orElse(false), "town hall isn't a point of interest"));
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void villagerAtTownHallBecomesGuide(GameTestHelper helper) {
        // On the floor, so the villager can walk up to it (y 2 would leave it floating out of reach).
        helper.setBlock(new BlockPos(1, 0, 1), SentienceVillage.townHall.get());
        Villager villager = helper.spawn(EntityType.VILLAGER, 3, 0, 3);
        helper.succeedWhen(() -> {
            helper.assertTrue(villager.isAlive(), "villager died");
            helper.assertTrue(villager.getVillagerData().getProfession() == SentienceVillage.guide.get(),
                    "villager hasn't become the Guide yet (is " + villager.getVillagerData().getProfession()
                            + ", job site memory " + villager.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.POTENTIAL_JOB_SITE) + ")");
        });
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void foundingAKingdomAtATownHall(GameTestHelper helper) {
        BlockPos local = new BlockPos(1, 2, 1);
        helper.setBlock(local, SentienceVillage.townHall.get());
        BlockPos hall = helper.absolutePos(local);
        String name = "Testoria " + helper.getLevel().getGameTime();

        ServerPlayer founder = helper.makeMockServerPlayerInLevel();
        founder.moveTo(hall.getX() + 1.5, hall.getY(), hall.getZ() + 0.5);
        helper.assertTrue(Kingdoms.cleanName("   ").isEmpty(), "blank names should be refused");
        helper.assertTrue(Kingdoms.tryCreate(founder, new CreateKingdomPayload(hall, "   ", DyeColor.RED)).isEmpty(), "founded with a blank name");

        Optional<Kingdom> kingdom = Kingdoms.tryCreate(founder, new CreateKingdomPayload(hall, "  " + name + "  ", DyeColor.RED));
        helper.assertTrue(kingdom.isPresent(), "couldn't found a kingdom");
        helper.assertTrue(kingdom.get().name().equals(name), "name wasn't trimmed: '" + kingdom.get().name() + "'");
        helper.assertTrue(kingdom.get().color() == DyeColor.RED, "wrong colour");
        helper.assertTrue(Kingdoms.ofPlayer(founder).isPresent(), "founder isn't in the kingdom");
        helper.assertTrue(Kingdoms.atTownHall(founder.server, GlobalPos.of(helper.getLevel().dimension(), hall)).isPresent(),
                "town hall isn't set up");

        ServerPlayer other = helper.makeMockServerPlayerInLevel();
        other.moveTo(hall.getX() + 1.5, hall.getY(), hall.getZ() + 0.5);
        helper.assertTrue(Kingdoms.tryCreate(other, new CreateKingdomPayload(hall, name + " II", DyeColor.BLUE)).isEmpty(),
                "a second kingdom was founded at a set-up town hall");

        BlockPos local2 = new BlockPos(3, 2, 3);
        helper.setBlock(local2, SentienceVillage.townHall.get());
        BlockPos hall2 = helper.absolutePos(local2);
        helper.assertTrue(Kingdoms.tryCreate(other, new CreateKingdomPayload(hall2, name.toUpperCase(), DyeColor.BLUE)).isEmpty(),
                "two kingdoms got the same name");
        helper.assertTrue(Kingdoms.tryCreate(founder, new CreateKingdomPayload(hall2, name + " II", DyeColor.BLUE)).isEmpty(),
                "a player founded a second kingdom");
        helper.assertTrue(Kingdoms.tryCreate(other, new CreateKingdomPayload(hall2, name + " II", DyeColor.BLUE)).isPresent(),
                "second player couldn't found their own kingdom at a new town hall");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void kingdomsWorkLikeTeams(GameTestHelper helper) {
        BlockPos local = new BlockPos(1, 0, 1);
        helper.setBlock(local, SentienceVillage.townHall.get());
        BlockPos hall = helper.absolutePos(local);
        String name = "Teamland " + helper.getLevel().getGameTime();

        ServerPlayer leader = helper.makeMockServerPlayerInLevel();
        leader.moveTo(hall.getX() + 1.5, hall.getY(), hall.getZ() + 0.5);
        Kingdom kingdom = Kingdoms.tryCreate(leader, new CreateKingdomPayload(hall, name, DyeColor.GREEN)).orElseThrow();
        ServerPlayer friend = helper.makeMockServerPlayerInLevel();
        ServerPlayer stranger = helper.makeMockServerPlayerInLevel();

        helper.assertFalse(Kingdoms.join(friend, name), "joined without an invite");
        helper.assertFalse(Kingdoms.invite(friend, stranger), "a non-member invited someone");
        helper.assertTrue(Kingdoms.invite(leader, friend), "leader couldn't invite");
        helper.assertTrue(Kingdoms.join(friend, name.toLowerCase()), "invited player couldn't join");
        helper.assertTrue(kingdom.isMember(friend.getUUID()), "friend isn't a member");
        helper.assertFalse(kingdom.isInvited(friend.getUUID()), "invite wasn't used up");
        helper.assertTrue(leader.server.getScoreboard().getPlayerTeam(KingdomTeams.teamName(kingdom)) != null, "no vanilla team");

        helper.assertFalse(Kingdoms.kick(friend, leader.getUUID()), "a member kicked the leader");
        helper.assertFalse(Kingdoms.kick(leader, leader.getUUID()), "leader kicked themselves");
        helper.assertTrue(Kingdoms.kick(leader, friend.getUUID()), "leader couldn't kick");
        helper.assertFalse(kingdom.isMember(friend.getUUID()), "kicked player is still a member");

        helper.assertFalse(Kingdoms.setBanner(leader, new ItemStack(net.minecraft.world.item.Items.STICK)), "a stick became the banner");
        helper.assertTrue(Kingdoms.setBanner(leader, new ItemStack(net.minecraft.world.item.Items.GREEN_BANNER, 3)), "couldn't set banner");
        helper.assertTrue(kingdom.banner().map(b -> b.is(net.minecraft.world.item.Items.GREEN_BANNER) && b.getCount() == 1).orElse(false), "banner not saved");

        Kingdoms.invite(leader, friend);
        Kingdoms.join(friend, name);
        helper.assertTrue(Kingdoms.leave(leader), "leader couldn't leave");
        helper.assertTrue(kingdom.isLeader(friend.getUUID()), "leadership didn't pass to the remaining member");
        helper.assertTrue(Kingdoms.leave(friend), "last member couldn't leave");
        helper.assertTrue(KingdomData.get(leader.server).byName(name).isEmpty(), "empty kingdom wasn't disbanded");
        helper.assertTrue(Kingdoms.atTownHall(leader.server, GlobalPos.of(helper.getLevel().dimension(), hall)).isEmpty(),
                "disbanded kingdom still holds the town hall");
        helper.succeed();
    }
}
