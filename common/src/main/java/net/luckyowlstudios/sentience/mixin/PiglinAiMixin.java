package net.luckyowlstudios.sentience.mixin;

import net.luckyowlstudios.sentience.impl.BarterTracker;
import net.luckyowlstudios.sentience.impl.ReputationTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {

    @Shadow
    private static boolean isBarterCurrency(ItemStack stack) {
        throw new AssertionError();
    }

    /** Remember who threw the gold, before the item entity is used up. */
    @Inject(method = "pickUpItem", at = @At("HEAD"))
    private static void sentience$rememberThrower(Piglin piglin, ItemEntity item, CallbackInfo ci) {
        if (isBarterCurrency(item.getItem())) {
            UUID thrower = item.getOwner() instanceof ServerPlayer player ? player.getUUID() : null;
            ((BarterTracker) piglin).sentience$setBarterPlayer(thrower);
        }
    }

    /** Only reached when the piglin actually barters (adult, admired long enough, holding barter currency). */
    @Inject(method = "stopHoldingOffHandItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/piglin/PiglinAi;getBarterResponseItems(Lnet/minecraft/world/entity/monster/piglin/Piglin;)Ljava/util/List;"))
    private static void sentience$onBarter(Piglin piglin, boolean barter, CallbackInfo ci) {
        BarterTracker tracker = (BarterTracker) piglin;
        UUID thrower = tracker.sentience$getBarterPlayer();
        tracker.sentience$setBarterPlayer(null);
        if (thrower != null && piglin.level().getPlayerByUUID(thrower) instanceof ServerPlayer player) {
            ReputationTriggers.onBarter(piglin, player);
        }
    }
}
