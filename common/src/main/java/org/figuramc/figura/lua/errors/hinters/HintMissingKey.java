package org.figuramc.figura.lua.errors.hinters;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.figuramc.figura.lua.errors.LuaErrorCapture;
import org.figuramc.figura.lua.errors.LuaRendering;
import org.figuramc.figura.utils.ColorUtils;
import org.luaj.vm2.LuaValue;

public class HintMissingKey extends CauseHint {
    private final String target;
    private final LuaValue key;

    public HintMissingKey(String target, LuaValue key) {
        this.target = target;
        this.key = key;
    }

    public static Component formatKey(LuaValue key) {
        LuaRendering.Annotated element;
        if (key.isstring()) element = LuaRendering.indexString(key.checkjstring());
        else element = new LuaRendering.Annotated("[", key.checkjstring(), "]");
        return element.toComponent(
                Style.EMPTY.withColor(ChatFormatting.GRAY),
                ColorUtils.Colors.LUA_ERROR_CAUSE_SUBJECT.style
        );
    }


    @Override
    Component getBody(LuaErrorCapture cap) {
        return Component.translatable(
                "figura.errors.missing_key",
                formatKey(key),
                Component.literal(target).withStyle(ColorUtils.Colors.LUA_ERROR_CAUSE_SUBJECT.style)
        ).withStyle(Style.EMPTY.withColor(ColorUtils.Colors.LUA_ERROR_CAUSE.hex));
    }
}
