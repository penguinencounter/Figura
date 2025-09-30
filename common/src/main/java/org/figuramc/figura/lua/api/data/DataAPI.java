package org.figuramc.figura.lua.api.data;

import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.lua.transfer.FunctionProtectLevel;
import org.figuramc.figura.lua.transfer.PartiallyProtectedFunction;
import org.figuramc.figura.lua.transfer.ProtectedFunction;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;

import java.util.Locale;

@LuaWhitelist
@LuaTypeDoc(name = "DataAPI", value = "data")
public class DataAPI {

    private final Avatar parent;

    public DataAPI(Avatar parent) {
        this.parent = parent;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "data.create_buffer",
            overloads = {
                    @LuaMethodOverload(
                            returnType = FiguraBuffer.class
                    ),
                    @LuaMethodOverload(
                            returnType = FiguraBuffer.class,
                            argumentNames = "capacity",
                            argumentTypes = Integer.class
                    )
            }
    )
    public FiguraBuffer createBuffer(Integer len) {
        return len == null ? new FiguraBuffer(parent) : new FiguraBuffer(parent, len);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "data.protect",
            overloads = @LuaMethodOverload(
                    returnType = LuaFunction.class,
                    argumentNames = {"func", "level"},
                    argumentTypes = {LuaFunction.class, FunctionProtectLevel.class}
            )
    )
    public PartiallyProtectedFunction protect(LuaFunction func, String level) {
        try {
            return new PartiallyProtectedFunction(
                    func,
                    FunctionProtectLevel.valueOf(level.toUpperCase(Locale.ENGLISH))
            );
        } catch (IllegalArgumentException e) {
            throw new LuaError(String.format(
                    "Unknown protection level '%s', acceptable values are %s",
                    level,
                    FunctionProtectLevel.hint
            ));
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "data.unprotect",
            overloads = @LuaMethodOverload(
                    returnType = LuaFunction.class,
                    argumentNames = {"func"},
                    argumentTypes = {LuaFunction.class}
            )
    )
    public LuaFunction unprotect(LuaFunction func) {
        if (func instanceof PartiallyProtectedFunction partial) return partial.around;
        if (func instanceof ProtectedFunction protect) {
            // it's unwrappable IF:
            // provider has 'Nothing' as protection level, and
            // we're the consumer
            if (protect.consumer != parent) throw new LuaError("This function can't be unprotected - not recipient");
            FunctionProtectLevel levelEffective = (
                    protect.ownerOverride != null ? protect.ownerOverride : protect.provider.luaRuntime.avatar_meta.providing
            );
            if (levelEffective != FunctionProtectLevel.NOTHING)
                throw new LuaError("This function has non-NOTHING protection");

            // good luck
            return protect.around;
        }
        throw new LuaError("Function is not protected");
    }

    @Override
    public String toString() {
        return "DataAPI";
    }
}
