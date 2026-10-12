package fr.luuuccasss.schizocraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.luuuccasss.schizocraft.Schizocraft;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** L'Ombre : une silhouette noire, trop grande et trop maigre, aux yeux blancs, qui n'existe que pour toi. */
public final class ShadowRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Schizocraft.MOD_ID, "textures/entity/ombre.png");
    private static final Identifier EYES = Identifier.fromNamespaceAndPath(Schizocraft.MOD_ID, "textures/entity/ombre_yeux.png");

    private static final Int2ObjectMap<Vec3> TARGETS = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<Vec3> SHOWN = new Int2ObjectOpenHashMap<>();
    private static ModelPart model;

    private ShadowRenderer() {
    }

    public static void update(int id, double x, double y, double z, boolean visible) {
        if (visible) TARGETS.put(id, new Vec3(x, y, z));
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
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            // 2,6 blocs de haut : jambes 22, corps 14, tête 8 (en pixels, 16 par bloc)
            root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -44, -4, 8, 8, 8), PartPose.ZERO);
            root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-3.5f, -36, -2, 7, 14, 4), PartPose.ZERO);
            root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 16).addBox(-1, 0, -1, 2, 24, 2), PartPose.offset(-4.5f, -35, 0));
            root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(8, 16).addBox(-1, 0, -1, 2, 24, 2), PartPose.offset(4.5f, -35, 0));
            root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 16).addBox(-1, 0, -1, 2, 22, 2), PartPose.offset(-1.8f, -22, 0));
            root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 16).addBox(-1, 0, -1, 2, 22, 2), PartPose.offset(1.8f, -22, 0));
            model = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        }
        return model;
    }

    public static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (TARGETS.isEmpty() || client.level == null) return;
        Vec3 camera = context.levelState().cameraRenderState.pos;
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();
        ModelPart root = model();
        float time = (client.level.getGameTime() + client.getDeltaTracker().getGameTimeDeltaPartialTick(true)) / 20f;
        for (Int2ObjectMap.Entry<Vec3> entry : TARGETS.int2ObjectEntrySet()) {
            // glisse doucement vers sa nouvelle position
            Vec3 target = entry.getValue();
            Vec3 pos = SHOWN.getOrDefault(entry.getIntKey(), target).lerp(target, 0.15);
            SHOWN.put(entry.getIntKey(), pos);
            double dx = camera.x - pos.x;
            double dz = camera.z - pos.z;
            float yaw = (float) Mth.atan2(dx, dz);
            root.getChild("right_arm").zRot = 0.08f + 0.04f * Mth.sin(time * 1.3f);
            root.getChild("left_arm").zRot = -0.08f - 0.04f * Mth.sin(time * 1.1f);
            root.getChild("head").zRot = 0.12f * Mth.sin(time * 0.7f);

            matrices.pushPose();
            matrices.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
            matrices.mulPose(Axis.YP.rotation(yaw));
            matrices.scale(-1, -1, 1);
            // lumière à zéro : elle reste noire même en plein jour
            collector.submitModelPart(root, matrices, RenderTypes.entityCutout(TEXTURE), 0, OverlayTexture.NO_OVERLAY, null);
            collector.submitModelPart(root, matrices, RenderTypes.eyes(EYES), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
            matrices.popPose();
        }
    }
}
