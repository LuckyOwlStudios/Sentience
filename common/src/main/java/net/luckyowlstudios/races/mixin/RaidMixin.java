package net.luckyowlstudios.races.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.luckyowlstudios.races.impl.ReputationTriggers;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Raid.class)
public abstract class RaidMixin {

    /**
     * When a raid is won, vanilla gives each hero of the village the "raid win" advancement trigger.
     * That call only happens on victory, once per hero, so it's the cleanest place to reward them.
     */
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/advancements/critereon/PlayerTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void races$onRaidVictory(PlayerTrigger trigger, ServerPlayer player, Operation<Void> original) {
        original.call(trigger, player);
        ReputationTriggers.onRaidVictory(player);
    }
}
