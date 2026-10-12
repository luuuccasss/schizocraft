package fr.luuuccasss.schizocraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Pour les clients qui ont le mod : une ombre apparaît (ou bouge) ici, ou disparaît. */
public record ShadowPayload(int id, double x, double y, double z, boolean visible) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShadowPayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Schizocraft.MOD_ID, "shadow"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShadowPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ShadowPayload::id,
        ByteBufCodecs.DOUBLE, ShadowPayload::x,
        ByteBufCodecs.DOUBLE, ShadowPayload::y,
        ByteBufCodecs.DOUBLE, ShadowPayload::z,
        ByteBufCodecs.BOOL, ShadowPayload::visible,
        ShadowPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
