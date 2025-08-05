package org.figuramc.figura.lua.errors.hinters;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.figuramc.figura.lua.errors.LuaErrorCapture;
import org.figuramc.figura.utils.ColorUtils;

public class HintMissingUpval extends CauseHint {
    private final String upvalName;

    public HintMissingUpval(String upvalName) {
        this.upvalName = upvalName;
    }

    @Override
    Component getBody(LuaErrorCapture cap) {
        return Component.translatable(
                "figura.errors.missing_upvalue",
                Component.literal(upvalName)
                        .withStyle(Style.EMPTY.withColor(ColorUtils.Colors.LUA_ERROR_CAUSE_SUBJECT.hex))
        ).withStyle(Style.EMPTY.withColor(ColorUtils.Colors.LUA_ERROR_CAUSE.hex));
    }
}
