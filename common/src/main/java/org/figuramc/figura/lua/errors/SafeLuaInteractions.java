package org.figuramc.figura.lua.errors;

import org.figuramc.figura.lua.SupportsPureIndex;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

public class SafeLuaInteractions {
    public static final int MAX_META_DEPTH = 32;

    public static LuaValue safeIndex(LuaValue target, LuaValue key) {
        try {
            return safeIndex(target, key, 0);
        } catch (LuaError e) {
            return null;
        }
    }

    private static LuaValue safeIndex(LuaValue target, LuaValue key, int depth) {
        if (depth >= MAX_META_DEPTH) return null;
        LuaValue result;
        if ((result = target.rawget(key)) != null) return result;
        LuaValue index = target.metatag(LuaValue.INDEX);
        if (index.isnil()) return LuaValue.NIL;
        if (index.istable() || index.isuserdata()) return safeIndex(index, key, depth + 1);
        if (index.isfunction()) {
            if (target.isuserdata()) {
                Object subject = target.checkuserdata();
                if (subject instanceof SupportsPureIndex) {
                    // Note that this might not match if the avatar's done some
                    // tomfoolery with the figuraMetatables object, but we're not
                    // here to be perfect for that subset of users.
                    return ((SupportsPureIndex) subject).lua_index(key);
                }
            }
            /* probably user-provided */
            return null;
        }
        /* unsupported __index type */
        return null;
    }
}
