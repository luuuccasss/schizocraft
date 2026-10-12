package fr.luuuccasss.schizocraft.client;

import fr.luuuccasss.schizocraft.ParanoiaPayload;
import fr.luuuccasss.schizocraft.ShadowPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public class SchizocraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ShadowPayload.ID,
            (payload, context) -> ShadowRenderer.update(payload.id(), payload.x(), payload.y(), payload.z(), payload.visible()));
        ClientPlayNetworking.registerGlobalReceiver(ParanoiaPayload.ID, (payload, context) -> Vignette.set(payload.paranoia()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ShadowRenderer.clear();
            Vignette.clear();
        });
        WorldRenderEvents.AFTER_ENTITIES.register(ShadowRenderer::render);
        HudRenderCallback.EVENT.register(Vignette::render);
    }
}
