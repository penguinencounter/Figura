package org.figuramc.figura.lua.errors;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.figuramc.figura.lua.errors.hinters.ImmediateModelPartNameHinter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.Upvaldesc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnalysisTools {
    private interface Analyzer {
        boolean accepts(LuaErrorCapture cap);

        Component execute(LuaErrorCapture cap);
    }

    private static LuaValue k(Prototype p, int kidx) {
        return p.k[kidx];
    }

    public static abstract class DataflowElement {
        public LuaValue valueAtHere = null;

        public LuaValue resolve(@Nullable LuaValue previousValue, LuaErrorCapture cap, int level) {
            LuaValue result = resolveInternal(previousValue, cap, level);
            valueAtHere = result;
            return result;
        }

        protected abstract LuaValue resolveInternal(@Nullable LuaValue previousValue, LuaErrorCapture cap, int level);
    }

    /**
     * Last resort if we don't know where it came from. Passes the previous value straight through.
     */
    public static class Register extends DataflowElement {
        final int reg;

        Register(int reg) {
            this.reg = reg;
        }

        @Override
        protected LuaValue resolveInternal(@Nullable LuaValue previousValue, LuaErrorCapture cap, int level) {
            return previousValue;
        }
    }

    public static class FunctionArgument extends DataflowElement {
        final int argIndex;

        FunctionArgument(int argIndex) {
            this.argIndex = argIndex;
        }

        @Override
        public LuaValue resolveInternal(@Nullable LuaValue previousValue, LuaErrorCapture cap, int level) {
            // Is the argument still available?
            LuaErrorCapture.PCFrame thisFrame = cap.frames.get(level);
            ProtoCache dataflowInfo = cap.getPrototypeFrame(level);
            int clobberedAt = dataflowInfo.getNextWrite(argIndex, -1);
            if (clobberedAt >= thisFrame.pc) { // it's free real estate
                return thisFrame.stackView[argIndex];
            }
            // we could do a bit better by looking for OP_MOVE aliases but whatever

            // Looks like we can't do that; try yoinking it from the caller?
            LuaErrorCapture.PCFrame above = cap.frames.get(level + 1);
            if (above == null) return null; // 'main chunk'
            if (above.c == null) return null; // Java functions (e.g. applyFunc)
            LuaClosure callingClosure = above.c;
            Instruction callLike = Instruction.of(
                    above.pc,
                    callingClosure.p.lineinfo[above.pc],
                    callingClosure.p.code[above.pc]
            );
            if (callLike instanceof Instruction.Call) {
                Instruction.Call call = (Instruction.Call) callLike;
                if (call.argc == -1) return null; // Varargs are nasty stuff
                int outerReg = call.function + argIndex + 1;
                return above.stackView[outerReg];
            }
            // tailcalls aren't supported because the stack is already gone
            // other reasons we might be here: error is in a metamethod
            return null;
        }
    }

    public static class GetUpValue extends DataflowElement {
        final int uvIndex;

        GetUpValue(int uvIndex) {
            this.uvIndex = uvIndex;
        }

        @Override
        public LuaValue resolveInternal(@Nullable LuaValue previousValue, LuaErrorCapture cap, int level) {
            return cap.frames.get(level).c.upValues[uvIndex].getValue();
        }
    }

    public static class ConstantIndexTable extends DataflowElement {
        public final LuaValue key;
        public final @Nullable Integer regSrc;

        ConstantIndexTable(LuaValue key, @Nullable Integer src) {
            this.key = key;
            this.regSrc = src;
        }

        @Override
        public LuaValue resolveInternal(@Nullable LuaValue previousValue, LuaErrorCapture cap, int level) {
            if (previousValue == null) {
                if (regSrc == null) return null;
                // Maybe we can yoink the reference from the register?
                LuaErrorCapture.PCFrame thisFrame = cap.frames.get(level);
                ProtoCache dataflowInfo = cap.getPrototypeFrame(level);
                int clobberedAt = dataflowInfo.getNextWrite(regSrc, -1);
                if (clobberedAt < thisFrame.pc) {
                    return null; // too bad :(
                }
                return SafeLuaInteractions.safeIndex(thisFrame.stackView[regSrc], key);
            } else return SafeLuaInteractions.safeIndex(previousValue, key);
        }
    }

    private static abstract class TracebackException extends Exception {
        // reduce performance impact of throwing this
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    private static class ReachedStartOfFunction extends TracebackException {
    }

    private static class MissingInstructionAtPC extends TracebackException {
        public final int pc;

        private MissingInstructionAtPC(int pc) {
            this.pc = pc;
        }
    }

    private static class MultipleOptions extends TracebackException {
    }

    private static class UnknownOpcode extends TracebackException {
        public final int pc;

        private UnknownOpcode(int pc) {
            this.pc = pc;
        }
    }


    private static Instruction findOriginOnce(
            ProtoCache chart,
            int register,
            int pc
    ) throws ReachedStartOfFunction, MissingInstructionAtPC, MultipleOptions, UnknownOpcode {
        pc--; // start scanning 1 instruction before...
        Instruction instr;
        while ((instr = chart.get(pc)) != null) {
            if (instr instanceof Instruction.Unknown)
                throw new UnknownOpcode(pc); // no idea what this instruction does?
            // does this instruction write this value?
            if (instr.modifies().contains(register)) return instr;
            if (instr.inbound.size() > 1) throw new MultipleOptions(); // no definite previous instruction
            // we can be confident about this because all jumping opcodes are known
            if (instr.inbound.isEmpty()) throw new ReachedStartOfFunction();
            pc = instr.inbound.iterator().next();
        }
        throw new MissingInstructionAtPC(pc); // chart has a hole in it
    }

    private static List<DataflowElement> findOriginAll(ProtoCache chart, int register, int pc, Prototype p) {
        ArrayList<DataflowElement> chain = new ArrayList<>();
        Instruction current;
        while (true) {
            try {
                current = findOriginOnce(chart, register, pc);
            } catch (ReachedStartOfFunction e) {
                if (register < p.numparams)
                    chain.add(new FunctionArgument(register));
                else
                    chain.add(new Register(register));
                break;
            } catch (MissingInstructionAtPC | MultipleOptions | UnknownOpcode e) {
                chain.add(new Register(register));
                break;
            }
            pc = current.pc;
            if (current instanceof Instruction.GetUpVal) {
                chain.add(new GetUpValue(((Instruction.GetUpVal) current).upval));
                break;
            } else if (current instanceof Instruction.GetTabUp) {
                Instruction.GetTabUp typed = (Instruction.GetTabUp) current;
                if (!typed.isk) {
                    chain.add(new Register(typed.to));
                    break;
                }
                chain.add(new ConstantIndexTable(p.k[typed.rk], null));
                chain.add(new GetUpValue(typed.upval));
                break;
            } else if (current instanceof Instruction.GetTable) {
                Instruction.GetTable typed = (Instruction.GetTable) current;
                if (!typed.isk) {
                    chain.add(new Register(typed.to));
                    break;
                }
                register = typed.src;
                chain.add(new ConstantIndexTable(p.k[typed.rk], typed.src));
            } else if (current instanceof Instruction.Self) {
                Instruction.Self typed = (Instruction.Self) current;
                if (!typed.isk) {
                    chain.add(new Register(typed.to));
                    break;
                }
                register = typed.src;
                chain.add(new ConstantIndexTable(p.k[typed.rk], typed.src));
            } else if (current instanceof Instruction.Move) {
                register = ((Instruction.Move) current).from;
            }
        }
        Collections.reverse(chain);
        return chain;
    }

    private static final Analyzer indexNilValue = new Analyzer() {
        public static final ErrorType TYPE = ErrorType.INDEX_NIL;

        @Override
        public boolean accepts(LuaErrorCapture cap) {
            if (!cap.errorObj.getMessage().contains("attempt to index ? (a nil value) with key")) return false;
            return cap.getTop().c != null;
        }

        private Component singleBlame(String rationale, Instruction at) {
            return Component.empty().withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA))
                    .append(
                            Component.literal("Analysis: ")
                    ).append(
                            Component.literal(rationale).withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))
                    ).append(
                            Component.literal(" could not be found.")
                    ).append(
                            Component.literal(" [line ").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
                                    .append(String.valueOf(at.line))
                                    .append(Component.literal(" pc ")
                                            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY))
                                            .append(String.valueOf(at.pc))
                                            .append(" ")
                                            .append(at.getClass().getSimpleName())
                                    )
                                    .append("]")
                    );
        }

        @Override
        public Component execute(LuaErrorCapture cap) {
            @NotNull Prototype p = cap.getTop().c.p;
            int pc = cap.getTop().pc;
            ProtoCache cache = cap.getPrototypeFrame(0);
            Instruction errorCause = cache.decoded.get(pc);
            if (errorCause == null) return null;

            ArrayList<Component> hints = new ArrayList<>();

            boolean isGetTable = errorCause instanceof Instruction.GetTable;
            boolean isSelf = errorCause instanceof Instruction.Self;
            if (isGetTable || isSelf) {
                final int registerCause;
                if (isGetTable)
                    registerCause = ((Instruction.GetTable) errorCause).src;
                else
                    registerCause = ((Instruction.Self) errorCause).src;

                List<DataflowElement> stack = findOriginAll(cache, registerCause, pc, p);
                LuaValue parent = null;
                for (DataflowElement element : stack) {
                    parent = element.resolve(parent, cap, 0);
                }

                Component mpHint = new ImmediateModelPartNameHinter().getHint(stack, cap);
                if (mpHint != null) hints.add(mpHint);
            } else if (errorCause instanceof Instruction.GetTabUp) {
                Upvaldesc upv = p.upvalues[((Instruction.GetTabUp) errorCause).upval];
                String reason = upv.name.tojstring();
                return singleBlame(reason, errorCause);
            }

            if (!hints.isEmpty()) hints.add(Component.literal("-- INCLUDE ALL OF THE ABOVE IN YOUR SCREENSHOT --")
                    .withStyle(ChatFormatting.YELLOW));

            MutableComponent finalHints = Component.literal("\n");
            boolean isFirst = true;
            for (Component c : hints) {
                if (!isFirst)
                    finalHints.append("\n");
                finalHints.append(c);
                isFirst = false;
            }

            return finalHints;
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
