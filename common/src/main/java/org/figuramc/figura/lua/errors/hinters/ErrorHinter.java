package org.figuramc.figura.lua.errors.hinters;

import net.minecraft.network.chat.Component;
import org.figuramc.figura.lua.errors.LuaErrorCapture;
import org.jetbrains.annotations.Nullable;

/**
 * Provides hints for resolving errors in context.
 */
public interface ErrorHinter {
    int getOrdering();

    @Nullable Component getHint(LuaErrorCapture cap);
}
