package net.luckyowlstudios.sentience.trading;

import net.luckyowlstudios.sentience.api.Race;
import net.luckyowlstudios.sentience.api.Races;
import net.luckyowlstudios.sentience.api.Reputation;
import net.luckyowlstudios.sentience.api.TradeTable;
import net.luckyowlstudios.sentience.impl.ReputationTriggers;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reputation-gated trading and peacefulness for mobs that aren't villagers.
 */
public final class SentienceTrading {

    /** Trading stops if the player walks further away than this. */
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private SentienceTrading() {
    }

    public static Optional<TradeTable> tradeTable(Mob mob) {
        return mob.level().registryAccess().registry(TradeTable.REGISTRY).flatMap(registry -> registry.stream()
                .filter(table -> table.entities().contains(mob.getType().builtInRegistryHolder()))
                .findFirst());
    }

    /** Whether this player's reputation is high enough to trade with this mob's race. */
    public static boolean hasTradeReputation(Mob mob, ServerPlayer player) {
        return Races.getRace(mob)
                .flatMap(race -> race.value().tradesAt().map(threshold -> Reputation.isAtLeast(player, race.key(), threshold)))
                .orElse(false);
    }

    /**
     * Whether this mob would rather not start a fight with this player: their reputation with its race is at
     * least {@code peaceful_at}, the player hasn't just hurt it, and it isn't part of an active raid.
     */
    public static boolean isPeacefulTowards(Mob mob, Player target) {
        if (!(target instanceof ServerPlayer player)) {
            return false;
        }
        if (mob.getLastHurtByMob() == player) {
            return false;
        }
        if (mob instanceof Raider raider && raider.hasActiveRaid()) {
            return false;
        }
        return Races.getRace(mob)
                .flatMap(race -> race.value().peacefulAt().map(threshold -> Reputation.isAtLeast(player, race.key(), threshold)))
                .orElse(false);
    }

    private static boolean isHostileTowards(Mob mob, Player player) {
        if (mob.getTarget() == player) {
            return true;
        }
        Optional<LivingEntity> brainTarget = mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                ? mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET)
                : Optional.empty();
        return brainTarget.isPresent() && brainTarget.get() == player;
    }

    /**
     * Right-click on a mob. Opens its trades if it has a trade table and the player is trusted enough.
     *
     * @return true if the click was handled (trades opened, or the player was told why not)
     */
    public static boolean tryOpenTrades(Mob mob, ServerPlayer player) {
        if (mob instanceof Merchant || !mob.isAlive() || mob.isBaby()) {
            return false;
        }
        Optional<TradeTable> table = tradeTable(mob);
        Optional<Holder.Reference<Race>> race = Races.getRace(mob);
        if (table.isEmpty() || race.isEmpty() || race.get().value().tradesAt().isEmpty()) {
            return false;
        }
        if (isHostileTowards(mob, player)) {
            return false;
        }
        int threshold = race.get().value().tradesAt().get();
        int reputation = Reputation.get(player, race.get().key());
        if (reputation < threshold) {
            player.displayClientMessage(Component.translatable("sentience.trading.not_trusted",
                    race.get().value().coloredName(), reputation, threshold), true);
            return true;
        }
        TradingMob state = (TradingMob) mob;
        Player current = state.sentience$getTradingPlayer();
        if (current != null && current != player && current.isAlive()) {
            return true; // busy with someone else
        }
        prepareOffers(mob, table.get());
        MerchantOffers offers = state.sentience$getOffers();
        if (offers == null || offers.isEmpty()) {
            return false;
        }
        SoundEvent sound = table.get().tradeSound().map(Holder::value).orElse(SoundEvents.VILLAGER_YES);
        MobMerchant merchant = new MobMerchant(mob, sound);
        merchant.setTradingPlayer(player);
        mob.getNavigation().stop();
        merchant.openTradingScreen(player, mob.getDisplayName(), 0);
        return true;
    }

    /** Picks this mob's trades the first time, and restocks used-up trades once enough time has passed. */
    static void prepareOffers(Mob mob, TradeTable table) {
        TradingMob state = (TradingMob) mob;
        long now = mob.level().getGameTime();
        if (state.sentience$getOffers() == null) {
            List<TradeTable.Trade> pool = new ArrayList<>(table.trades());
            int count = Math.min(pool.size(), table.offersPerMob().orElse(pool.size()));
            MerchantOffers offers = new MerchantOffers();
            for (int i = 0; i < count; i++) {
                offers.add(pool.remove(mob.getRandom().nextInt(pool.size())).toOffer());
            }
            state.sentience$setOffers(offers);
            state.sentience$setLastRestock(now);
        } else if (now - state.sentience$getLastRestock() >= table.restockTicks()) {
            state.sentience$getOffers().forEach(MerchantOffer::resetUses);
            state.sentience$setLastRestock(now);
        }
    }

    /** Whether an open trade screen should stay open. */
    public static boolean canKeepTrading(Mob mob, Player player) {
        return mob.isAlive() && player.isAlive() && !player.isRemoved()
                && mob.distanceToSqr(player) <= MAX_DISTANCE_SQR
                && player instanceof ServerPlayer serverPlayer && hasTradeReputation(mob, serverPlayer)
                && !isHostileTowards(mob, player);
    }

    /** Server tick of a mob that is trading: hold still and look at the player, or end the trade. */
    public static void tickTrading(Mob mob) {
        TradingMob state = (TradingMob) mob;
        Player player = state.sentience$getTradingPlayer();
        if (player == null) {
            return;
        }
        if (!canKeepTrading(mob, player)) {
            state.sentience$setTradingPlayer(null);
            if (player.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu) {
                player.closeContainer();
            }
            return;
        }
        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
    }

    static void onTrade(Mob mob, ServerPlayer player) {
        ReputationTriggers.onTrade(mob, player);
    }
}
