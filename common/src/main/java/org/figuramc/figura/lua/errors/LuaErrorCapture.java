package org.figuramc.figura.lua.errors;

import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.figuramc.figura.mixin.lua.CallFrameAccessor;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.DebugLib;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LuaErrorCapture {
    @SuppressWarnings("ClassCanBeRecord")
    public static class PCFrame {
        public final LuaClosure c;
        public final int pc;
        public final LuaValue[] stackView;

        public PCFrame(LuaClosure c, int pc, LuaValue[] stackView) {
            this.c = c;
            this.pc = pc;
            this.stackView = stackView;
        }

        public static PCFrame of(DebugLib.CallFrame frame) {
            CallFrameAccessor access = (CallFrameAccessor) frame;
            LuaClosure c = null;
            if (access.getF() instanceof LuaClosure) c = (LuaClosure) access.getF();
            return new PCFrame(c, access.getPc(), access.getStack());
        }
    }

    public LuaErrorCapture(FiguraLuaRuntime runtime, LuaError errorObj, List<PCFrame> frames) {
        this.runtime = runtime;
        this.errorObj = errorObj;
        this.frames = frames;
    }

    public LuaErrorCapture(FiguraLuaRuntime runtime, LuaError errorObj, PCFrame first) {
        this(runtime, errorObj, List.of(first));
    }

    public final FiguraLuaRuntime runtime;
    public final LuaError errorObj;
    public final List<PCFrame> frames;
    public final Map<Integer, ProtoCache> caches = new HashMap<>();

    public ProtoCache buildCache(int level) {
        PCFrame frame = frames.get(level);
        if (frame == null) return null;
        if (frame.c == null || frame.c.p == null) return null;
        return new ProtoCache(frame.c.p);
    }

    public ProtoCache getPrototypeFrame(int level) {
        return caches.computeIfAbsent(level, this::buildCache);
    }

    public PCFrame getTop() {
        return frames.get(0);
    }

    /**
     * This constructor is used if we for some reason don't have a way to freeze the stack
     * (in practice, this means that the debugging library was missing)
     * @param error The error and level information
     * @param c The currently active LuaClosure
     * @param pc The program counter in this LuaClosure
     * @return a capture of the error state.
     */
    public static LuaErrorCapture capturePartial(FiguraLuaRuntime runtime, LuaError error, LuaClosure c, int pc) {
        return new LuaErrorCapture(runtime, error, new PCFrame(c, pc, null));
    }

    public static LuaErrorCapture captureFull(FiguraLuaRuntime runtime, LuaError error, List<DebugLib.CallFrame> frames) {
        return new LuaErrorCapture(runtime, error, frames.stream().map(PCFrame::of).toList());
    }
}
