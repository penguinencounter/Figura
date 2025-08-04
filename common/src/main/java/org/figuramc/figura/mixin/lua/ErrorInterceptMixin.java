package org.figuramc.figura.mixin.lua;

import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.figuramc.figura.lua.errors.LuaErrorCapture;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.lib.DebugLib;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value=LuaClosure.class, remap=false)
public abstract class ErrorInterceptMixin {
    @Shadow @Final Globals globals;

    @Inject(method = "processErrorHooks", at = @At(value = "TAIL"))
    private void figura$collectErrorInfo(LuaError le, Prototype p, int pc, CallbackInfo ci) {
        if (globals == null) return; // something's gone wrong here
        DebugLib debug = globals.debuglib;
        FiguraLuaRuntime runtime = FiguraLuaRuntime.getForGlobals(globals);
        LuaClosure that = (LuaClosure) (Object) this;
        if (runtime == null) return;
        runtime.isSyntaxError = false;
        if (debug == null) {
            runtime.lastError = LuaErrorCapture.capturePartial(runtime, le, that, pc);
        } else {
            List<DebugLib.CallFrame> frames = new ArrayList<>();
            int n = 1;
            DebugLib.CallFrame frame;
            while ((frame = debug.getCallFrame(n++)) != null) {
                frames.add(frame);
            }
            runtime.lastError = LuaErrorCapture.captureFull(runtime, le, frames);
        }
    }
}
