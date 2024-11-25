package org.figuramc.figura.lua.errors;

import org.luaj.vm2.LuaError;

/// Calling a static method with ':' and vice versa. Might not always be accurate.
///
/// <a href="https://www.lua.org/manual/5.2/manual.html#3.4.9">Lua 5.2 manual: 3.4.9 Function Calls</a>
public class LuaWrongCallTypeError extends LuaError {
    public LuaWrongCallTypeError(LuaTypeError error) {
        super(String.format("%s\n  (did you mean to use a colon to call this method?)", error.getMessage()));
        this.cause = error;
    }
}
