package net.luckyowlstudios.sentience.impl;

import net.luckyowlstudios.sentience.api.Race;
import net.luckyowlstudios.sentience.api.Races;
import net.luckyowlstudios.sentience.api.Reputation;
import net.luckyowlstudios.sentience.api.ReputationCause;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The built-in actions that change reputation. Called from the mixins.
 * The amounts come from each race's {@code changes} block.
 */
public final class ReputationTriggers {

    private ReputationTriggers() {
    }

    /** The player responsible for this damage: the attacker, their arrow, or whoever gets kill credit. */
    @Nullable
    public static ServerPlayer responsiblePlayer(LivingEntity victim, DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        // e.g. knocked into lava or off a cliff by a player
        return victim.getKillCredit() instanceof ServerPlayer player ? player : null;
    }

    /** A living entity died. If a player killed it, lower their reputation with its race and any race protecting it. */
    public static void onDeath(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide) {
            return;
        }
        ServerPlayer killer = responsiblePlayer(victim, source);
        if (killer == null) {
            return;
        }
        withRace(victim).ifPresent(race -> Reputation.add(killer, race.key(),
                race.value().changes().killedMember(), ReputationCause.KILLED_MEMBER));
        onProtectedHarmed(victim, killer, true);
    }

    /** A living entity took damage and survived. */
    public static void onHurt(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide || victim.isDeadOrDying()) {
            return; // deaths are handled by onDeath
        }
        ServerPlayer attacker = source.getEntity() instanceof ServerPlayer player ? player : null;
        if (attacker != null) {
            onProtectedHarmed(victim, attacker, false);
        }
    }

    /** Races that protect this creature, and have a member close enough to see, hold it against the player. */
    private static void onProtectedHarmed(LivingEntity victim, ServerPlayer player, boolean killed) {
        if (victim instanceof TamableAnimal pet && pet.isTame()) {
            return; // someone's pet, not a village animal
        }
        Races.all(victim.level().registryAccess()).forEach(race -> race.value().protects().ifPresent(protection -> {
            int amount = killed ? protection.killed() : protection.hurt();
            if (amount == 0 || !protection.covers(victim.getType())) {
                return;
            }
            AABB area = victim.getBoundingBox().inflate(protection.witnessRadius());
            boolean witnessed = !victim.level().getEntitiesOfClass(LivingEntity.class, area,
                    witness -> witness != victim && witness.isAlive() && Races.isMember(witness, race.key())).isEmpty();
            if (witnessed) {
                Reputation.add(player, race.key(), amount,
                        killed ? ReputationCause.KILLED_PROTECTED : ReputationCause.HURT_PROTECTED);
            }
        }));
    }

    /** A player killed a raider that was part of an active raid. Applies every race's {@code killed_raider}. */
    public static void onRaiderKilled(ServerPlayer player) {
        Races.all(player.server.registryAccess()).forEach(race -> Reputation.add(player, race.key(),
                race.value().changes().killedRaider(), ReputationCause.KILLED_RAIDER));
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
