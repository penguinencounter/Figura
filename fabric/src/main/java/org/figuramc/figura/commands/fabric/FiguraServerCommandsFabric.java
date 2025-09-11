package org.figuramc.figura.commands.fabric;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.figuramc.figura.server.commands.FiguraServerCommandSource;
import org.figuramc.figura.server.commands.FiguraServerCommands;

public class FiguraServerCommandsFabric {
    public static void init() {
        CommandRegistrationCallback.EVENT.register(FiguraServerCommandsFabric::register);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, boolean dedicated) {
        if (dedicated) {
            CommandDispatcher<FiguraServerCommandSource> casted = (CommandDispatcher) dispatcher;
            casted.register(FiguraServerCommands.getCommand());
        }
    }
}
