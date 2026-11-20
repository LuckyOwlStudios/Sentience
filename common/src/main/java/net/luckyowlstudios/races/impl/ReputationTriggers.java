package net.luckyowlstudios.races.impl;

import net.luckyowlstudios.races.api.Race;
import net.luckyowlstudios.races.api.Races;
import net.luckyowlstudios.races.api.Reputation;
import net.luckyowlstudios.races.api.ReputationCause;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * The built-in actions that change reputation. Called from the mixins.
 * The amounts come from each race's {@code changes} block.
 */
public final class ReputationTriggers {

    private ReputationTriggers() {
    }

    /** A living entity died. If a player killed it, lower their reputation with its race. */
    public static void onDeath(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide) {
            return;
        }
        ServerPlayer killer = null;
        if (source.getEntity() instanceof ServerPlayer player) {
            killer = player;
        } else if (victim.getKillCredit() instanceof ServerPlayer player) {
            // e.g. knocked into lava or off a cliff by a player
            killer = player;
        }
        if (killer == null) {
            return;
        }
        ServerPlayer finalKiller = killer;
        withRace(victim).ifPresent(race -> Reputation.add(finalKiller, race.key(),
                race.value().changes().killedMember(), ReputationCause.KILLED_MEMBER));
    }

    /** A player finished a trade with a merchant (villagers, wandering traders, or any modded merchant). */
    public static void onTrade(Entity merchant, ServerPlayer player) {
        withRace(merchant).ifPresent(race -> Reputation.add(player, race.key(),
                race.value().changes().traded(), ReputationCause.TRADED));
    }

    /** A piglin bartered gold that this player threw to it. */
    public static void onBarter(Entity piglin, ServerPlayer player) {
        withRace(piglin).ifPresent(race -> Reputation.add(player, race.key(),
                race.value().changes().traded(), ReputationCause.BARTERED));
    }

    /** A player was a hero of the village when a raid was defeated. Applies every race's {@code raid_victory}. */
    public static void onRaidVictory(ServerPlayer player) {
        Races.all(player.server.registryAccess()).forEach(race -> Reputation.add(player, race.key(),
                race.value().changes().raidVictory(), ReputationCause.RAID_VICTORY));
    }

    private static Optional<Holder.Reference<Race>> withRace(Entity entity) {
        return Races.getRace(entity);
    }
}
