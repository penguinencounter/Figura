package org.figuramc.figura.lua.errors.hinters;

import net.minecraft.network.chat.Component;
import org.figuramc.figura.lua.errors.AnalysisTools;
import org.figuramc.figura.lua.errors.LuaErrorCapture;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * hints for names in another part
 */
public class SimilarNameModelPartNameHinter implements ErrorHinter {
    public static final int MAX_DETAILED = 3;
    public static final int MAX_OTHERS = 12;

    @Override
    public @Nullable Component getHint(List<AnalysisTools.DataflowElement> origin, LuaErrorCapture cap) {
        return null;
    }
}
