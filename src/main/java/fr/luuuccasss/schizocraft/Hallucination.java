package fr.luuuccasss.schizocraft;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Tout ce que le joueur peut voir ou entendre qui n'existe pas. Tout est envoyé à lui seul :
 * les autres joueurs ne voient et n'entendent rien.
 */
public enum Hallucination {
    // --- Bruits (dès « nerveux ») ---
    CREEPER_HISS(Stage.NERVEUX, 6) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            sound(player, SoundEvents.ENTITY_CREEPER_PRIMED, behind(player, 2), 1f, 0.5f);
            return true;
        }
    },
    FOOTSTEPS(Stage.NERVEUX, 8) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            SoundEvent step = world.getBlockState(player.getBlockPos().down()).getSoundGroup().getStepSound();
            Vec3d back = horizontalLook(player).multiply(-1);
            Visions visions = Visions.of(player);
            for (int i = 0; i < 7; i++) {
                double distance = 10 - i * 1.2;
                int delay = i * 7;
                visions.later(world, delay, () -> sound(player, step, player.getEntityPos().add(back.multiply(distance)), 0.6f, 1f));
            }
            return true;
        }
    },
    DOOR(Stage.NERVEUX, 5) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            Vec3d pos = around(player, 7, world.random.nextFloat() * 360);
            sound(player, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, pos, 1f, 0.9f);
            Visions.of(player).later(world, 30 + world.random.nextInt(30),
                () -> sound(player, SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, pos, 1f, 0.9f));
            return true;
        }
    },
    CAVE_NOISE(Stage.NERVEUX, 6) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            sound(player, SoundEvents.AMBIENT_CAVE, around(player, 6, world.random.nextFloat() * 360), 1f, 1f);
            return true;
        }
    },
    GROAN(Stage.NERVEUX, 5) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            SoundEvent[] groans = {SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundEvents.ENTITY_SKELETON_AMBIENT,
                SoundEvents.ENTITY_SPIDER_AMBIENT, SoundEvents.ENTITY_ENDERMAN_AMBIENT};
            sound(player, groans[world.random.nextInt(groans.length)], behind(player, 5), 1f, 1f);
            return true;
        }
    },
    DIGGING(Stage.NERVEUX, 4) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            Vec3d pos = around(player, 5, 90 + world.random.nextInt(2) * 180);
            Visions visions = Visions.of(player);
            for (int i = 0; i < 6; i++) visions.later(world, i * 5, () -> sound(player, SoundEvents.BLOCK_STONE_HIT, pos, 0.8f, 0.8f));
            visions.later(world, 32, () -> sound(player, SoundEvents.BLOCK_STONE_BREAK, pos, 1f, 0.8f));
            return true;
        }
    },
    // --- Plus inquiétant (dès « paranoïaque ») ---
    HEARTBEAT(Stage.PARANOIAQUE, 3) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            Visions visions = Visions.of(player);
            for (int i = 0; i < 5; i++) visions.later(world, i * 16, () -> sound(player, SoundEvents.ENTITY_WARDEN_HEARTBEAT, player.getEyePos(), 1f, 1f));
            return true;
        }
    },
    ARROW(Stage.PARANOIAQUE, 4) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            sound(player, SoundEvents.ENTITY_SKELETON_SHOOT, around(player, 12, world.random.nextFloat() * 360), 1f, 1f);
            Visions.of(player).later(world, 6, () -> sound(player, SoundEvents.ENTITY_ARROW_HIT, around(player, 1.2, world.random.nextFloat() * 360), 1f, 1.1f));
            return true;
        }
    },
    TNT(Stage.PARANOIAQUE, 3) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            sound(player, SoundEvents.ENTITY_TNT_PRIMED, behind(player, 3), 1f, 1f);
            return true;
        }
    },
    FAKE_JOIN(Stage.PARANOIAQUE, 4) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            Text name = Text.literal(someone(world, player));
            player.sendMessage(Text.translatable("multiplayer.player.joined", name).formatted(Formatting.YELLOW), false);
            Visions.of(player).later(world, 60 + world.random.nextInt(200),
                () -> player.sendMessage(Text.translatable("multiplayer.player.left", name).formatted(Formatting.YELLOW), false));
            return true;
        }
    },
    WHISPER(Stage.PARANOIAQUE, 5) {
        private static final String[] LINES = {"je te vois", "derrière toi", "tu n'es pas seul ici", "pourquoi tu creuses ?",
            "ne te retourne pas", "je sais où est ta base", "c'est moi qui ai ouvert la porte", "regarde en haut"};

        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            Text text = Text.translatable("commands.message.display.incoming", Text.literal(someone(world, player)),
                Text.literal(LINES[world.random.nextInt(LINES.length)])).formatted(Formatting.GRAY, Formatting.ITALIC);
            player.sendMessage(text, false);
            return true;
        }
    },
    FAKE_DEATH(Stage.PARANOIAQUE, 3) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            String[] killers = {"entity.minecraft.zombie", "entity.minecraft.skeleton", "entity.minecraft.creeper", "entity.minecraft.spider"};
            Text text = Text.translatable("death.attack.mob", Text.literal(someone(world, player)),
                Text.translatable(killers[world.random.nextInt(killers.length)]));
            player.sendMessage(text, false);
            return true;
        }
    },
    FAKE_ADVANCEMENT(Stage.PARANOIAQUE, 2) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            Text title = Texts.bracketed(Text.translatable("advancements.story.mine_diamond.title")).formatted(Formatting.GREEN);
            player.sendMessage(Text.translatable("chat.type.advancement.task", player.getDisplayName(), title), false);
            return true;
        }
    },
    CREEPER_BEHIND(Stage.PARANOIAQUE, 5) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            BlockPos pos = ground(world, behind(player, 3.5));
            if (pos == null) return false;
            CreeperEntity creeper = EntityType.CREEPER.create(world, SpawnReason.MOB_SUMMONED);
            if (creeper == null) return false;
            creeper.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            creeper.setFuseSpeed(1);
            Visions.of(player).add(new Phantom(player, creeper, false, Phantom.Vanish.WHEN_SEEN, 80, 0));
            sound(player, SoundEvents.ENTITY_CREEPER_PRIMED, creeper.getEntityPos(), 1f, 0.5f);
            return true;
        }
    },
    WATCHER(Stage.PARANOIAQUE, 6) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            EntityType<?>[] types = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.ENDERMAN, EntityType.HUSK};
            float angle = (world.random.nextFloat() - 0.5f) * 120f;
            BlockPos pos = ground(world, onGround(player, 14 + world.random.nextInt(8), angle));
            if (pos == null) return false;
            Entity entity = types[world.random.nextInt(types.length)].create(world, SpawnReason.MOB_SUMMONED);
            if (entity == null) return false;
            entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            Visions.of(player).add(new Phantom(player, entity, false, Phantom.Vanish.WHEN_STARED_AT, 400, 0));
            return true;
        }
    },
    SHADOW(Stage.PARANOIAQUE, 5) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            boolean modded = ServerPlayNetworking.canSend(player, ShadowPayload.ID);
            float side = (world.random.nextBoolean() ? 1 : -1) * (45 + world.random.nextFloat() * 25);
            BlockPos pos = ground(world, onGround(player, 12 + world.random.nextInt(7), side));
            if (pos == null) return false;
            Entity holder = modded ? EntityType.MARKER.create(world, SpawnReason.MOB_SUMMONED) : EntityType.ENDERMAN.create(world, SpawnReason.MOB_SUMMONED);
            if (holder == null) return false;
            holder.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            Visions.of(player).add(new Phantom(player, holder, modded, Phantom.Vanish.WHEN_STARED_AT, 300, 0));
            return true;
        }
    },
    // --- Délire ---
    FOLLOWER(Stage.DELIRE, 5) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            boolean modded = ServerPlayNetworking.canSend(player, ShadowPayload.ID);
            BlockPos pos = ground(world, behind(player, 11));
            if (pos == null) return false;
            Entity entity = modded ? EntityType.MARKER.create(world, SpawnReason.MOB_SUMMONED) : EntityType.ZOMBIE.create(world, SpawnReason.MOB_SUMMONED);
            if (entity == null) return false;
            entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            Visions.of(player).add(new Phantom(player, entity, modded, Phantom.Vanish.WHEN_SEEN, 400, 0.06));
            return true;
        }
    },
    FAKE_DIAMONDS(Stage.DELIRE, 4) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            List<BlockPos> walls = new ArrayList<>();
            BlockPos center = player.getBlockPos();
            for (BlockPos pos : BlockPos.iterate(center.add(-5, -2, -5), center.add(5, 3, 5))) {
                BlockState state = world.getBlockState(pos);
                if (!state.isIn(BlockTags.BASE_STONE_OVERWORLD) || !exposed(world, pos)) continue;
                if (pos.getSquaredDistance(center) < 4) continue;
                walls.add(pos.toImmutable());
            }
            if (walls.isEmpty()) return false;
            BlockPos origin = walls.get(world.random.nextInt(walls.size()));
            Visions visions = Visions.of(player);
            int count = 2 + world.random.nextInt(3);
            for (BlockPos pos : walls) {
                if (count == 0) break;
                if (pos.getManhattanDistance(origin) > 1) continue;
                BlockState fake = world.getBlockState(pos).isOf(Blocks.DEEPSLATE) ? Blocks.DEEPSLATE_DIAMOND_ORE.getDefaultState()
                    : Blocks.DIAMOND_ORE.getDefaultState();
                visions.fakeBlock(world, player, pos, fake, 1200);
                count--;
            }
            return true;
        }
    },
    TORCHES_OUT(Stage.DELIRE, 4) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            Visions visions = Visions.of(player);
            int found = 0;
            BlockPos center = player.getBlockPos();
            for (BlockPos pos : BlockPos.iterate(center.add(-10, -4, -10), center.add(10, 5, 10))) {
                BlockState state = world.getBlockState(pos);
                if (!state.isOf(Blocks.TORCH) && !state.isOf(Blocks.WALL_TORCH) && !state.isOf(Blocks.LANTERN)) continue;
                visions.fakeBlock(world, player, pos, Blocks.AIR.getDefaultState(), 80 + world.random.nextInt(40));
                found++;
            }
            if (found == 0) return false;
            sound(player, SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, player.getEyePos(), 0.6f, 0.8f);
            return true;
        }
    },
    FAKE_HIT(Stage.DELIRE, 4) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            player.networkHandler.sendPacket(new EntityDamageS2CPacket(player, world.getDamageSources().generic()));
            sound(player, SoundEvents.ENTITY_PLAYER_HURT, player.getEyePos(), 1f, 1f);
            return true;
        }
    },
    BLACKOUT(Stage.DELIRE, 3) {
        @Override
        boolean run(ServerWorld world, ServerPlayerEntity player) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0, false, false));
            return true;
        }
    };

    private static final String[] NAMES = {"Herobrine", "Entity303", "Null", "Steve", "Alex", "xX_Ombre_Xx", "Personne"};

    public final Stage minStage;
    private final int weight;

    Hallucination(Stage minStage, int weight) {
        this.minStage = minStage;
        this.weight = weight;
    }

    /** Renvoie faux si elle n'a pas pu avoir lieu ici (pas de mur, pas de torche…). */
    abstract boolean run(ServerWorld world, ServerPlayerEntity player);

    public void play(ServerWorld world, ServerPlayerEntity player) {
        if (!run(world, player)) CAVE_NOISE.run(world, player);
    }

    /** Une hallucination au hasard, parmi celles permises à ce stade. */
    public static Hallucination random(Stage stage, Random random) {
        int total = 0;
        for (Hallucination h : values()) if (stage.atLeast(h.minStage)) total += h.weight;
        int n = random.nextInt(Math.max(1, total));
        for (Hallucination h : values()) {
            if (!stage.atLeast(h.minStage)) continue;
            n -= h.weight;
            if (n < 0) return h;
        }
        return CAVE_NOISE;
    }

    // --- outils ---

    static void sound(ServerPlayerEntity player, SoundEvent event, Vec3d pos, float volume, float pitch) {
        sound(player, Registries.SOUND_EVENT.getEntry(event), pos, volume, pitch);
    }

    static void sound(ServerPlayerEntity player, RegistryEntry<SoundEvent> event, Vec3d pos, float volume, float pitch) {
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(event, SoundCategory.HOSTILE, pos.x, pos.y, pos.z, volume, pitch,
            player.getRandom().nextLong()));
    }

    static Vec3d horizontalLook(ServerPlayerEntity player) {
        float yaw = player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(-MathHelper.sin(yaw), 0, MathHelper.cos(yaw));
    }

    static Vec3d behind(ServerPlayerEntity player, double distance) {
        return player.getEntityPos().add(horizontalLook(player).multiply(-distance)).add(0, 1, 0);
    }

    /** Point à {@code distance} blocs, à {@code angle} degrés de l'endroit où le joueur regarde, au niveau de sa tête. */
    static Vec3d around(ServerPlayerEntity player, double distance, float angle) {
        return onGround(player, distance, angle).add(0, 1, 0);
    }

    /** Pareil, au niveau de ses pieds. */
    static Vec3d onGround(ServerPlayerEntity player, double distance, float angle) {
        float yaw = (player.getYaw() + angle) * MathHelper.RADIANS_PER_DEGREE;
        return player.getEntityPos().add(-MathHelper.sin(yaw) * distance, 0, MathHelper.cos(yaw) * distance);
    }

    /** Un sol où un mob tiendrait debout, près de ce point. */
    static BlockPos ground(ServerWorld world, Vec3d near) {
        BlockPos base = BlockPos.ofFloored(near);
        for (int dy = 0; dy <= 6; dy++) {
            for (int sign : new int[]{1, -1}) {
                BlockPos pos = base.up(dy * sign);
                if (world.getBlockState(pos.down()).isSolidBlock(world, pos.down())
                    && world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
                    && world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty()
                    && world.getFluidState(pos).isEmpty()) {
                    return pos;
                }
            }
        }
        return null;
    }

    private static boolean exposed(ServerWorld world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (world.getBlockState(pos.offset(direction)).isAir()) return true;
        }
        return false;
    }

    /** Le nom d'un autre joueur connecté, ou d'un inconnu. */
    static String someone(ServerWorld world, ServerPlayerEntity player) {
        List<String> names = new ArrayList<>();
        for (ServerPlayerEntity other : world.getServer().getPlayerManager().getPlayerList()) {
            if (other != player) names.add(other.getName().getString());
        }
        if (names.isEmpty() || world.random.nextInt(3) == 0) return NAMES[world.random.nextInt(NAMES.length)];
        return names.get(world.random.nextInt(names.size()));
    }
}
