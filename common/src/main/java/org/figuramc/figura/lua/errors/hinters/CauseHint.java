package org.figuramc.figura.lua.errors.hinters;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.figuramc.figura.lua.errors.LuaErrorCapture;
import org.figuramc.figura.utils.ColorUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Template for a simple hint with "What went wrong:" translatable text
 */
public abstract class CauseHint implements ErrorHinter {
    @Override
    public @Nullable Component getHint(LuaErrorCapture cap) {
        MutableComponent header = Component.translatable("figura.errors.cause")
                .withStyle(Style.EMPTY.withColor(ColorUtils.Colors.LUA_ERROR_HINT_HEADER.hex));
        return Component.literal("\n").append(header).append("\n").append(getBody(cap)).append("\n");
    }

    @Override
    public int getOrdering() {
        return 1;
    }

    abstract Component getBody(LuaErrorCapture cap);
}
