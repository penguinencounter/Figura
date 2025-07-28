package org.figuramc.figura.lua;

import org.luaj.vm2.LuaValue;

/**
 * Tag classes that support a 'pure' (without-side-effects) version of __index.
 * These classes must implement a {@link #lua_index} method separately to their {@code __index}
 * metamethod that abstracts to {@link LuaValue}s.
 * <br>
 * See {@link Auto} for a partial implementation that takes a {@link LuaTypeManager}
 * and interfaces with the existing {@code __index} instead.
 */
public interface SupportsPureIndex {
    /**
     * <p>
     * Helper mixin interface for building pure-index types.
     * </p>
     * <p>
     * Assumes a custom {@code __index} method exists and is trusted, and a {@link LuaTypeManager} is available.
     * </p>
     */
    interface Auto extends SupportsPureIndex {
        Object __index(String key);
        LuaTypeManager getTypeManager();

        @Override
        default LuaValue lua_index(LuaValue key) {
            return getTypeManager().javaToLua(__index(key.checkjstring())).arg1();
        }
    }

    LuaValue lua_index(LuaValue key);
}
