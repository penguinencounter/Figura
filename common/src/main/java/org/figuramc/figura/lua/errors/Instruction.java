package org.figuramc.figura.lua.errors;


import org.luaj.vm2.Prototype;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.luaj.vm2.Lua.*;

public abstract class Instruction {
    public final int pc;
    public final int line;

    public final Set<Integer> inbound = new HashSet<>();

    public void markOutgoing(Map<Integer, Instruction> context) {
        Instruction instr = context.get(pc + 1);
        if (instr != null) instr.inbound.add(pc);
    }

    public abstract int getOpcode();

    public abstract Set<Integer> modifies();

    public abstract Set<Integer> consumes();

    private static final Set<Integer> NOTHING = Set.of();

    interface TableAccess {
        int getIndexValue();

        boolean isIndexConstant();
    }

    /// Move data from register [#from] to register [#to].
    public static class Move extends Instruction {
        public final int from;
        public final int to;

        public Move(int pc, int line, int i) {
            super(pc, line);
            from = GETARG_B(i);
            to = GETARG_A(i);
        }

        @Override
        public int getOpcode() {
            return OP_MOVE;
        }

        @Override
        public Set<Integer> modifies() {
            return Set.of(to);
        }

        @Override
        public Set<Integer> consumes() {
            return Set.of(from);
        }
    }

    public static abstract class LoadInstr extends Instruction {
        public LoadInstr(int pc, int line) {
            super(pc, line);
        }

        @Override
        public Set<Integer> consumes() {
            return NOTHING;
        }
    }

    /// Load constant [#k] into register [#to].
    public static class LoadK extends LoadInstr {
        public final int k;
        public final int to;

        public LoadK(int pc, int line, int i) {
            super(pc, line);
            k = GETARG_Bx(i);
            to = GETARG_A(i);
        }

        @Override
        public int getOpcode() {
            return OP_LOADK;
        }

        @Override
        public Set<Integer> modifies() {
            return Set.of(to);
        }
    }

    /// Load constant in subsequent ExtraArg into register [#to].
    public static class LoadKX extends LoadInstr {
        public final int to;

        public LoadKX(int pc, int line, int i) {
            super(pc, line);
            to = GETARG_A(i);
        }

        @Override
        public int getOpcode() {
            return OP_LOADKX;
        }

        @Override
        public Set<Integer> modifies() {
            return Set.of(to);
        }

        @Override
        public void markOutgoing(Map<Integer, Instruction> context) {
            Instruction next = context.get(pc + 2); // skip EXTRAARG
            if (next != null) next.inbound.add(pc);
        }
    }

    /// Load the constant boolean value [#value] into register [#to]
    public static class LoadBool extends LoadInstr {
        public final int to;
        public final boolean value;
        public final boolean skipNext;

        public LoadBool(int pc, int line, int i) {
            super(pc, line);
            to = GETARG_A(i);
            value = GETARG_B(i) != 0;
            skipNext = GETARG_C(i) != 0;
        }

        @Override
        public int getOpcode() {
            return OP_LOADBOOL;
        }

        @Override
        public Set<Integer> modifies() {
            return Set.of(to);
        }

        @Override
        public void markOutgoing(Map<Integer, Instruction> context) {
            if (skipNext) {
                Instruction next = context.get(pc + 2);
                if (next != null) next.inbound.add(pc);
            } else {
                super.markOutgoing(context);
            }
        }
    }

    /// Load [#count]+1 nils starting at register [#to]
    public static class LoadNil extends LoadInstr {
        public final int count;
        public final int basis;
        private final Set<Integer> span = new HashSet<>();

        public LoadNil(int pc, int line, int i) {
            super(pc, line);
            basis = GETARG_A(i);
            count = GETARG_B(i);
            int x = basis;
            for (int c = count; c-- >= 0; ) {
                span.add(x++);
            }
        }

        @Override
        public int getOpcode() {
            return OP_LOADNIL;
        }

        @Override
        public Set<Integer> modifies() {
            return span;
        }
    }

    /// Load upvalue [#upval] into register [#to]
    public static class GetUpVal extends LoadInstr {
        public final int to;
        public final int upval;

        public GetUpVal(int pc, int line, int i) {
            super(pc, line);
            to = GETARG_A(i);
            upval = GETARG_B(i);
        }

        @Override
        public int getOpcode() {
            return OP_GETUPVAL;
        }

        @Override
        public Set<Integer> modifies() {
            return Set.of(to);
        }
    }

    /// Load upvalue [#upval] indexed with reg/k [#rk] (see [#isk] for disamb.) into register [#to]
    public static class GetTabUp extends Instruction implements TableAccess {
        public final int to;
        public final int upval;
        public final int rk;
        public final boolean isk;

        public GetTabUp(int pc, int line, int i) {
            super(pc, line);
            to = GETARG_A(i);
            upval = GETARG_B(i);
            int rk_actual = GETARG_C(i);
            isk = ISK(rk_actual);
            rk = isk ? INDEXK(rk_actual) : rk_actual;
        }

        @Override
        public int getOpcode() {
            return OP_GETTABUP;
        }

        @Override
        public Set<Integer> modifies() {
            return Set.of(to);
        }

        @Override
        public Set<Integer> consumes() {
            return isk ? NOTHING : Set.of(rk);
        }

        @Override
        public int getIndexValue() {
            return rk;
        }

        @Override
        public boolean isIndexConstant() {
            return isk;
        }
    }

    /// Index table [#src] with key [#rk] (disamb. [#isk]) and put result into register [#to]
    public static class GetTable extends Instruction implements TableAccess {
        public final int to;
        public final int src;
        public final int rk;
        public final boolean isk;

        public GetTable(int pc, int line, int i) {
            super(pc, line);
            to = GETARG_A(i);
            src = GETARG_B(i);
            int actual_rk = GETARG_C(i);
            isk = ISK(actual_rk);
            rk = isk ? INDEXK(actual_rk) : actual_rk;
        }

        @Override
        public int getOpcode() {
            return OP_GETTABLE;
        }

        @Override
        public Set<Integer> modifies() {
            return Set.of(to);
        }

        @Override
        public Set<Integer> consumes() {
            return isk ? Set.of(src) : Set.of(src, rk);
        }

        @Override
        public int getIndexValue() {
            return rk;
        }

        @Override
        public boolean isIndexConstant() {
            return isk;
        }
    }

    public static class Self extends Instruction implements TableAccess {
        public final int to;
        public final int src;
        public final boolean isk;
        public final int rk;

        public Self(int pc, int line, int i) {
            super(pc, line);
            to = GETARG_A(i);
            src = GETARG_B(i);
            int actual_rk = GETARG_C(i);
            isk = ISK(actual_rk);
            rk = isk ? INDEXK(actual_rk) : actual_rk;
        }

        @Override
        public int getOpcode() {
            return OP_SELF;
        }

        @Override
        public Set<Integer> modifies() {
            return Set.of(to, to + 1);
        }

        @Override
        public Set<Integer> consumes() {
            return isk ? Set.of(src) : Set.of(src, rk);
        }

        @Override
        public int getIndexValue() {
            return rk;
        }

        @Override
        public boolean isIndexConstant() {
            return isk;
        }
    }

    public static class Jmp extends Instruction {
        public final boolean closeUpvalues;
        public final int pcoffset;

        public Jmp(int pc, int line, int i) {
            super(pc, line);
            pcoffset = GETARG_sBx(i);
            closeUpvalues = GETARG_A(i) != 0;
        }

        @Override
        public int getOpcode() {
            return OP_JMP;
        }

        @Override
        public Set<Integer> modifies() {
            return NOTHING;
        }

        @Override
        public Set<Integer> consumes() {
            return NOTHING;
        }

        @Override
        public void markOutgoing(Map<Integer, Instruction> context) {
            Instruction next = context.get(pc + 1 + pcoffset);
            if (next != null) next.inbound.add(pc);
        }
    }

    public static abstract class CompareInstr extends Instruction {
        public final int rk1;
        public final boolean isk1;
        public final int rk2;
        public final boolean isk2;
        public final boolean expected;

        private final Set<Integer> inputs = new HashSet<>();

        public CompareInstr(int pc, int line, int i) {
            super(pc, line);

            int rk1_actual = GETARG_B(i);
            isk1 = ISK(rk1_actual);
            rk1 = isk1 ? INDEXK(rk1_actual) : rk1_actual;
            if (!isk1) inputs.add(rk1);
            int rk2_actual = GETARG_C(i);
            isk2 = ISK(rk2_actual);
            rk2 = isk2 ? INDEXK(rk2_actual) : rk2_actual;
            if (!isk2) inputs.add(rk2);

            expected = GETARG_A(i) != 0;
        }

        @Override
        public Set<Integer> modifies() {
            return NOTHING;
        }

        @Override
        public Set<Integer> consumes() {
            return inputs;
        }

        @Override
        public void markOutgoing(Map<Integer, Instruction> context) {
            super.markOutgoing(context);
            // if branch taken
            Instruction alsoNext = context.get(pc + 2);
            if (alsoNext != null) alsoNext.inbound.add(pc);
        }
    }

    /// Check if [#rk1] is equal to [#rk2]. If the result matches [#expected], increment pc.
    public static class Eq extends CompareInstr {
        public Eq(int pc, int line, int i) {
            super(pc, line, i);
        }

        @Override
        public int getOpcode() {
            return OP_EQ;
        }
    }

    /// Check if [#rk1] is less than [#rk2]. If the result matches [#expected], increment pc.
    public static class Lt extends CompareInstr {
        public Lt(int pc, int line, int i) {
            super(pc, line, i);
        }

        @Override
        public int getOpcode() {
            return OP_LT;
        }
    }

    /// Check if [#rk1] is less than or equal to [#rk2]. If the result matches [#expected], increment pc.
    public static class Le extends CompareInstr {
        public Le(int pc, int line, int i) {
            super(pc, line, i);
        }

        @Override
        public int getOpcode() {
            return OP_LE;
        }
    }

    /// Check if [#source], converted to a boolean, matches [#expected]. If not, increment pc.
    public static class Test extends Instruction {
        public final int source;
        public final boolean expected;

        public Test(int pc, int line, int i) {
            super(pc, line);
            source = GETARG_A(i);
            expected = GETARG_C(i) != 0;
        }

        @Override
        public int getOpcode() {
            return OP_TEST;
        }

        @Override
        public void markOutgoing(Map<Integer, Instruction> context) {
            super.markOutgoing(context);
            // if branch taken
            Instruction alsoNext = context.get(pc + 2);
            if (alsoNext != null) alsoNext.inbound.add(pc);
        }

        @Override
        public Set<Integer> modifies() {
            return NOTHING;
        }

        @Override
        public Set<Integer> consumes() {
            return Set.of(source);
        }
    }

    /// Check if [#cmp], converted to a boolean, matches [#expected]. If so, set [#dest] to [#cmp]; if not, increment pc.
    public static class TestSet extends Instruction {
        public final int cmp;
        public final int dest;
        public final boolean expected;

        public TestSet(int pc, int line, int i) {
            super(pc, line);
            dest = GETARG_A(i);
            cmp = GETARG_B(i);
            expected = GETARG_C(i) != 0;
        }

        @Override
        public int getOpcode() {
            return OP_TESTSET;
        }

        @Override
        public void markOutgoing(Map<Integer, Instruction> context) {
            super.markOutgoing(context);
            // if branch taken
            Instruction alsoNext = context.get(pc + 2);
            if (alsoNext != null) alsoNext.inbound.add(pc);
        }

        @Override
        public Set<Integer> modifies() {
            return Set.of(dest);
        }

        @Override
        public Set<Integer> consumes() {
            return Set.of(cmp);
        }
    }

    public static abstract class CallInstr extends Instruction {
        public final int function;
        public final int argc;
        private final Set<Integer> reads = new HashSet<>();

        public CallInstr(int pc, int line, int i) {
            super(pc, line);
            function = GETARG_A(i);
            argc = GETARG_B(i) - 1;
            reads.add(function);
            if (argc > 0) {
                for (int a = function + 1; a <= function + argc; a++) {
                    reads.add(a);
                }
            }
        }

        @Override
        public Set<Integer> consumes() {
            return reads;
        }
    }

    public static class Call extends CallInstr {
        public final int retc;
        private final Set<Integer> writes = new HashSet<>();

        public Call(int pc, int line, int i) {
            super(pc, line, i);
            retc = GETARG_C(i) - 1;
            if (retc > 0) {
                for (int a = function; a <= function + argc - 1; a++) {
                    writes.add(a);
                }
            }
        }

        @Override
        public int getOpcode() {
            return OP_CALL;
        }

        @Override
        public Set<Integer> modifies() {
            return writes;
        }
    }

    public static class TailCall extends CallInstr {
        public TailCall(int pc, int line, int i) {
            super(pc, line, i);
        }

        @Override
        public int getOpcode() {
            return OP_TAILCALL;
        }

        @Override
        public Set<Integer> modifies() {
            return NOTHING; /* we can only return after this, so no need... */
        }
    }

    public static class Return extends Instruction {
        public final int start;
        public final int size;
        public final boolean isVar;
        // may not be accurate on vararg returns
        private final Set<Integer> inputs = new HashSet<>();

        public Return(int pc, int line, int i) {
            super(pc, line);
            start = GETARG_A(i);
            int b = GETARG_B(i);
            isVar = b == 0;
            if (b <= 1) {
                size = 0;
            } else {
                size = b - 1;
                for (int a = start; a < start + size; a++) {
                    inputs.add(a);
                }
            }
        }

        @Override
        public int getOpcode() {
            return OP_RETURN;
        }

        @Override
        public Set<Integer> modifies() {
            return Set.of();
        }

        @Override
        public Set<Integer> consumes() {
            return inputs;
        }

        @Override
        public void markOutgoing(Map<Integer, Instruction> context) {
            /* no next instruction */
        }
    }

    public static class TForLoop extends Instruction {
        public final int output;
        public final int input;
        public final int pcoffset;

        public TForLoop(int pc, int line, int i) {
            super(pc, line);
            output = GETARG_A(i);
            input = output + 1;
            pcoffset = GETARG_sBx(i);
        }

        @Override
        public int getOpcode() {
            return OP_TFORLOOP;
        }

        @Override
        public Set<Integer> modifies() {
            return Set.of(output);
        }

        @Override
        public Set<Integer> consumes() {
            return Set.of(input);
        }

        @Override
        public void markOutgoing(Map<Integer, Instruction> context) {
            super.markOutgoing(context);
            Instruction alsoNext = context.get(pc + 1 + pcoffset);
            if (alsoNext != null) alsoNext.inbound.add(pc);
        }
    }

    /// Provides extra space for the people who need more than 262,000 constants for some reason
    public static class ExtraArg extends Instruction {
        public final int k;

        public ExtraArg(int pc, int line, int i) {
            super(pc, line);
            k = GETARG_Ax(i);
        }

        @Override
        public int getOpcode() {
            return OP_EXTRAARG;
        }

        @Override
        public Set<Integer> modifies() {
            return NOTHING;
        }

        @Override
        public Set<Integer> consumes() {
            return NOTHING;
        }

        @Override
        public void markOutgoing(Map<Integer, Instruction> context) {
            /* not executable code */
        }
    }

    public static class Unknown extends Instruction {
        public Unknown(int pc, int line) {
            super(pc, line);
        }

        @Override
        public int getOpcode() {
            return -1;
        }

        @Override
        public Set<Integer> modifies() {
            return Set.of();
        }

        @Override
        public Set<Integer> consumes() {
            return Set.of();
        }

        @Override
        public String toString() {
            return "<UNKNOWN " + pc + ">";
        }
    }

    public static Instruction of(int pc, int line, int i) {
        switch (GET_OPCODE(i)) {
            case OP_MOVE:
                return new Move(pc, line, i);
            case OP_LOADK:
                return new LoadK(pc, line, i);
            case OP_LOADKX:
                return new LoadKX(pc, line, i);
            case OP_LOADBOOL:
                return new LoadBool(pc, line, i);
            case OP_LOADNIL:
                return new LoadNil(pc, line, i);
            case OP_GETUPVAL:
                return new GetUpVal(pc, line, i);
            case OP_GETTABUP:
                return new GetTabUp(pc, line, i);
            case OP_GETTABLE:
                return new GetTable(pc, line, i);
            case OP_SELF:
                return new Self(pc, line, i);
            case OP_JMP:
                return new Jmp(pc, line, i);
            case OP_EQ:
                return new Eq(pc, line, i);
            case OP_LT:
                return new Lt(pc, line, i);
            case OP_LE:
                return new Le(pc, line, i);
            case OP_TEST:
                return new Test(pc, line, i);
            case OP_TESTSET:
                return new TestSet(pc, line, i);
            case OP_CALL:
                return new Call(pc, line, i);
            case OP_TAILCALL:
                return new TailCall(pc, line, i);
            case OP_RETURN:
                return new Return(pc, line, i);
            case OP_TFORLOOP:
                return new TForLoop(pc, line, i);
            case OP_EXTRAARG:
                return new ExtraArg(pc, line, i);
        }
        return new Unknown(pc, line);
    }

    public Instruction(int pc, int line) {
        this.pc = pc;
        this.line = line;
    }

    public static Map<Integer, Instruction> scanProto(Prototype p) {
        Map<Integer, Instruction> instrMap = new HashMap<>();
        int pc = 0;
        for (int op : p.code) {
            instrMap.put(pc, Instruction.of(pc, p.lineinfo[pc], op));
            pc++;
        }
        for (Instruction instr : instrMap.values()) {
            instr.markOutgoing(instrMap);
        }
        return instrMap;
    }
}
