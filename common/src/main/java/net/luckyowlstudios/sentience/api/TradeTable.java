package net.luckyowlstudios.sentience.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.luckyowlstudios.sentience.Constants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.List;
import java.util.Optional;

/**
 * The trades a kind of mob offers once a player's reputation with its race reaches the race's
 * {@code trades_at}. One file per table at {@code data/<namespace>/sentience/trades/<name>.json}:
 * <pre>{@code
 * {
 *   "entities": "minecraft:piglin",
 *   "offers_per_mob": 4,
 *   "trade_sound": "minecraft:entity.piglin.admiring_item",
 *   "trades": [
 *     { "wants": { "id": "minecraft:gold_ingot", "count": 4 }, "gives": { "id": "minecraft:crying_obsidian", "count": 2 }, "max_uses": 8 }
 *   ]
 * }
 * }</pre>
 * Each mob picks {@code offers_per_mob} trades at random the first time someone trades with it, and keeps them.
 * Trades restock once a day ({@code restock_ticks}).
 *
 * @param entities     the mobs that use this table
 * @param offersPerMob how many trades each mob gets; empty means all of them
 * @param restockTicks how long until used-up trades are available again
 * @param tradeSound   played when a trade is made
 * @param trades       the possible trades
 */
public record TradeTable(HolderSet<EntityType<?>> entities, Optional<Integer> offersPerMob, int restockTicks,
                         Optional<Holder<SoundEvent>> tradeSound, List<Trade> trades) {

    /** Datapack registry of trade tables. Server side only. */
    public static final ResourceKey<Registry<TradeTable>> REGISTRY = ResourceKey.createRegistryKey(Constants.id("trades"));

    public static final Codec<TradeTable> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf("entities").forGetter(TradeTable::entities),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("offers_per_mob").forGetter(TradeTable::offersPerMob),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("restock_ticks", 24000).forGetter(TradeTable::restockTicks),
            SoundEvent.CODEC.optionalFieldOf("trade_sound").forGetter(TradeTable::tradeSound),
            Trade.CODEC.listOf().fieldOf("trades").forGetter(TradeTable::trades)
    ).apply(instance, TradeTable::new));

    /**
     * One trade.
     *
     * @param wants           what the player pays
     * @param wantsAlso       an optional second payment
     * @param gives           what the player gets
     * @param maxUses         how many times it can be used before restocking
     * @param xp              experience given to the player
     * @param priceMultiplier same as villagers; kept for mods that read it
     */
    public record Trade(ItemCost wants, Optional<ItemCost> wantsAlso, ItemStack gives, int maxUses, int xp, float priceMultiplier) {

        public static final Codec<Trade> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemCost.CODEC.fieldOf("wants").forGetter(Trade::wants),
                ItemCost.CODEC.optionalFieldOf("wants_also").forGetter(Trade::wantsAlso),
                ItemStack.CODEC.fieldOf("gives").forGetter(Trade::gives),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("max_uses", 8).forGetter(Trade::maxUses),
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("xp", 1).forGetter(Trade::xp),
                Codec.FLOAT.optionalFieldOf("price_multiplier", 0.05F).forGetter(Trade::priceMultiplier)
        ).apply(instance, Trade::new));

        public MerchantOffer toOffer() {
            return new MerchantOffer(this.wants, this.wantsAlso, this.gives.copy(), 0, this.maxUses, this.xp, this.priceMultiplier);
        }
    }
}
