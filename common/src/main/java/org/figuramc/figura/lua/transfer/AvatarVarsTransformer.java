package org.figuramc.figura.lua.transfer;

import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaTypeManager;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;

public class AvatarVarsTransformer implements LuaTransformer {
    private final LuaTypeManager targetTypes;

    public AvatarVarsTransformer(Avatar consumer) {
        targetTypes = consumer.luaRuntime.typeManager;
    }

    @Override
    public @NotNull LuaValue userdata(LuaUserdata usr) {
        return targetTypes.wrap(usr.m_instance);
    }

    @Override
    public @NotNull LuaValue table(LuaTable t) {
        return TransformerLuaTable.make(t, this).removeMeta();
    }
}
