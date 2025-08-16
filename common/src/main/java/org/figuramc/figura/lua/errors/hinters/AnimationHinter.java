package org.figuramc.figura.lua.errors.hinters;

import net.minecraft.network.chat.Component;
import org.figuramc.figura.lua.errors.LuaErrorCapture;
import org.jetbrains.annotations.Nullable;

/**
 * Hints for:
 * <ul>
 *     <li>Models in subfolders</li>
 *     <li>Misnamed animations</li>
 *     <li>Models without any animations</li>
 *     <li>Nonexistent models</li>
 * </ul>
 */
public class AnimationHinter implements ErrorHinter{
    @Override
    public int getOrdering() {
        return 11;
    }

    @Override
    public @Nullable Component getHint(LuaErrorCapture cap) {
        return null;
    }
}
