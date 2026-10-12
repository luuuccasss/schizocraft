package fr.luuuccasss.schizocraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

/** Les hallucinations en cours d'un joueur : mobs fantômes, faux blocs et événements programmés. */
public final class Visions {
    private static final Map<UUID, Visions> BY_PLAYER = new HashMap<>();

    private final List<Phantom> phantoms = new ArrayList<>();
    private final Map<BlockPos, Long> fakeBlocks = new HashMap<>();
    private final List<Scheduled> scheduled = new ArrayList<>();
    private long nextHallucination = -1;
    private float lastSent = -1;

    private record Scheduled(long at, Runnable action) {
    }

    private Visions() {
    }

    public static Visions of(ServerPlayer player) {
        return BY_PLAYER.computeIfAbsent(player.getUUID(), id -> new Visions());
    }

    /** Appelé à chaque tick pour chaque joueur. */
    public static void tick(ServerLevel world, ServerPlayer player) {
        Visions visions = of(player);
        long now = world.getGameTime();
        visions.runScheduled(now);
        visions.phantoms.removeIf(phantom -> !phantom.tick());
        visions.revertBlocks(world, player, now);

        Stage stage = ((Paranoid) player).schizocraft$getStage();
        if (stage == Stage.LUCIDE || player.isSpectator() || player.isCreative() || !player.isAlive()) {
            visions.nextHallucination = -1;
            return;
        }
        if (visions.nextHallucination < 0) visions.planNext(world, stage);
        if (now >= visions.nextHallucination) {
            Hallucination.random(stage, world.getRandom()).play(world, player);
            visions.planNext(world, stage);
        }
    }

    private void planNext(ServerLevel world, Stage stage) {
        int seconds = stage.minDelay + world.getRandom().nextInt(stage.maxDelay - stage.minDelay + 1);
        nextHallucination = world.getGameTime() + seconds * 20L;
    }

    /** Envoie le niveau aux clients qui ont le mod, quand il a assez changé. */
    public static void sync(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, ParanoiaPayload.ID)) return;
        Visions visions = of(player);
        float paranoia = ((Paranoid) player).schizocraft$getParanoia();
        if (Math.abs(paranoia - visions.lastSent) < 0.5f) return;
        visions.lastSent = paranoia;
        ServerPlayNetworking.send(player, new ParanoiaPayload(paranoia));
    }

    public void later(ServerLevel world, int ticks, Runnable action) {
        scheduled.add(new Scheduled(world.getGameTime() + ticks, action));
    }

    private void runScheduled(long now) {
        if (scheduled.isEmpty()) return;
        List<Scheduled> due = new ArrayList<>();
        for (Iterator<Scheduled> it = scheduled.iterator(); it.hasNext(); ) {
            Scheduled s = it.next();
            if (s.at() <= now) {
                due.add(s);
                it.remove();
            }
        }
        due.forEach(s -> s.action().run());
    }

    public void add(Phantom phantom) {
        phantoms.add(phantom);
        phantom.show();
    }

    public int phantomCount() {
        return phantoms.size();
    }

    /** Montre un faux bloc à ce joueur seulement, pendant {@code ticks}. */
    public void fakeBlock(ServerLevel world, ServerPlayer player, BlockPos pos, BlockState state, int ticks) {
        fakeBlocks.put(pos.immutable(), world.getGameTime() + ticks);
        player.connection.send(new ClientboundBlockUpdatePacket(pos, state));
    }

    public boolean isFake(BlockPos pos) {
        return fakeBlocks.containsKey(pos);
    }

    private void revertBlocks(ServerLevel world, ServerPlayer player, long now) {
        if (fakeBlocks.isEmpty()) return;
        fakeBlocks.entrySet().removeIf(entry -> {
            if (entry.getValue() > now) return false;
            player.connection.send(new ClientboundBlockUpdatePacket(world, entry.getKey()));
            return true;
        });
    }

    /** Tout disparaît d'un coup (réveil, mort, commande). */
    public void clear(ServerLevel world, ServerPlayer player) {
        phantoms.forEach(Phantom::hide);
        phantoms.clear();
        for (BlockPos pos : fakeBlocks.keySet()) player.connection.send(new ClientboundBlockUpdatePacket(world, pos));
        fakeBlocks.clear();
        scheduled.clear();
    }

    public static void forget(ServerPlayer player) {
        BY_PLAYER.remove(player.getUUID());
    }
}
