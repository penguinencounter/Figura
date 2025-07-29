package org.figuramc.figura.lua.errors;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class LuaRendering {
    public record Annotated(String left, String main, String right) {
        public Component toComponent(Style outer, Style inner) {
            if (left.isEmpty() && right.isEmpty()) return Component.literal(main).withStyle(inner);
            return Component.literal(left).withStyle(outer)
                    .append(Component.literal(main).withStyle(inner))
                    .append(Component.literal(right));
        }

        public String flatten() {
            return left + main + right;
        }
    }

    public static Annotated indexString(String key) {
        if (key.matches("^[a-zA-Z_][a-zA-Z0-9_]*$"))
            return new Annotated(".", key, "");
        return new Annotated("[\"", key.replaceAll("[\"\\\\]", "\\$1"), "\"]");
    }
}
