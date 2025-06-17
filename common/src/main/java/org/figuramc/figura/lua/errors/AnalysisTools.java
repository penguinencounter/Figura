package org.figuramc.figura.lua.errors;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnalysisTools {
    private interface Analyzer {
        boolean accepts(LuaErrorCapture cap);
        Component execute(LuaErrorCapture cap);
    }

    private static LuaValue k(Prototype p, int kidx) {
        return p.k[kidx];
    }

    private static abstract class DataflowElement {
        abstract LuaValue resolve(@Nullable LuaValue previousValue, LuaErrorCapture cap, int level);
    }

    private static class FunctionArgument extends DataflowElement {
        final int argIndex;

        FunctionArgument(int argIndex) {
            this.argIndex = argIndex;
        }

        @Override
        public LuaValue resolve(@Nullable LuaValue previousValue, LuaErrorCapture cap, int level) {
            // Is the argument still available?
            LuaErrorCapture.PCFrame thisFrame = cap.frames.get(level);
            ProtoCache dataflowInfo = cap.getPrototypeFrame(level);
            int clobberedAt = dataflowInfo.getNextWrite(argIndex, -1);
            if (clobberedAt >= thisFrame.pc) { // it's free real estate
                return thisFrame.stackView[argIndex];
            }
            // we could do a bit better by looking for OP_MOVE aliases, but that's a bit too much effort for
            // this already over-engineered feature

            // Looks like we can't do that; try yoinking it from the caller?
            LuaErrorCapture.PCFrame above = cap.frames.get(level + 1);
            if (above == null) return null; // 'main chunk'
            if (above.c == null) return null; // Java functions (e.g. applyFunc)
            LuaClosure callingClosure = above.c;
            Instruction callLike = Instruction.of(above.pc, callingClosure.p.code[above.pc]);
            if (callLike instanceof Instruction.Call) {
                Instruction.Call call = (Instruction.Call) callLike;
                if (call.argc == -1) return null; // Varargs are nasty stuff tbh
                int outerReg = call.function + argIndex + 1;
                return above.stackView[outerReg];
            }
            // tailcalls aren't supported because the stack is already gone
            // other reasons we might be here: error is in a metamethod
            return null;
        }
    }

    private static class GetUpValue extends DataflowElement {
        final int uvIndex;

        GetUpValue(int uvIndex) {
            this.uvIndex = uvIndex;
        }

        @Override
        public LuaValue resolve(@Nullable LuaValue previousValue, LuaErrorCapture cap, int level) {
            return cap.frames.get(level).c.upValues[uvIndex].getValue();
        }
    }

    private static class ConstantIndexTable extends DataflowElement {
        final LuaValue key;

        ConstantIndexTable(LuaValue key) {
            this.key = key;
        }

        @Override
        public LuaValue resolve(@Nullable LuaValue previousValue, LuaErrorCapture cap, int level) {
            if (previousValue == null) return null;
            return SafeLuaInteractions.safeIndex(previousValue, key);
        }
    }

    private static Instruction findOriginOnce(ProtoCache chart, int register, int pc) {
        pc--; // start scanning 1 instruction before...
        Instruction instr;
        while ((instr = chart.get(pc)) != null) {
            if (instr instanceof Instruction.Unknown) return null; // no idea what this instruction does?
            // does this instruction write this value?
            if (instr.modifies().contains(register)) return instr;
            if (instr.inbound.size() != 1) return null; // no definite previous instruction
            pc = instr.inbound.iterator().next();
        }
        return null; // chart has a hole in it
    }

    private static List<DataflowElement> findOriginAll(ProtoCache chart, int register, int pc, LuaClosure c) {
        ArrayList<DataflowElement> chain = new ArrayList<>();
        Instruction current;
        while ((current = findOriginOnce(chart, register, pc)) != null) {

        }
        throw new RuntimeException("err");
    }

    private static final Analyzer indexNilValue = new Analyzer() {
        @Override
        public boolean accepts(LuaErrorCapture cap) {
            if (!cap.errorObj.getMessage().contains("attempt to index ? (a nil value) with key")) return false;
            return cap.getTop().c != null;
        }

        private Component singleBlame(String rationale) {
            return Component.empty().withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA))
                    .append(
                            Component.literal("Analysis: ")
                    ).append(
                            Component.literal(rationale).withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))
                    ).append(
                            Component.literal(" could not be found.")
                    );
        }

        @Override
        public Component execute(LuaErrorCapture cap) {
            @NotNull Prototype p = cap.getTop().c.p;
            int pc = cap.getTop().pc;
            ProtoCache cache = cap.getPrototypeFrame(0);
            Instruction errorCause = cache.decoded.get(pc);
            if (errorCause == null) return null;

            if (errorCause instanceof Instruction.GetTable) {
                int registerCause = ((Instruction.GetTable) errorCause).src;
                Instruction previous = findOriginOnce(cache, registerCause, pc);
                if (previous == null) {
                    return null;
                }
                if (previous instanceof Instruction.TableAccess) {
                    Instruction.TableAccess casted = (Instruction.TableAccess) previous;
                    if (casted.isIndexConstant()) {
                        String reason = p.k[casted.getIndexValue()].tojstring();
                        return singleBlame(reason);
                    } else {
                        // uhhh don't want to resolve variables here...
                    }
                }
            } else if (errorCause instanceof Instruction.GetTabUp) {
                Upvaldesc upv = p.upvalues[((Instruction.GetTabUp) errorCause).upval];
                String reason = upv.name.tojstring();
                return singleBlame(reason);
            }

            return null;
        }
    };

    private static final List<Analyzer> allAnalyzers = List.of(
            indexNilValue
    );

    public static Component analyze(LuaErrorCapture cap) {
        for (Analyzer analyzer : allAnalyzers) {
            if (analyzer.accepts(cap)) {
                return analyzer.execute(cap);
            }
        }
        return null;
    }
}
