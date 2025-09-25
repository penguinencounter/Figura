package org.figuramc.figura.lua.api.data;

import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.lua.transfer.FunctionProtectLevel;
import org.figuramc.figura.lua.transfer.PartiallyProtectedFunction;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;

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
                    argumentNames = { "func", "level" },
                    argumentTypes = { LuaFunction.class, FunctionProtectLevel.class }
            )
    )
    public PartiallyProtectedFunction protect(LuaFunction func, String level) {
        try {
            return new PartiallyProtectedFunction(func, FunctionProtectLevel.valueOf(level.toUpperCase(Locale.ENGLISH)));
        } catch (IllegalArgumentException e) {
            throw new LuaError(String.format(
                    "Unknown protection level '%s', acceptable values are %s",
                    level,
                    FunctionProtectLevel.hint
            ));
        }
    }

    @Override
    public String toString() {
        return "DataAPI";
    }
}
