package net.luckyowlstudios.sentience.village;

import com.google.common.collect.ImmutableSet;
import net.luckyowlstudios.sentience.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

/**
 * The Town Hall block and the Guide, the villager profession that works at it.
 * <p>
 * Each loader registers these (Fabric directly, NeoForge through deferred registers) and fills in the
 * suppliers below. The Town Hall is a job site point of interest with one ticket, so one villager can claim
 * each Town Hall, and an unemployed villager that claims it becomes the Guide.
 */
public final class SentienceVillage {

    public static final ResourceLocation TOWN_HALL_ID = Constants.id("town_hall");
    public static final ResourceLocation GUIDE_ID = Constants.id("guide");

    public static final ResourceKey<PoiType> TOWN_HALL_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, TOWN_HALL_ID);

    /** Set by the loader once registered. */
    public static Supplier<Block> townHall = () -> {
        throw new IllegalStateException("Town Hall not registered yet");
    };
    /** Set by the loader once registered. */
    public static Supplier<VillagerProfession> guide = () -> {
        throw new IllegalStateException("Guide not registered yet");
    };

    /** Like vanilla job sites: one villager per block, and it has to stand next to it. */
    public static final int TOWN_HALL_TICKETS = 1;
    public static final int TOWN_HALL_RANGE = 1;

    private SentienceVillage() {
    }

    public static Block createTownHall() {
        return new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.5F)
                .sound(SoundType.WOOD)
                .ignitedByLava());
    }

    public static Item createTownHallItem(Block block) {
        return new BlockItem(block, new Item.Properties());
    }

    public static PoiType createTownHallPoi(Block block) {
        return new PoiType(ImmutableSet.copyOf(block.getStateDefinition().getPossibleStates()), TOWN_HALL_TICKETS, TOWN_HALL_RANGE);
    }

    public static VillagerProfession createGuide() {
        return new VillagerProfession(GUIDE_ID.getPath(),
                poi -> poi.is(TOWN_HALL_POI),
                poi -> poi.is(TOWN_HALL_POI),
                ImmutableSet.of(), ImmutableSet.of(),
                SoundEvents.VILLAGER_WORK_CARTOGRAPHER);
    }
}
