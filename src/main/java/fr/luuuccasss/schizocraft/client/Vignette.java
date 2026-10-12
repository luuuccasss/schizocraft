package fr.luuuccasss.schizocraft.client;

import fr.luuuccasss.schizocraft.Schizocraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Les bords de l'écran s'assombrissent avec la paranoïa, et battent comme un cœur en délire. */
public final class Vignette {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Schizocraft.MOD_ID, "vignette");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Schizocraft.MOD_ID, "textures/gui/vignette.png");
    private static float paranoia;
    private static float shown;

    private Vignette() {
    }

    public static void set(float value) {
        paranoia = value;
    }

    public static void clear() {
        paranoia = 0;
        shown = 0;
    }

    public static void render(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        shown += (paranoia - shown) * 0.05f;
        if (shown < 25) return;
        float strength = (shown - 25) / 75f;
        float alpha = 0.35f + 0.65f * strength;
        if (shown >= 75) {
            float t = (client.player.tickCount + tickCounter.getGameTimeDeltaPartialTick(true)) / 20f;
            float beat = Math.max(0, Mth.sin(t * 7.5f)) * Math.max(0, Mth.sin(t * 3.75f));
            alpha = Math.min(1f, alpha + 0.25f * beat);
        }
        int w = context.guiWidth();
        int h = context.guiHeight();
        int color = ((int) (alpha * 255) << 24) | 0xFFFFFF;
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0, 0, w, h, w, h, color);
    }
}
