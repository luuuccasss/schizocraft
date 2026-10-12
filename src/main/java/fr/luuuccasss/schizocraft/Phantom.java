package fr.luuuccasss.schizocraft;

import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;

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

    private final ServerPlayer player;
    private final Entity entity;
    private final boolean shadow;
    private final Vanish vanish;
    private final long expiresAt;
    /** Blocs par tick dont il se rapproche du joueur (0 : immobile). */
    private final double approach;
    private int lookedAt;
    private boolean gone;

    public Phantom(ServerPlayer player, Entity entity, boolean shadow, Vanish vanish, int lifetime, double approach) {
        this.player = player;
        this.entity = entity;
        this.shadow = shadow;
        this.vanish = vanish;
        this.expiresAt = player.level().getGameTime() + lifetime;
        this.approach = approach;
    }

    public void show() {
        faceThePlayer();
        if (shadow) {
            ServerPlayNetworking.send(player, new ShadowPayload(entity.getId(), entity.getX(), entity.getY(), entity.getZ(), true));
            return;
        }
        player.connection.send(new ClientboundAddEntityPacket(entity, 0, entity.blockPosition()));
        var data = entity.getEntityData().getNonDefaultValues();
        if (data != null) player.connection.send(new ClientboundSetEntityDataPacket(entity.getId(), data));
        sendRotation();
    }

    /** Renvoie faux quand il a disparu. */
    public boolean tick() {
        if (gone) return false;
        if (player.isRemoved() || player.level().getGameTime() > expiresAt || player.level() != entity.level()) {
            hide();
            return false;
        }
        double height = shadow ? 2.3 : entity.getBbHeight() * 0.7;
        Vec3 toPhantom = entity.position().add(0, height, 0).subtract(player.getEyePosition());
        double distance = toPhantom.length();
        if (distance < 2) {
            hide();
            return false;
        }
        double alignment = player.getViewVector(1f).dot(toPhantom.normalize());
        boolean seen = alignment > (vanish == Vanish.WHEN_SEEN ? 0.55 : 0.95);
        lookedAt = seen ? lookedAt + 1 : Math.max(0, lookedAt - 1);
        int patience = vanish == Vanish.WHEN_SEEN ? 3 : 25;
        if (lookedAt > patience || (vanish == Vanish.WHEN_STARED_AT && distance < 8)) {
            hide();
            return false;
        }
        if (approach > 0 && !seen && player.level().getGameTime() % 2 == 0) {
            Vec3 step = player.position().subtract(entity.position()).multiply(1, 0, 1).normalize().scale(approach * 2);
            entity.setPos(entity.position().add(step));
            faceThePlayer();
            if (shadow) {
                ServerPlayNetworking.send(player, new ShadowPayload(entity.getId(), entity.getX(), entity.getY(), entity.getZ(), true));
            } else {
                player.connection.send(ClientboundTeleportEntityPacket.teleport(entity.getId(), PositionMoveRotation.of(entity), Set.of(), false));
                sendRotation();
            }
        }
        return true;
    }

    public void hide() {
        if (gone) return;
        gone = true;
        if (shadow) ServerPlayNetworking.send(player, new ShadowPayload(entity.getId(), 0, 0, 0, false));
        else player.connection.send(new ClientboundRemoveEntitiesPacket(entity.getId()));
    }

    private void faceThePlayer() {
        Vec3 d = player.getEyePosition().subtract(entity.getEyePosition());
        float yaw = (float) (Mth.atan2(d.z, d.x) * 180 / Math.PI) - 90f;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI);
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
        entity.setXRot(pitch);
    }

    private void sendRotation() {
        player.connection.send(new ClientboundRotateHeadPacket(entity, (byte) Mth.floor(entity.getYHeadRot() * 256f / 360f)));
    }

    /** Pour les tests. */
    public Entity entity() {
        return entity;
    }
}
