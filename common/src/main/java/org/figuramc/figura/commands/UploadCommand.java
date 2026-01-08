package org.figuramc.figura.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.network.chat.Component;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.gui.widgets.lists.AvatarList;
import org.figuramc.figura.lua.LuaTypeManager;
import org.figuramc.figura.utils.FiguraClientCommandSource;

import java.util.Set;

class UploadCommand {
    public static LiteralArgumentBuilder<FiguraClientCommandSource> getCommand() {
        LiteralArgumentBuilder<FiguraClientCommandSource> upload = LiteralArgumentBuilder.literal("upload");
        upload.executes(context -> {
            // Check stack frames
            StackTraceElement[] frames = Thread.currentThread().getStackTrace();
            int i = 0;
            for (StackTraceElement frame : frames) {
                if (frame.getClassName().startsWith("org.figuramc.figura.lua")) {
                    context.getSource().figura$sendError(
                            Component.literal("Uploading avatars using Figura scripts is not supported.")
                    );
                    return 0;
                }
                FiguraMod.LOGGER.info("frame {} : {} {} {}:{}", i++, frame.getClassName(), frame.getMethodName(), frame.getFileName(), frame.getLineNumber());
            }

            Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());

            NetworkStuff.uploadAvatar(avatar);
            AvatarList.selectedEntry = null;
            return 1;
        });
        return upload;
    }
}