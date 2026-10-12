package fr.luuuccasss.schizocraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class Schizocraft implements ModInitializer {
    public static final String MOD_ID = "schizocraft";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(ShadowPayload.ID, ShadowPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ParanoiaPayload.ID, ParanoiaPayload.CODEC);
        ServerTickEvents.END_LEVEL_TICK.register(world -> {
            for (ServerPlayer player : world.players()) {
                if ((world.getGameTime() + player.getId()) % 20 == 0) {
                    Mind.tick(world, player);
                    Visions.sync(player);
                }
                Visions.tick(world, player);
            }
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
            if (entity instanceof ServerPlayer player && damage > 0) {
                Paranoid mind = (Paranoid) player;
                mind.schizocraft$setParanoia(mind.schizocraft$getParanoia() + Mind.HURT);
            }
        });
        // Une vraie nuit de sommeil fait le plus grand bien ; une sieste, un peu
        EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
            if (!(entity instanceof ServerPlayer player)) return;
            Paranoid mind = (Paranoid) player;
            boolean slept = player.getSleepTimer() >= 100 && player.level().isBrightOutside();
            mind.schizocraft$setParanoia(mind.schizocraft$getParanoia() + (slept ? Mind.GOOD_NIGHT : Mind.NAP));
            Visions.of(player).clear((ServerLevel) player.level(), player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Visions.forget(handler.getPlayer());
            Mind.forget(handler.getPlayer());
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ParanoiaCommand.register(dispatcher));
    }
}
