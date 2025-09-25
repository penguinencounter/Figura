package org.figuramc.figura.lua.transfer;

import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaTypeManager;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;

public class AvatarVarsTransformer implements LuaTransformer {
    private final Avatar provider;
    private final Avatar consumer;
    private final LuaTypeManager targetTypes;

    public AvatarVarsTransformer(
            Avatar provider,
            Avatar consumer
    ) {
        this.provider = provider;
        this.consumer = consumer;
        targetTypes = consumer.luaRuntime.typeManager;
    }

    @Override
    public @NotNull LuaValue userdata(LuaUserdata usr) {
        Object inner = usr.m_instance;
        if (inner instanceof CopyOnTransfer<?> copyable) inner = copyable.copy();
        return targetTypes.wrap(inner);
    }

    @Override
    public @NotNull LuaValue table(LuaTable t) {
        return TransformerLuaTable.make(t, this).removeMeta();
    }

    @Override
    public @NotNull LuaValue function(LuaFunction f) {
        return new ProtectedFunction(f, provider, consumer);
    }
}
