package org.figuramc.figura.lua.errors.hinters;

import net.minecraft.network.chat.Component;
import org.figuramc.figura.lua.errors.AnalysisTools;
import org.figuramc.figura.lua.errors.LuaErrorCapture;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Provides hints for resolving errors in context.
 */
public interface ErrorHinter {
    @Nullable Component getHint(List<AnalysisTools.DataflowElement> origin, LuaErrorCapture cap);
}
