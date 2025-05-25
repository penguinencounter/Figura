package org.figuramc.figura.mixin.lua;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.DebugLib;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = DebugLib.CallFrame.class, remap = false)
public interface CallFrameAccessor {
    @Accessor
    LuaFunction getF();
    @Accessor
    int getPc();
    @Accessor
    LuaValue[] getStack();
}
