package net.luckyowlstudios.sentience.trading;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;

/**
 * Added to every {@link net.minecraft.world.entity.Mob} by mixin: the trade state of a mob that trades
 * through a {@link net.luckyowlstudios.sentience.api.TradeTable}. Saved with the mob.
 */
public interface TradingMob {

    @Nullable
    MerchantOffers sentience$getOffers();

    void sentience$setOffers(@Nullable MerchantOffers offers);

    long sentience$getLastRestock();

    void sentience$setLastRestock(long gameTime);

    @Nullable
    Player sentience$getTradingPlayer();

    void sentience$setTradingPlayer(@Nullable Player player);
}
