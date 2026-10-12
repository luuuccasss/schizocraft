package fr.luuuccasss.schizocraft;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Locale;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Commandes réservées aux ops (le joueur, lui, ne doit pas pouvoir vérifier ce qui est réel) :
 * /paranoia <joueur>                          voir sa paranoïa
 * /paranoia set <joueur> <0-100>              la régler
 * /paranoia hallucination <joueur> <type>     déclencher une hallucination
 */
public final class ParanoiaCommand {
    private ParanoiaCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("paranoia")
            .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
            .then(argument("joueur", EntityArgumentType.player())
                .executes(ctx -> show(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "joueur"))))
            .then(literal("set")
                .then(argument("joueur", EntityArgumentType.player())
                    .then(argument("valeur", FloatArgumentType.floatArg(0, 100))
                        .executes(ctx -> {
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "joueur");
                            ((Paranoid) target).schizocraft$setParanoia(FloatArgumentType.getFloat(ctx, "valeur"));
                            return show(ctx.getSource(), target);
                        }))))
            .then(literal("hallucination")
                .then(argument("joueur", EntityArgumentType.player())
                    .then(argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (Hallucination h : Hallucination.values()) builder.suggest(h.name().toLowerCase(Locale.ROOT));
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "joueur");
                            Hallucination h;
                            try {
                                h = Hallucination.valueOf(StringArgumentType.getString(ctx, "type").toUpperCase(Locale.ROOT));
                            } catch (IllegalArgumentException e) {
                                ctx.getSource().sendError(Text.literal("Hallucination inconnue."));
                                return 0;
                            }
                            h.play(target.getEntityWorld(), target);
                            return 1;
                        })))));
    }

    private static int show(ServerCommandSource source, ServerPlayerEntity player) {
        Paranoid mind = (Paranoid) player;
        float paranoia = mind.schizocraft$getParanoia();
        Stage stage = mind.schizocraft$getStage();
        int filled = Math.round(paranoia / 5f);
        MutableText bar = Text.literal("█".repeat(filled)).formatted(stage.color)
            .append(Text.literal("█".repeat(20 - filled)).formatted(Formatting.DARK_GRAY));
        source.sendFeedback(() -> Text.literal("Paranoïa de ").formatted(Formatting.GRAY)
            .append(player.getDisplayName())
            .append(Text.literal(" : ").formatted(Formatting.GRAY))
            .append(bar)
            .append(Text.literal(String.format(" %d %% · %s", Math.round(paranoia), stage.label)).formatted(stage.color))
            .append(Text.literal(Mind.sleepless(player) ? " · insomnie" : "").formatted(Formatting.DARK_GRAY)), false);
        return Math.round(paranoia);
    }
}
