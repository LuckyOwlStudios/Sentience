package net.luckyowlstudios.races;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.luckyowlstudios.races.api.Race;
import net.luckyowlstudios.races.api.Races;
import net.luckyowlstudios.races.impl.ReputationCommand;

public class RacesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        RacesCommon.init();
        DynamicRegistries.registerSynced(Races.REGISTRY, Race.CODEC);
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> ReputationCommand.register(dispatcher));
    }
}
