package org.figuramc.figura.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.backend2.HttpAPI;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.utils.FiguraClientCommandSource;

import java.util.Objects;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

public class BadgeCommand {
    public static LiteralArgumentBuilder<FiguraClientCommandSource> getCommand() {
        LiteralArgumentBuilder<FiguraClientCommandSource> badgeCommand = literal("badge");

        for (Badges.Pride badge: Badges.Pride.values()) {
            LiteralArgumentBuilder<FiguraClientCommandSource> badgeArgument = literal(badge.name().toLowerCase());
            badgeArgument.executes(new BadgeCommandExecutor(badge));

            badgeCommand.then(badgeArgument);
        }

        LiteralArgumentBuilder<FiguraClientCommandSource> clearBadge = literal("clear");
        clearBadge.executes(BadgeCommand::clearBadge);
        badgeCommand.then(clearBadge);

        return badgeCommand;
    }

    private static int clearBadge(CommandContext<FiguraClientCommandSource> ctx) {
        NetworkStuff.clearBadge();
        return 0;
    }

    private static final class BadgeCommandExecutor implements Command<FiguraClientCommandSource> {
        private final Badges.Pride badge;

        private BadgeCommandExecutor(Badges.Pride badge) {
            this.badge = badge;
        }

        public Badges.Pride badge() {
            return badge;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            BadgeCommandExecutor that = (BadgeCommandExecutor) obj;
            return Objects.equals(this.badge, that.badge);
        }

        @Override
        public int hashCode() {
            return Objects.hash(badge);
        }

        @Override
        public String toString() {
            return "BadgeCommandExecutor[" +
                    "badge=" + badge + ']';
        }

        @Override
        public int run(CommandContext<FiguraClientCommandSource> ctx) throws CommandSyntaxException {
            NetworkStuff.setBadge(badge.ordinal());
            return 0;
        }
    }
}
