package fr.luuuccasss.schizocraft.client;

import fr.luuuccasss.schizocraft.Schizocraft;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

/** L'Ombre : une silhouette noire, trop grande et trop maigre, aux yeux blancs, qui n'existe que pour toi. */
public final class ShadowRenderer {
    private static final Identifier TEXTURE = Identifier.of(Schizocraft.MOD_ID, "textures/entity/ombre.png");
    private static final Identifier EYES = Identifier.of(Schizocraft.MOD_ID, "textures/entity/ombre_yeux.png");

    private static final Int2ObjectMap<Vec3d> TARGETS = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<Vec3d> SHOWN = new Int2ObjectOpenHashMap<>();
    private static ModelPart model;

    private ShadowRenderer() {
    }

    public static void update(int id, double x, double y, double z, boolean visible) {
        if (visible) TARGETS.put(id, new Vec3d(x, y, z));
        else {
            TARGETS.remove(id);
            SHOWN.remove(id);
        }
    }

    public static void clear() {
        TARGETS.clear();
        SHOWN.clear();
    }

    private static ModelPart model() {
        if (model == null) {
            ModelData data = new ModelData();
            ModelPartData root = data.getRoot();
            // 2,6 blocs de haut : jambes 22, corps 14, tête 8 (en pixels, 16 par bloc)
            root.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4, -44, -4, 8, 8, 8), ModelTransform.NONE);
            root.addChild("body", ModelPartBuilder.create().uv(32, 0).cuboid(-3.5f, -36, -2, 7, 14, 4), ModelTransform.NONE);
            root.addChild("right_arm", ModelPartBuilder.create().uv(0, 16).cuboid(-1, 0, -1, 2, 24, 2), ModelTransform.origin(-4.5f, -35, 0));
            root.addChild("left_arm", ModelPartBuilder.create().uv(8, 16).cuboid(-1, 0, -1, 2, 24, 2), ModelTransform.origin(4.5f, -35, 0));
            root.addChild("right_leg", ModelPartBuilder.create().uv(16, 16).cuboid(-1, 0, -1, 2, 22, 2), ModelTransform.origin(-1.8f, -22, 0));
            root.addChild("left_leg", ModelPartBuilder.create().uv(24, 16).cuboid(-1, 0, -1, 2, 22, 2), ModelTransform.origin(1.8f, -22, 0));
            model = TexturedModelData.of(data, 64, 64).createModel();
        }
        return model;
    }

    public static void render(WorldRenderContext context) {
        if (TARGETS.isEmpty() || context.consumers() == null || context.matrixStack() == null) return;
        Vec3d camera = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        ModelPart root = model();
        float time = (context.world().getTime() + context.tickCounter().getTickProgress(true)) / 20f;
        for (Int2ObjectMap.Entry<Vec3d> entry : TARGETS.int2ObjectEntrySet()) {
            // glisse doucement vers sa nouvelle position
            Vec3d target = entry.getValue();
            Vec3d pos = SHOWN.getOrDefault(entry.getIntKey(), target).lerp(target, 0.15);
            SHOWN.put(entry.getIntKey(), pos);
            double dx = camera.x - pos.x;
            double dz = camera.z - pos.z;
            float yaw = (float) MathHelper.atan2(dx, dz);
            root.getChild("right_arm").roll = 0.08f + 0.04f * MathHelper.sin(time * 1.3f);
            root.getChild("left_arm").roll = -0.08f - 0.04f * MathHelper.sin(time * 1.1f);
            root.getChild("head").roll = 0.12f * MathHelper.sin(time * 0.7f);

            matrices.push();
            matrices.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(yaw));
            matrices.scale(-1, -1, 1);
            // lumière à zéro : elle reste noire même en plein jour
            root.render(matrices, context.consumers().getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)),
                0, OverlayTexture.DEFAULT_UV);
            root.render(matrices, context.consumers().getBuffer(RenderLayer.getEyes(EYES)), 0xF000F0, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }
    }
}
