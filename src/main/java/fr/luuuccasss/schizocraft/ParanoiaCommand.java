package fr.luuuccasss.schizocraft;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commandes réservées aux ops (le joueur, lui, ne doit pas pouvoir vérifier ce qui est réel) :
 * /paranoia <joueur>                          voir sa paranoïa
 * /paranoia set <joueur> <0-100>              la régler
 * /paranoia hallucination <joueur> <type>     déclencher une hallucination
 */
public final class ParanoiaCommand {
    private ParanoiaCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("paranoia")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(argument("joueur", EntityArgument.player())
                .executes(ctx -> show(ctx.getSource(), EntityArgument.getPlayer(ctx, "joueur"))))
            .then(literal("set")
                .then(argument("joueur", EntityArgument.player())
                    .then(argument("valeur", FloatArgumentType.floatArg(0, 100))
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "joueur");
                            ((Paranoid) target).schizocraft$setParanoia(FloatArgumentType.getFloat(ctx, "valeur"));
                            return show(ctx.getSource(), target);
                        }))))
            .then(literal("hallucination")
                .then(argument("joueur", EntityArgument.player())
                    .then(argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (Hallucination h : Hallucination.values()) builder.suggest(h.name().toLowerCase(Locale.ROOT));
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "joueur");
                            Hallucination h;
                            try {
                                h = Hallucination.valueOf(StringArgumentType.getString(ctx, "type").toUpperCase(Locale.ROOT));
                            } catch (IllegalArgumentException e) {
                                ctx.getSource().sendFailure(Component.literal("Hallucination inconnue."));
                                return 0;
                            }
                            h.play(target.level(), target);
                            return 1;
                        })))));
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        Paranoid mind = (Paranoid) player;
        float paranoia = mind.schizocraft$getParanoia();
        Stage stage = mind.schizocraft$getStage();
        int filled = Math.round(paranoia / 5f);
        MutableComponent bar = Component.literal("█".repeat(filled)).withStyle(stage.color)
            .append(Component.literal("█".repeat(20 - filled)).withStyle(ChatFormatting.DARK_GRAY));
        source.sendSuccess(() -> Component.literal("Paranoïa de ").withStyle(ChatFormatting.GRAY)
            .append(player.getDisplayName())
            .append(Component.literal(" : ").withStyle(ChatFormatting.GRAY))
            .append(bar)
            .append(Component.literal(String.format(" %d %% · %s", Math.round(paranoia), stage.label)).withStyle(stage.color))
            .append(Component.literal(Mind.sleepless(player) ? " · insomnie" : "").withStyle(ChatFormatting.DARK_GRAY)), false);
        return Math.round(paranoia);
    }
}
