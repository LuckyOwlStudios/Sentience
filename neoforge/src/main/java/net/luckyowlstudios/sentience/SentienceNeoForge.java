package net.luckyowlstudios.sentience;

import net.luckyowlstudios.sentience.api.Race;
import net.luckyowlstudios.sentience.api.Races;
import net.luckyowlstudios.sentience.api.TradeTable;
import net.luckyowlstudios.sentience.impl.ReputationCommand;
import net.luckyowlstudios.sentience.village.SentienceVillage;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@Mod(Constants.MOD_ID)
public class SentienceNeoForge {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Constants.MOD_ID);
    private static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, Constants.MOD_ID);

    private static final DeferredBlock<Block> TOWN_HALL = BLOCKS.register(SentienceVillage.TOWN_HALL_ID.getPath(), SentienceVillage::createTownHall);
    private static final DeferredItem<Item> TOWN_HALL_ITEM = ITEMS.register(SentienceVillage.TOWN_HALL_ID.getPath(),
            () -> SentienceVillage.createTownHallItem(TOWN_HALL.get()));
    // NeoForge fills in the block state to point of interest lookup for registered types.
    private static final DeferredHolder<PoiType, PoiType> TOWN_HALL_POI = POI_TYPES.register(SentienceVillage.TOWN_HALL_ID.getPath(),
            () -> SentienceVillage.createTownHallPoi(TOWN_HALL.get()));
    private static final DeferredHolder<VillagerProfession, VillagerProfession> GUIDE = PROFESSIONS.register(SentienceVillage.GUIDE_ID.getPath(),
            SentienceVillage::createGuide);

    public SentienceNeoForge(IEventBus modBus) {
        SentienceCommon.init();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        POI_TYPES.register(modBus);
        PROFESSIONS.register(modBus);
        SentienceVillage.townHall = TOWN_HALL;
        SentienceVillage.guide = GUIDE;
        modBus.addListener(SentienceNeoForge::addToCreativeTabs);
        modBus.addListener(SentienceNeoForge::registerDatapackRegistries);
        NeoForge.EVENT_BUS.addListener(SentienceNeoForge::registerCommands);
    }

    private static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(Races.REGISTRY, Race.CODEC, Race.CODEC);
        event.dataPackRegistry(TradeTable.REGISTRY, TradeTable.CODEC);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(TOWN_HALL_ITEM.get());
        }
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        ReputationCommand.register(event.getDispatcher());
    }
}
