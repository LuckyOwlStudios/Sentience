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
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.luckyowlstudios.sentience.kingdom.KingdomCommand;
import net.luckyowlstudios.sentience.kingdom.Kingdoms;
import net.luckyowlstudios.sentience.network.CreateKingdomPayload;
import net.luckyowlstudios.sentience.network.KingdomActionPayload;
import net.luckyowlstudios.sentience.network.KingdomViewPayload;
import net.luckyowlstudios.sentience.kingdom.KingdomScreens;
import net.luckyowlstudios.sentience.network.OpenCreateKingdomPayload;
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
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> {
            ReputationCommand.register(dispatcher);
            KingdomCommand.register(dispatcher);
        });
        registerVillage();
        registerNetworking();
    }

    private static void registerNetworking() {
        PayloadTypeRegistry.playS2C().register(OpenCreateKingdomPayload.TYPE, OpenCreateKingdomPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(CreateKingdomPayload.TYPE, CreateKingdomPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(KingdomViewPayload.TYPE, KingdomViewPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(KingdomActionPayload.TYPE, KingdomActionPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(KingdomActionPayload.TYPE,
                (payload, context) -> KingdomScreens.handle(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(CreateKingdomPayload.TYPE,
                (payload, context) -> Kingdoms.tryCreate(context.player(), payload));
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
