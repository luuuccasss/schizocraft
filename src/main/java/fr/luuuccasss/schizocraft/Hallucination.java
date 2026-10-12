package fr.luuuccasss.schizocraft;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Tout ce que le joueur peut voir ou entendre qui n'existe pas. Tout est envoyé à lui seul :
 * les autres joueurs ne voient et n'entendent rien.
 */
public enum Hallucination {
    // --- Bruits (dès « nerveux ») ---
    CREEPER_HISS(Stage.NERVEUX, 6) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            sound(player, SoundEvents.CREEPER_PRIMED, behind(player, 2), 1f, 0.5f);
            return true;
        }
    },
    FOOTSTEPS(Stage.NERVEUX, 8) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            SoundEvent step = world.getBlockState(player.blockPosition().below()).getSoundType().getStepSound();
            Vec3 back = horizontalLook(player).scale(-1);
            Visions visions = Visions.of(player);
            for (int i = 0; i < 7; i++) {
                double distance = 10 - i * 1.2;
                int delay = i * 7;
                visions.later(world, delay, () -> sound(player, step, player.position().add(back.scale(distance)), 0.6f, 1f));
            }
            return true;
        }
    },
    DOOR(Stage.NERVEUX, 5) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            Vec3 pos = around(player, 7, world.getRandom().nextFloat() * 360);
            sound(player, SoundEvents.WOODEN_DOOR_OPEN, pos, 1f, 0.9f);
            Visions.of(player).later(world, 30 + world.getRandom().nextInt(30),
                () -> sound(player, SoundEvents.WOODEN_DOOR_CLOSE, pos, 1f, 0.9f));
            return true;
        }
    },
    CAVE_NOISE(Stage.NERVEUX, 6) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            sound(player, SoundEvents.AMBIENT_CAVE, around(player, 6, world.getRandom().nextFloat() * 360), 1f, 1f);
            return true;
        }
    },
    GROAN(Stage.NERVEUX, 5) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            SoundEvent[] groans = {SoundEvents.ZOMBIE_AMBIENT, SoundEvents.SKELETON_AMBIENT,
                SoundEvents.SPIDER_AMBIENT, SoundEvents.ENDERMAN_AMBIENT};
            sound(player, groans[world.getRandom().nextInt(groans.length)], behind(player, 5), 1f, 1f);
            return true;
        }
    },
    DIGGING(Stage.NERVEUX, 4) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            Vec3 pos = around(player, 5, 90 + world.getRandom().nextInt(2) * 180);
            Visions visions = Visions.of(player);
            for (int i = 0; i < 6; i++) visions.later(world, i * 5, () -> sound(player, SoundEvents.STONE_HIT, pos, 0.8f, 0.8f));
            visions.later(world, 32, () -> sound(player, SoundEvents.STONE_BREAK, pos, 1f, 0.8f));
            return true;
        }
    },
    // --- Plus inquiétant (dès « paranoïaque ») ---
    HEARTBEAT(Stage.PARANOIAQUE, 3) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            Visions visions = Visions.of(player);
            for (int i = 0; i < 5; i++) visions.later(world, i * 16, () -> sound(player, SoundEvents.WARDEN_HEARTBEAT, player.getEyePosition(), 1f, 1f));
            return true;
        }
    },
    ARROW(Stage.PARANOIAQUE, 4) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            sound(player, SoundEvents.SKELETON_SHOOT, around(player, 12, world.getRandom().nextFloat() * 360), 1f, 1f);
            Visions.of(player).later(world, 6, () -> sound(player, SoundEvents.ARROW_HIT, around(player, 1.2, world.getRandom().nextFloat() * 360), 1f, 1.1f));
            return true;
        }
    },
    TNT(Stage.PARANOIAQUE, 3) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            sound(player, SoundEvents.TNT_PRIMED, behind(player, 3), 1f, 1f);
            return true;
        }
    },
    FAKE_JOIN(Stage.PARANOIAQUE, 4) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            Component name = Component.literal(someone(world, player));
            player.sendSystemMessage(Component.translatable("multiplayer.player.joined", name).withStyle(ChatFormatting.YELLOW), false);
            Visions.of(player).later(world, 60 + world.getRandom().nextInt(200),
                () -> player.sendSystemMessage(Component.translatable("multiplayer.player.left", name).withStyle(ChatFormatting.YELLOW), false));
            return true;
        }
    },
    WHISPER(Stage.PARANOIAQUE, 5) {
        private static final String[] LINES = {"je te vois", "derrière toi", "tu n'es pas seul ici", "pourquoi tu creuses ?",
            "ne te retourne pas", "je sais où est ta base", "c'est moi qui ai ouvert la porte", "regarde en haut"};

        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            Component text = Component.translatable("commands.message.display.incoming", Component.literal(someone(world, player)),
                Component.literal(LINES[world.getRandom().nextInt(LINES.length)])).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
            player.sendSystemMessage(text, false);
            return true;
        }
    },
    FAKE_DEATH(Stage.PARANOIAQUE, 3) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            String[] killers = {"entity.minecraft.zombie", "entity.minecraft.skeleton", "entity.minecraft.creeper", "entity.minecraft.spider"};
            Component text = Component.translatable("death.attack.mob", Component.literal(someone(world, player)),
                Component.translatable(killers[world.getRandom().nextInt(killers.length)]));
            player.sendSystemMessage(text, false);
            return true;
        }
    },
    FAKE_ADVANCEMENT(Stage.PARANOIAQUE, 2) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            Component title = ComponentUtils.wrapInSquareBrackets(Component.translatable("advancements.story.mine_diamond.title")).withStyle(ChatFormatting.GREEN);
            player.sendSystemMessage(Component.translatable("chat.type.advancement.task", player.getDisplayName(), title), false);
            return true;
        }
    },
    CREEPER_BEHIND(Stage.PARANOIAQUE, 5) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            BlockPos pos = ground(world, behind(player, 3.5));
            if (pos == null) return false;
            Creeper creeper = (Creeper) type("creeper").create(world, EntitySpawnReason.MOB_SUMMONED);
            if (creeper == null) return false;
            creeper.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            creeper.setSwellDir(1);
            Visions.of(player).add(new Phantom(player, creeper, false, Phantom.Vanish.WHEN_SEEN, 80, 0));
            sound(player, SoundEvents.CREEPER_PRIMED, creeper.position(), 1f, 0.5f);
            return true;
        }
    },
    WATCHER(Stage.PARANOIAQUE, 6) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            EntityType<?>[] types = {type("zombie"), type("skeleton"), type("enderman"), type("husk")};
            float angle = (world.getRandom().nextFloat() - 0.5f) * 120f;
            BlockPos pos = ground(world, onGround(player, 14 + world.getRandom().nextInt(8), angle));
            if (pos == null) return false;
            Entity entity = types[world.getRandom().nextInt(types.length)].create(world, EntitySpawnReason.MOB_SUMMONED);
            if (entity == null) return false;
            entity.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            Visions.of(player).add(new Phantom(player, entity, false, Phantom.Vanish.WHEN_STARED_AT, 400, 0));
            return true;
        }
    },
    SHADOW(Stage.PARANOIAQUE, 5) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            boolean modded = ServerPlayNetworking.canSend(player, ShadowPayload.ID);
            float side = (world.getRandom().nextBoolean() ? 1 : -1) * (45 + world.getRandom().nextFloat() * 25);
            BlockPos pos = ground(world, onGround(player, 12 + world.getRandom().nextInt(7), side));
            if (pos == null) return false;
            Entity holder = modded ? type("marker").create(world, EntitySpawnReason.MOB_SUMMONED) : type("enderman").create(world, EntitySpawnReason.MOB_SUMMONED);
            if (holder == null) return false;
            holder.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            Visions.of(player).add(new Phantom(player, holder, modded, Phantom.Vanish.WHEN_STARED_AT, 300, 0));
            return true;
        }
    },
    // --- Délire ---
    FOLLOWER(Stage.DELIRE, 5) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            boolean modded = ServerPlayNetworking.canSend(player, ShadowPayload.ID);
            BlockPos pos = ground(world, behind(player, 11));
            if (pos == null) return false;
            Entity entity = modded ? type("marker").create(world, EntitySpawnReason.MOB_SUMMONED) : type("zombie").create(world, EntitySpawnReason.MOB_SUMMONED);
            if (entity == null) return false;
            entity.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            Visions.of(player).add(new Phantom(player, entity, modded, Phantom.Vanish.WHEN_SEEN, 400, 0.06));
            return true;
        }
    },
    FAKE_DIAMONDS(Stage.DELIRE, 4) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            List<BlockPos> walls = new ArrayList<>();
            BlockPos center = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -2, -5), center.offset(5, 3, 5))) {
                BlockState state = world.getBlockState(pos);
                if (!state.is(BlockTags.BASE_STONE_OVERWORLD) || !exposed(world, pos)) continue;
                if (pos.distSqr(center) < 4) continue;
                walls.add(pos.immutable());
            }
            if (walls.isEmpty()) return false;
            BlockPos origin = walls.get(world.getRandom().nextInt(walls.size()));
            Visions visions = Visions.of(player);
            int count = 2 + world.getRandom().nextInt(3);
            for (BlockPos pos : walls) {
                if (count == 0) break;
                if (pos.distManhattan(origin) > 1) continue;
                BlockState fake = world.getBlockState(pos).is(Blocks.DEEPSLATE) ? Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState()
                    : Blocks.DIAMOND_ORE.defaultBlockState();
                visions.fakeBlock(world, player, pos, fake, 1200);
                count--;
            }
            return true;
        }
    },
    TORCHES_OUT(Stage.DELIRE, 4) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            Visions visions = Visions.of(player);
            int found = 0;
            BlockPos center = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-10, -4, -10), center.offset(10, 5, 10))) {
                BlockState state = world.getBlockState(pos);
                if (!state.is(Blocks.TORCH) && !state.is(Blocks.WALL_TORCH) && !state.is(Blocks.LANTERN)) continue;
                visions.fakeBlock(world, player, pos, Blocks.AIR.defaultBlockState(), 80 + world.getRandom().nextInt(40));
                found++;
            }
            if (found == 0) return false;
            sound(player, SoundEvents.GENERIC_EXTINGUISH_FIRE, player.getEyePosition(), 0.6f, 0.8f);
            return true;
        }
    },
    FAKE_HIT(Stage.DELIRE, 4) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            player.connection.send(new ClientboundDamageEventPacket(player, world.damageSources().generic()));
            sound(player, SoundEvents.PLAYER_HURT, player.getEyePosition(), 1f, 1f);
            return true;
        }
    },
    BLACKOUT(Stage.DELIRE, 3) {
        @Override
        boolean run(ServerLevel world, ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
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
    abstract boolean run(ServerLevel world, ServerPlayer player);

    public void play(ServerLevel world, ServerPlayer player) {
        if (!run(world, player)) CAVE_NOISE.run(world, player);
    }

    /** Une hallucination au hasard, parmi celles permises à ce stade. */
    public static Hallucination random(Stage stage, RandomSource random) {
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

    /** Par le registre : les constantes d'EntityType ont changé de classe entre 26.1 et 26.2. */
    static EntityType<?> type(String id) {
        return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id));
    }

    static void sound(ServerPlayer player, SoundEvent event, Vec3 pos, float volume, float pitch) {
        sound(player, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(event), pos, volume, pitch);
    }

    static void sound(ServerPlayer player, Holder<SoundEvent> event, Vec3 pos, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(event, SoundSource.HOSTILE, pos.x, pos.y, pos.z, volume, pitch,
            player.getRandom().nextLong()));
    }

    static Vec3 horizontalLook(ServerPlayer player) {
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    static Vec3 behind(ServerPlayer player, double distance) {
        return player.position().add(horizontalLook(player).scale(-distance)).add(0, 1, 0);
    }

    /** Point à {@code distance} blocs, à {@code angle} degrés de l'endroit où le joueur regarde, au niveau de sa tête. */
    static Vec3 around(ServerPlayer player, double distance, float angle) {
        return onGround(player, distance, angle).add(0, 1, 0);
    }

    /** Pareil, au niveau de ses pieds. */
    static Vec3 onGround(ServerPlayer player, double distance, float angle) {
        float yaw = (player.getYRot() + angle) * Mth.DEG_TO_RAD;
        return player.position().add(-Mth.sin(yaw) * distance, 0, Mth.cos(yaw) * distance);
    }

    /** Un sol où un mob tiendrait debout, près de ce point. */
    static BlockPos ground(ServerLevel world, Vec3 near) {
        BlockPos base = BlockPos.containing(near);
        for (int dy = 0; dy <= 6; dy++) {
            for (int sign : new int[]{1, -1}) {
                BlockPos pos = base.above(dy * sign);
                if (world.getBlockState(pos.below()).isFaceSturdy(world, pos.below(), Direction.UP)
                    && world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
                    && world.getBlockState(pos.above()).getCollisionShape(world, pos.above()).isEmpty()
                    && world.getFluidState(pos).isEmpty()) {
                    return pos;
                }
            }
        }
        return null;
    }

    private static boolean exposed(ServerLevel world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (world.getBlockState(pos.relative(direction)).isAir()) return true;
        }
        return false;
    }

    /** Le nom d'un autre joueur connecté, ou d'un inconnu. */
    static String someone(ServerLevel world, ServerPlayer player) {
        List<String> names = new ArrayList<>();
        for (ServerPlayer other : world.getServer().getPlayerList().getPlayers()) {
            if (other != player) names.add(other.getName().getString());
        }
        if (names.isEmpty() || world.getRandom().nextInt(3) == 0) return NAMES[world.getRandom().nextInt(NAMES.length)];
        return names.get(world.getRandom().nextInt(names.size()));
    }
}
