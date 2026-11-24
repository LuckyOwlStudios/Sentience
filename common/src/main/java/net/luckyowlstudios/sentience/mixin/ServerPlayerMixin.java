package net.luckyowlstudios.sentience.mixin;

import net.luckyowlstudios.sentience.kingdom.KingdomMaps;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    /** Kingdom members fill in their kingdom's map as they walk around. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void sentience$exploreKingdomMap(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (self.tickCount % 20 == 0 && !self.isSpectator()) {
            KingdomMaps.explore(self);
        }
    }
}
