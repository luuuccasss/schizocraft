package fr.luuuccasss.schizocraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Pour les clients qui ont le mod : une ombre apparaît (ou bouge) ici, ou disparaît. */
public record ShadowPayload(int id, double x, double y, double z, boolean visible) implements CustomPayload {
    public static final CustomPayload.Id<ShadowPayload> ID = new CustomPayload.Id<>(Identifier.of(Schizocraft.MOD_ID, "shadow"));
    public static final PacketCodec<RegistryByteBuf, ShadowPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT, ShadowPayload::id,
        PacketCodecs.DOUBLE, ShadowPayload::x,
        PacketCodecs.DOUBLE, ShadowPayload::y,
        PacketCodecs.DOUBLE, ShadowPayload::z,
        PacketCodecs.BOOL, ShadowPayload::visible,
        ShadowPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
