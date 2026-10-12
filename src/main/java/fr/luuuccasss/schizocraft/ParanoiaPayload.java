package fr.luuuccasss.schizocraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Pour les clients qui ont le mod : le niveau de paranoïa du joueur, pour assombrir les bords de l'écran. */
public record ParanoiaPayload(float paranoia) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ParanoiaPayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Schizocraft.MOD_ID, "paranoia"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ParanoiaPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, ParanoiaPayload::paranoia, ParanoiaPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
