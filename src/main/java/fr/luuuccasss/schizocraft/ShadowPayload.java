package fr.luuuccasss.schizocraft;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Pour les clients qui ont le mod : une ombre apparaît (ou bouge) ici, ou disparaît. */
public record ShadowPayload(int id, double x, double y, double z, boolean visible) {
    public static final Identifier ID = new Identifier(Schizocraft.MOD_ID, "shadow");

    public void send(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(id);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeBoolean(visible);
        ServerPlayNetworking.send(player, ID, buf);
    }

    public static ShadowPayload read(PacketByteBuf buf) {
        return new ShadowPayload(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean());
    }
}
