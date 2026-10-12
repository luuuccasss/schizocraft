package fr.luuuccasss.schizocraft.client;

import fr.luuuccasss.schizocraft.ParanoiaPayload;
import fr.luuuccasss.schizocraft.ShadowPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

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
        LevelRenderEvents.COLLECT_SUBMITS.register(ShadowRenderer::render);
        HudElementRegistry.addLast(Vignette.ID, Vignette::render);
    }
}
