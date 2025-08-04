package org.figuramc.figura.lua.api;

import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.lua.errors.AnalysisTools;
import org.figuramc.figura.lua.errors.Instruction;
import org.figuramc.figura.lua.errors.LuaErrorCapture;
import org.figuramc.figura.lua.errors.ProtoCache;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.lib.DebugLib;

/**
 * Debugging tools for the debugging tools.
 * Lets you do some really cursed stuff.
 */
@LuaWhitelist
@LuaTypeDoc(name = "HintingAPI", value = "hinting")
public class HintingAPI {
    private final DebugLib debugAccess;

    public HintingAPI(FiguraLuaRuntime runtime) {
        this.debugAccess = runtime.userGlobals.debuglib;
    }

    @LuaWhitelist
    public LuaString getLocalName(LuaValue anything) {
        LuaErrorCapture.PCFrame cf1 = LuaErrorCapture.PCFrame.of(debugAccess.getCallFrame(1));
        Prototype p = cf1.c.p;
        Instruction i = Instruction.of(cf1.pc, p.lineinfo[cf1.pc], p.code[cf1.pc]);
        if (i instanceof Instruction.Call) {
            int reg = ((Instruction.Call) i).function + 2; // (self included)
            ProtoCache chart = new ProtoCache(p);
            while (true) {
                LuaString loc;
                if ((loc = p.getlocalname(reg + 1, i.pc)) != null) return loc;
                try {
                    i = AnalysisTools.findOriginOnce(chart, reg, i.pc);
                } catch (AnalysisTools.TracebackException e) {
                    return null;
                }
                if (i instanceof Instruction.Move) {
                    reg = ((Instruction.Move) i).from;
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "HintingAPI";
    }
}
