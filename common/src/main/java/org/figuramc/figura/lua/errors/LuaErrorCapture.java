package org.figuramc.figura.lua.errors;

import org.figuramc.figura.mixin.lua.CallFrameAccessor;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.DebugLib;

import java.util.List;

public class LuaErrorCapture {
    @SuppressWarnings("ClassCanBeRecord")
    public static class PCFrame {
        public final Prototype p;
        public final int pc;
        public final LuaValue[] stackView;

        public PCFrame(Prototype p, int pc, LuaValue[] stackView) {
            this.p = p;
            this.pc = pc;
            this.stackView = stackView;
        }

        public static PCFrame of(DebugLib.CallFrame frame) {
            CallFrameAccessor access = (CallFrameAccessor) frame;
            Prototype p = null;
            if (access.getF() instanceof LuaClosure) p = ((LuaClosure) access.getF()).p;
            return new PCFrame(p, access.getPc(), access.getStack());
        }
    }

    public LuaErrorCapture(LuaError errorObj, List<PCFrame> frames) {
        this.errorObj = errorObj;
        this.frames = frames;
    }

    public LuaErrorCapture(LuaError errorObj, PCFrame first) {
        this(errorObj, List.of(first));
    }

    public final LuaError errorObj;
    public final List<PCFrame> frames;

    /**
     * This constructor is used if we for some reason don't have a way to freeze the stack
     * (in practice, this means that the debugging library was missing)
     * @param error The error and level information
     * @param p The currently active Prototype
     * @param pc The program counter in this Prototype
     * @return a capture of the error state.
     */
    public static LuaErrorCapture capturePartial(LuaError error, Prototype p, int pc) {
        return new LuaErrorCapture(error, new PCFrame(p, pc, null));
    }

    public static LuaErrorCapture captureFull(LuaError error, List<DebugLib.CallFrame> frames) {
        return new LuaErrorCapture(error, frames.stream().map(PCFrame::of).toList());
    }
}
