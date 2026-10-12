package fr.luuuccasss.schizocraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Pour les clients qui ont le mod : le niveau de paranoïa du joueur, pour assombrir les bords de l'écran. */
public record ParanoiaPayload(float paranoia) implements CustomPayload {
    public static final CustomPayload.Id<ParanoiaPayload> ID = new CustomPayload.Id<>(Identifier.of(Schizocraft.MOD_ID, "paranoia"));
    public static final PacketCodec<RegistryByteBuf, ParanoiaPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.FLOAT, ParanoiaPayload::paranoia, ParanoiaPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
