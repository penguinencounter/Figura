package org.figuramc.figura.lua;

import org.luaj.vm2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuaDumper {
    public static final Logger LOGGER = LoggerFactory.getLogger("Figura Static Analysis");


    public static String getInstructions(CallFrameWrapper frame, int context) {
        StringBuilder main = new StringBuilder();
        main.append("Instruction dump from frame:\n");
        LuaFunction f = frame.get_f();
        int pc = frame.get_pc();
        if (!f.isclosure()) {
            main.append("  No data: Function is not a closure");
            return main.toString();
        }
        Prototype p = f.checkclosure().p;
        int[] code = p.code;
        LuaValue[] kt = p.k;
        int from = Math.max(0, pc - context);
        int to = Math.min(code.length, pc + context);
        for (int i = from; i < to; i++) {
            int unit = code[i];

            int op = Lua.GET_OPCODE(unit);
            int readMode = Lua.getOpMode(op);
            String opcode;
            if (op >= Print.OPNAMES.length - 1) {
                opcode = op + "?";
            } else {
                opcode = Print.OPNAMES[op];
            }

            int a = Lua.GETARG_A(unit);
            int b = Lua.GETARG_B(unit);
            int c = Lua.GETARG_C(unit);
            int bx = Lua.GETARG_Bx(unit);
            int sbx = Lua.GETARG_sBx(unit);

            StringBuilder sb = new StringBuilder();
            sb.append(i == pc ? "> " : "  ").append(i).append(':');

            sb.append(" ").append(opcode).append(" ");
            switch (readMode) {
                case Lua.iABC -> {
                    sb.append(a);
                    if (Lua.getBMode(op) != Lua.OpArgN) {
                        sb.append(" ").append(Lua.ISK(b) ? -1 - Lua.INDEXK(b) : b);
                        if (Lua.ISK(b)) {
                            sb.append(" (").append(kt[Lua.INDEXK(b)]).append(")");
                        }
                    }
                    if (Lua.getCMode(op) != Lua.OpArgN) {
                        sb.append(" ").append(Lua.ISK(c) ? -1 - Lua.INDEXK(c) : c);
                        if (Lua.ISK(c)) {
                            sb.append(" (").append(kt[Lua.INDEXK(c)]).append(")");
                        }
                    }
                }
                case Lua.iABx -> {
                    sb.append(a);
                    if (Lua.getBMode(op) == Lua.OpArgK) {
                        sb.append(" ").append(-1 - bx).append(" (").append(kt[bx]).append(")");
                    } else {
                        sb.append(" ").append(bx);
                    }
                }
                case Lua.iAsBx -> {
                    if (op == Lua.OP_JMP) {
                        sb.append(sbx);
                    } else {
                        sb.append(a).append(" ").append(sbx);
                    }
                }
            }
            main.append(sb).append('\n');
        }
        return main.toString();
    }
}
