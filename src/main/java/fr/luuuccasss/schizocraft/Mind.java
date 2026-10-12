package fr.luuuccasss.schizocraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

/** Ce qui fait monter ou descendre la paranoïa. Appelé chaque seconde. */
public final class Mind {
    /** Dans le noir (lumière ≤ 3) : 100 % en un peu moins de 7 minutes. */
    private static final float DARK = 0.25f;
    /** Personne à moins de 24 blocs (joueur, villageois, animal apprivoisé). */
    private static final float ALONE = 0.05f;
    /** Plus de trois jours sans dormir. */
    private static final float INSOMNIA = 0.1f;
    private static final int INSOMNIA_TICKS = 3 * 24000;
    /** Quelqu'un à moins de 8 blocs. */
    private static final float COMPANY = -0.2f;
    /** Dehors, en plein jour. */
    private static final float DAYLIGHT = -0.15f;
    /** Ailleurs, ça redescend tout doucement. */
    private static final float CALM = -0.02f;
    public static final float HURT = 3f;
    public static final float GOOD_NIGHT = -50f;
    public static final float NAP = -10f;

    private static final Map<UUID, Stage> LAST_STAGE = new HashMap<>();

    private Mind() {
    }

    public static void tick(ServerLevel world, ServerPlayer player) {
        if (player.isSpectator() || player.isCreative() || !player.isAlive()) return;
        Paranoid mind = (Paranoid) player;
        BlockPos eyes = BlockPos.containing(player.getEyePosition());
        int light = world.getMaxLocalRawBrightness(eyes);
        float delta = 0;
        if (light <= 3) delta += DARK;
        if (sleepless(player)) delta += INSOMNIA;
        if (hasCompany(world, player, 8)) delta += COMPANY;
        else if (!hasCompany(world, player, 24)) delta += ALONE;
        if (world.isBrightOutside() && world.canSeeSkyFromBelowWater(eyes) && light >= 12) delta += DAYLIGHT;
        if (delta == 0) delta = CALM;
        mind.schizocraft$setParanoia(mind.schizocraft$getParanoia() + delta);
        announce(player, mind.schizocraft$getStage());
    }

    public static boolean sleepless(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)) > INSOMNIA_TICKS;
    }

    private static boolean hasCompany(ServerLevel world, ServerPlayer player, double radius) {
        double r2 = radius * radius;
        for (ServerPlayer other : world.players()) {
            if (other != player && !other.isSpectator() && other.distanceToSqr(player) < r2) return true;
        }
        AABB box = player.getBoundingBox().inflate(radius);
        if (!world.getEntitiesOfClass(Villager.class, box, v -> v.isAlive()).isEmpty()) return true;
        return !world.getEntitiesOfClass(TamableAnimal.class, box, t -> t.isAlive() && t.isOwnedBy(player)).isEmpty();
    }

    /** Pas de jauge visible : seulement une impression, quand on change de stade. */
    private static void announce(ServerPlayer player, Stage stage) {
        Stage previous = LAST_STAGE.put(player.getUUID(), stage);
        if (previous == null || previous == stage) return;
        String message = null;
        if (stage.ordinal() > previous.ordinal()) {
            message = switch (stage) {
                case NERVEUX -> "Tu as l'impression d'être observé.";
                case PARANOIAQUE -> "Quelque chose ne va pas. Tu entends des choses.";
                case DELIRE -> "Tu ne sais plus ce qui est réel.";
                default -> null;
            };
        } else if (stage == Stage.LUCIDE) {
            message = "Tu te sens plus calme.";
        }
        if (message != null) {
            player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    public static void forget(ServerPlayer player) {
        LAST_STAGE.remove(player.getUUID());
    }
}
