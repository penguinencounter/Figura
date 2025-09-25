package org.figuramc.figura.lua.transfer;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

/**
 * Proxy for a function with attached protection level.
 * Protection level is not actually applied until conversion into {@link ProtectedFunction}.
 */
public class PartiallyProtectedFunction extends VarArgFunction {
    public final LuaFunction around;
    public final FunctionProtectLevel providerLevel;

    public PartiallyProtectedFunction(LuaFunction around, FunctionProtectLevel providerLevel) {
        this.around = around;
        this.providerLevel = providerLevel;
    }

    @Override
    public Varargs invoke(Varargs args) {
        return around.invoke(args);
    }
}
