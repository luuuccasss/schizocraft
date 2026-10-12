package fr.luuuccasss.schizocraft;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Un mob qui n'existe que pour un joueur : il n'est jamais ajouté au monde, seuls les paquets de son
 * apparition sont envoyés à ce joueur. Il disparaît quand on le regarde trop, quand on s'approche, ou au bout d'un moment.
 * Pour les joueurs qui ont le mod, une « ombre » en 3D est envoyée à la place d'un mob.
 */
public class Phantom {
    public enum Vanish {
        /** Disparaît dès qu'on se retourne dessus. */
        WHEN_SEEN,
        /** Se laisse voir un instant, puis disparaît si on le fixe ou qu'on approche. */
        WHEN_STARED_AT
    }

    private final ServerPlayerEntity player;
    private final Entity entity;
    private final boolean shadow;
    private final Vanish vanish;
    private final long expiresAt;
    /** Blocs par tick dont il se rapproche du joueur (0 : immobile). */
    private final double approach;
    private int lookedAt;
    private boolean gone;

    public Phantom(ServerPlayerEntity player, Entity entity, boolean shadow, Vanish vanish, int lifetime, double approach) {
        this.player = player;
        this.entity = entity;
        this.shadow = shadow;
        this.vanish = vanish;
        this.expiresAt = player.getServerWorld().getTime() + lifetime;
        this.approach = approach;
    }

    public void show() {
        faceThePlayer();
        if (shadow) {
            ServerPlayNetworking.send(player, new ShadowPayload(entity.getId(), entity.getX(), entity.getY(), entity.getZ(), true));
            return;
        }
        player.networkHandler.sendPacket(new EntitySpawnS2CPacket(entity, 0, entity.getBlockPos()));
        var data = entity.getDataTracker().getChangedEntries();
        if (data != null) player.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(entity.getId(), data));
        sendRotation();
    }

    /** Renvoie faux quand il a disparu. */
    public boolean tick() {
        if (gone) return false;
        if (player.isRemoved() || player.getServerWorld().getTime() > expiresAt || player.getWorld() != entity.getWorld()) {
            hide();
            return false;
        }
        double height = shadow ? 2.3 : entity.getHeight() * 0.7;
        Vec3d toPhantom = entity.getPos().add(0, height, 0).subtract(player.getEyePos());
        double distance = toPhantom.length();
        if (distance < 2) {
            hide();
            return false;
        }
        double alignment = player.getRotationVec(1f).dotProduct(toPhantom.normalize());
        boolean seen = alignment > (vanish == Vanish.WHEN_SEEN ? 0.55 : 0.95);
        lookedAt = seen ? lookedAt + 1 : Math.max(0, lookedAt - 1);
        int patience = vanish == Vanish.WHEN_SEEN ? 3 : 25;
        if (lookedAt > patience || (vanish == Vanish.WHEN_STARED_AT && distance < 8)) {
            hide();
            return false;
        }
        if (approach > 0 && !seen && player.getServerWorld().getTime() % 2 == 0) {
            Vec3d step = player.getPos().subtract(entity.getPos()).multiply(1, 0, 1).normalize().multiply(approach * 2);
            entity.setPosition(entity.getPos().add(step));
            faceThePlayer();
            if (shadow) {
                ServerPlayNetworking.send(player, new ShadowPayload(entity.getId(), entity.getX(), entity.getY(), entity.getZ(), true));
            } else {
                player.networkHandler.sendPacket(new EntityPositionS2CPacket(entity));
                sendRotation();
            }
        }
        return true;
    }

    public void hide() {
        if (gone) return;
        gone = true;
        if (shadow) ServerPlayNetworking.send(player, new ShadowPayload(entity.getId(), 0, 0, 0, false));
        else player.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(entity.getId()));
    }

    private void faceThePlayer() {
        Vec3d d = player.getEyePos().subtract(entity.getEyePos());
        float yaw = (float) (MathHelper.atan2(d.z, d.x) * 180 / Math.PI) - 90f;
        float pitch = (float) -(MathHelper.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI);
        entity.setYaw(yaw);
        entity.setHeadYaw(yaw);
        entity.setBodyYaw(yaw);
        entity.setPitch(pitch);
    }

    private void sendRotation() {
        player.networkHandler.sendPacket(new EntitySetHeadYawS2CPacket(entity, (byte) MathHelper.floor(entity.getHeadYaw() * 256f / 360f)));
    }

    /** Pour les tests. */
    public Entity entity() {
        return entity;
    }
}
