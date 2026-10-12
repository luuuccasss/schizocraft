package fr.luuuccasss.schizocraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

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

    public static Visions of(ServerPlayerEntity player) {
        return BY_PLAYER.computeIfAbsent(player.getUuid(), id -> new Visions());
    }

    /** Appelé à chaque tick pour chaque joueur. */
    public static void tick(ServerWorld world, ServerPlayerEntity player) {
        Visions visions = of(player);
        long now = world.getTime();
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
            Hallucination.random(stage, world.random).play(world, player);
            visions.planNext(world, stage);
        }
    }

    private void planNext(ServerWorld world, Stage stage) {
        int seconds = stage.minDelay + world.random.nextInt(stage.maxDelay - stage.minDelay + 1);
        nextHallucination = world.getTime() + seconds * 20L;
    }

    /** Envoie le niveau aux clients qui ont le mod, quand il a assez changé. */
    public static void sync(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, ParanoiaPayload.ID)) return;
        Visions visions = of(player);
        float paranoia = ((Paranoid) player).schizocraft$getParanoia();
        if (Math.abs(paranoia - visions.lastSent) < 0.5f) return;
        visions.lastSent = paranoia;
        new ParanoiaPayload(paranoia).send(player);
    }

    public void later(ServerWorld world, int ticks, Runnable action) {
        scheduled.add(new Scheduled(world.getTime() + ticks, action));
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
    public void fakeBlock(ServerWorld world, ServerPlayerEntity player, BlockPos pos, BlockState state, int ticks) {
        fakeBlocks.put(pos.toImmutable(), world.getTime() + ticks);
        player.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, state));
    }

    public boolean isFake(BlockPos pos) {
        return fakeBlocks.containsKey(pos);
    }

    private void revertBlocks(ServerWorld world, ServerPlayerEntity player, long now) {
        if (fakeBlocks.isEmpty()) return;
        fakeBlocks.entrySet().removeIf(entry -> {
            if (entry.getValue() > now) return false;
            player.networkHandler.sendPacket(new BlockUpdateS2CPacket(world, entry.getKey()));
            return true;
        });
    }

    /** Tout disparaît d'un coup (réveil, mort, commande). */
    public void clear(ServerWorld world, ServerPlayerEntity player) {
        phantoms.forEach(Phantom::hide);
        phantoms.clear();
        for (BlockPos pos : fakeBlocks.keySet()) player.networkHandler.sendPacket(new BlockUpdateS2CPacket(world, pos));
        fakeBlocks.clear();
        scheduled.clear();
    }

    public static void forget(ServerPlayerEntity player) {
        BY_PLAYER.remove(player.getUuid());
    }
}
