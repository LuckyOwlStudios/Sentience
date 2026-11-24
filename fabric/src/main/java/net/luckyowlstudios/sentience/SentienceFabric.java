package net.luckyowlstudios.sentience;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.luckyowlstudios.sentience.api.Race;
import net.luckyowlstudios.sentience.api.Races;
import net.luckyowlstudios.sentience.api.TradeTable;
import net.luckyowlstudios.sentience.impl.ReputationCommand;
import net.luckyowlstudios.sentience.village.SentienceVillage;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class SentienceFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SentienceCommon.init();
        DynamicRegistries.registerSynced(Races.REGISTRY, Race.CODEC);
        DynamicRegistries.register(TradeTable.REGISTRY, TradeTable.CODEC);
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> ReputationCommand.register(dispatcher));
        registerVillage();
    }

    private static void registerVillage() {
        Block townHall = Registry.register(BuiltInRegistries.BLOCK, SentienceVillage.TOWN_HALL_ID, SentienceVillage.createTownHall());
        Item townHallItem = Registry.register(BuiltInRegistries.ITEM, SentienceVillage.TOWN_HALL_ID, SentienceVillage.createTownHallItem(townHall));
        // Fabric's helper also tells the game which block states belong to the point of interest.
        PointOfInterestHelper.register(SentienceVillage.TOWN_HALL_ID, SentienceVillage.TOWN_HALL_TICKETS, SentienceVillage.TOWN_HALL_RANGE, townHall);
        VillagerProfession guide = Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, SentienceVillage.GUIDE_ID, SentienceVillage.createGuide());
        SentienceVillage.townHall = () -> townHall;
        SentienceVillage.guide = () -> guide;
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(townHallItem));
    }
}
