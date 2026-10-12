package fr.luuuccasss.schizocraft;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Pour les clients qui ont le mod : le niveau de paranoïa du joueur, pour assombrir les bords de l'écran. */
public record ParanoiaPayload(float paranoia) {
    public static final Identifier ID = new Identifier(Schizocraft.MOD_ID, "paranoia");

    public void send(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(paranoia);
        ServerPlayNetworking.send(player, ID, buf);
    }

    public static ParanoiaPayload read(PacketByteBuf buf) {
        return new ParanoiaPayload(buf.readFloat());
    }
}
