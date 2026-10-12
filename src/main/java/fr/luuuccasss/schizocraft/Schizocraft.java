package fr.luuuccasss.schizocraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class Schizocraft implements ModInitializer {
    public static final String MOD_ID = "schizocraft";

    @Override
    public void onInitialize() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if ((world.getTime() + player.getId()) % 20 == 0) {
                    Mind.tick(world, player);
                    Visions.sync(player);
                }
                Visions.tick(world, player);
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player && amount > 0) {
                Paranoid mind = (Paranoid) player;
                mind.schizocraft$setParanoia(mind.schizocraft$getParanoia() + Mind.HURT);
            }
            return true;
        });
        // Une vraie nuit de sommeil fait le plus grand bien ; une sieste, un peu
        EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            Paranoid mind = (Paranoid) player;
            boolean slept = player.getSleepTimer() >= 100 && player.getWorld().isDay();
            mind.schizocraft$setParanoia(mind.schizocraft$getParanoia() + (slept ? Mind.GOOD_NIGHT : Mind.NAP));
            Visions.of(player).clear((ServerWorld) player.getWorld(), player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Visions.forget(handler.getPlayer());
            Mind.forget(handler.getPlayer());
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ParanoiaCommand.register(dispatcher));
    }
}
