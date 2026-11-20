package net.luckyowlstudios.sentience;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.luckyowlstudios.sentience.api.Race;
import net.luckyowlstudios.sentience.api.Races;
import net.luckyowlstudios.sentience.impl.ReputationCommand;

public class SentienceFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SentienceCommon.init();
        DynamicRegistries.registerSynced(Races.REGISTRY, Race.CODEC);
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> ReputationCommand.register(dispatcher));
    }
}
