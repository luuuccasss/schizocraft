package fr.luuuccasss.schizocraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.luuuccasss.schizocraft.Schizocraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Les bords de l'écran s'assombrissent avec la paranoïa, et battent comme un cœur en délire. */
public final class Vignette {
    private static final Identifier TEXTURE = Identifier.of(Schizocraft.MOD_ID, "textures/gui/vignette.png");
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

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        shown += (paranoia - shown) * 0.05f;
        if (shown < 25) return;
        float strength = (shown - 25) / 75f;
        float alpha = 0.35f + 0.65f * strength;
        if (shown >= 75) {
            float t = (client.player.age + tickCounter.getTickDelta(true)) / 20f;
            float beat = Math.max(0, MathHelper.sin(t * 7.5f)) * Math.max(0, MathHelper.sin(t * 3.75f));
            alpha = Math.min(1f, alpha + 0.25f * beat);
        }
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, alpha);
        context.drawTexture(TEXTURE, 0, 0, 0, 0, w, h, w, h);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
