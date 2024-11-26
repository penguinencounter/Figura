package org.figuramc.figura.lua;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Print;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.Upvaldesc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.luaj.vm2.Lua.*;

public class LuaTrace {
    public static final Logger LOGGER = LoggerFactory.getLogger("Figura Dataflow Analysis");

    public static final class OpNode {
        public final int lineinfo;
        public final int pc;
        public final int opcode;
        public final @Nullable String sourceFile;
        public final int instruction;
        public final ArrayList<Integer> outbounds = new ArrayList<>();
        public final ArrayList<Integer> inbounds = new ArrayList<>();
        public final HashSet<Integer> slotWrites = new HashSet<>();

        @Override
        public String toString() {
            return renderInstruction(instruction, null);
        }

        public OpNode(int pc, int instruction, int lineinfo, @Nullable String sourceFile) {
            this.pc = pc;
            this.instruction = instruction;
            this.lineinfo = lineinfo;
            this.sourceFile = sourceFile;
            int opcode = GET_OPCODE(instruction);
            this.opcode = opcode;

            // Branching
            switch (opcode) {
                case OP_LOADBOOL -> {
                    // constant jump based on C (static analysis)
                    if (GETARG_C(instruction) != 0) {
                        outbounds.add(pc + 2);
                    } else {
                        outbounds.add(pc + 1);
                    }
                }
                case OP_LOADKX -> outbounds.add(pc + 2); // Skip the ExtraArg...
                case OP_JMP, OP_FORPREP -> outbounds.add(pc + 1 + GETARG_sBx(instruction));
                case OP_EQ, OP_LT, OP_LE, OP_TEST, OP_TESTSET -> {
                    // values unavailable in this analysis, so we assume both branches are taken
                    outbounds.add(pc + 1);
                    outbounds.add(pc + 2);
                }
                case OP_FORLOOP, OP_TFORLOOP -> {
                    // either we are done or we loop
                    outbounds.add(pc + 1); // loop
                    outbounds.add(pc + 1 + GETARG_sBx(instruction)); // exit
                }
                default -> outbounds.add(pc + 1);
            }

            // Slots
            switch (opcode) {
                case OP_MOVE, OP_LOADK, OP_LOADKX, OP_LOADBOOL, OP_GETUPVAL, OP_GETTABUP, OP_GETTABLE, OP_SETTABLE,
                     OP_NEWTABLE, OP_ADD, OP_SUB, OP_MUL, OP_DIV, OP_MOD, OP_POW, OP_UNM, OP_NOT, OP_LEN, OP_CONCAT,
                     OP_TESTSET, OP_FORPREP, OP_TFORLOOP, OP_SETLIST, OP_CLOSURE ->
                        slotWrites.add(GETARG_A(instruction));
                case OP_LOADNIL -> {
                    for (int i = GETARG_A(instruction); i <= GETARG_A(instruction) + GETARG_B(instruction); i++) {
                        slotWrites.add(i);
                    }
                }
                case OP_SELF -> {
                    slotWrites.add(GETARG_A(instruction));
                    slotWrites.add(GETARG_A(instruction) + 1);
                }
                case OP_CALL -> {
                    if (GETARG_C(instruction) > 1) {
                        for (int i = GETARG_A(instruction); i <= GETARG_A(instruction) + GETARG_C(instruction) - 2; i++) {
                            slotWrites.add(i);
                        }
                    }
                }
                case OP_FORLOOP -> {
                    slotWrites.add(GETARG_A(instruction));
                    slotWrites.add(GETARG_A(instruction) + 3);
                }
                case OP_TFORCALL -> {
                    for (int i = GETARG_A(instruction) + 3; i <= GETARG_A(instruction) + 2 + GETARG_C(instruction); i++) {
                        slotWrites.add(i);
                    }
                }
                case OP_VARARG -> {
                    for (int i = GETARG_A(instruction); i <= GETARG_A(instruction) + GETARG_B(instruction) - 1; i++) {
                        slotWrites.add(i);
                    }
                }
            }
        }
    }

    private static String renderInstruction(int unit, @Nullable LuaValue[] kt) {
        int op = GET_OPCODE(unit);
        int readMode = getOpMode(op);
        String opcode;
        if (op >= Print.OPNAMES.length - 1) {
            opcode = op + "?";
        } else {
            opcode = Print.OPNAMES[op];
        }

        int a = GETARG_A(unit);
        int b = GETARG_B(unit);
        int c = GETARG_C(unit);
        int bx = GETARG_Bx(unit);
        int sbx = GETARG_sBx(unit);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s ", opcode));
        switch (readMode) {
            case iABC -> {
                sb.append(a);
                if (getBMode(op) != OpArgN) {
                    sb.append(" ").append(ISK(b) ? -1 - INDEXK(b) : b);
                    if (ISK(b) && kt != null) {
                        sb.append(" (").append(kt[INDEXK(b)]).append(")");
                    }
                }
                if (getCMode(op) != OpArgN) {
                    sb.append(" ").append(ISK(c) ? -1 - INDEXK(c) : c);
                    if (ISK(c) && kt != null) {
                        sb.append(" (").append(kt[INDEXK(c)]).append(")");
                    }
                }
            }
            case iABx -> {
                sb.append(a);
                if (getBMode(op) == OpArgK) {
                    sb.append(" ").append(-1 - bx);
                    if (kt != null) sb.append(" (").append(kt[bx]).append(")");
                } else {
                    sb.append(" ").append(bx);
                }
            }
            case iAsBx -> {
                if (op == OP_JMP) {
                    sb.append(sbx);
                } else {
                    sb.append(a).append(" ").append(sbx);
                }
            }
        }
        return sb.toString();
    }

    public static ArrayList<OpNode> traceInstructions(Prototype p) {
        ArrayList<OpNode> nodes = new ArrayList<>();
        int[] code = p.code;
        int[] lines = p.lineinfo;
        for (int i = 0; i < code.length; i++) {
            nodes.add(new OpNode(i, code[i], lines[i], p.source.tojstring()));
        }
        nodes.get(0).inbounds.add(-1); // entrypoint

        Queue<OpNode> frontier = new LinkedList<>();
        HashSet<Integer> visited = new HashSet<>();
        frontier.add(nodes.get(0));

        while (!frontier.isEmpty()) {
            OpNode node = frontier.poll();
            if (visited.contains(node.pc)) {
                continue;
            }
            for (int out : node.outbounds) {
                if (nodes.size() > out) {
                    OpNode n = nodes.get(out);
                    n.inbounds.add(node.pc);
                    frontier.add(n);
                }
            }
            visited.add(node.pc);
        }
        return nodes;
    }

    @SafeVarargs
    private static <T> ArrayList<T> arraylist(T... values) {
        return new ArrayList<>(List.of(values));
    }

    private static TimelineItem registerOrConst(List<OpNode> map, OpNode from, int rkVal) {
        if (ISK(rkVal)) {
            return new Constant(from, INDEXK(rkVal));
        }
        TreeMap<Integer, SlotAlias> hist = slotAliasTo(map, from, rkVal, new HashSet<>());
        return new MultiSlot(hist);
    }

    private static final HashMap<Integer, String> OP_SYMBOLS = new HashMap<>();

    static {
        OP_SYMBOLS.put(OP_ADD, "+");
        OP_SYMBOLS.put(OP_SUB, "-");
        OP_SYMBOLS.put(OP_MUL, "*");
        OP_SYMBOLS.put(OP_DIV, "/");
        OP_SYMBOLS.put(OP_MOD, "%");
        OP_SYMBOLS.put(OP_POW, "^");
        OP_SYMBOLS.put(OP_UNM, "-");
        OP_SYMBOLS.put(OP_NOT, "not");
        OP_SYMBOLS.put(OP_LEN, "#");
    }

    public static final class Timeline extends ArrayList<TimelineItem> {
        public Timeline(int initialCapacity) {
            super(initialCapacity);
        }

        public Timeline() {
            super();
        }

        public Timeline(@NotNull Collection<? extends TimelineItem> c) {
            super(c);
        }

        public void setTraceTarget(int box) {
            this.box = box;
        }

        private int box = -1;

        public int getTraceTarget() {
            return box;
        }

        public static Timeline of(TimelineItem... logs) {
            return new Timeline(List.of(logs));
        }

        public static Timeline of(int box, TimelineItem... logs) {
            Timeline timeline = new Timeline(List.of(logs));
            timeline.setTraceTarget(box);
            return timeline;
        }
    }

    public static abstract class TimelineItem {
        public static final TimelineItem PRUNE = new TimelineItem(null) {
            @Override
            String resolve(LuaValue[] ksts, Upvaldesc[] upvalues) {
                return "! marker only !";
            }

            @Override
            public <T> T accept(TimelineItemVisitor<T> visitor) {
                return null;
            }
        };
        public final @Nullable OpNode source;

        public TimelineItem() {
            this(null);
        }

        public TimelineItem(@Nullable OpNode from) {
            this.source = from;
        }

        abstract String resolve(LuaValue[] ksts, Upvaldesc[] upvalues);

        @Deprecated
        public TimelineItem concat(TimelineItem after) {
            return new Concatenation(this, after);
        }

        @Deprecated
        public TimelineItem concat(TimelineItem before, TimelineItem after) {
            return new Concatenation(before, this).concat(after);
        }

        @Deprecated
        public TimelineItem prefix(String message) {
            return new Concatenation(Message.of(message), this);
        }

        @Deprecated
        public TimelineItem suffix(String message) {
            return new Concatenation(this, Message.of(message));
        }

        public <T> T accept(TimelineItemVisitor<T> visitor) {
            LOGGER.warn("TimelineItem doesn't provide a specific implementation for visitor: {}",
                    this.getClass().getSimpleName()
            );
            return visitor.visitGeneric(this);
        }
    }

    public interface TimelineItemVisitor<T> {
        T visitGeneric(TimelineItem item);

        T visit(Concatenation item);

        T visit(Message item);

        T visit(UnaryOperator item);

        T visit(Constant item);

        T visit(Upvalue item);

        T visit(Literal item);

        T visit(Slot item);

        T visit(Index item);

        T visit(InnerResolve item);

        T visit(BinaryOperator item);

        T visit(CallResult item);

        T visit(Closure item);

        T visit(MultiSlot multiSlot);
    }

    @Deprecated
    public static class Concatenation extends TimelineItem {
        public final TimelineItem before;
        public final TimelineItem after;

        public Concatenation(TimelineItem before, TimelineItem after) {
            this(null, before, after);
        }

        public Concatenation(OpNode from, TimelineItem before, TimelineItem after) {
            super(from);
            this.before = before;
            this.after = after;
        }

        @Override
        String resolve(LuaValue[] ksts, Upvaldesc[] upvalues) {
            return before.resolve(ksts, upvalues) + after.resolve(ksts, upvalues);
        }

        @Override
        public <T> T accept(TimelineItemVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Message extends TimelineItem {
        public final String message;

        public Message(String message) {
            this(null, message);
        }

        public Message(OpNode from, String message) {
            super(from);
            this.message = message;
        }

        @Override
        public String resolve(LuaValue[] ksts, Upvaldesc[] upvalues) {
            return message;
        }

        public static Message of(String message) {
            return new Message(message);
        }

        @Override
        public <T> T accept(TimelineItemVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class UnaryOperator extends Message {

        public UnaryOperator(String message) {
            this(null, message);
        }

        public UnaryOperator(OpNode from, String message) {
            super(from, message);
        }

        public static UnaryOperator of(String message) {
            return new UnaryOperator(message);
        }

        @Override
        public <T> T accept(TimelineItemVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Constant extends TimelineItem {
        public final int index;

        public Constant(OpNode from, int index) {
            super(from);
            this.index = index;
        }

        @Override
        public String resolve(LuaValue[] ksts, Upvaldesc[] upvalues) {
            LuaValue kst = ksts[index];
            switch (kst.type()) {
                case LuaValue.TNIL -> {
                    return "nil";
                }
                case LuaValue.TBOOLEAN -> {
                    return kst.toboolean() ? "true" : "false";
                }
                case LuaValue.TNUMBER -> {
                    return kst.tojstring();
                }
                case LuaValue.TSTRING -> {
                    return String.format("\"%s\"", kst.tojstring());
                }
                default -> {
                    return "(" + kst.tojstring() + ")";
                }
            }
        }

        @Override
        public <T> T accept(TimelineItemVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Upvalue extends TimelineItem {
        public final int index;

        public Upvalue(OpNode from, int index) {
            super(from);
            this.index = index;
        }

        @Override
        public String resolve(LuaValue[] ksts, Upvaldesc[] upvalues) {
            return String.format("^%s", upvalues[index].name);
        }

        @Override
        public <T> T accept(TimelineItemVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Literal extends TimelineItem {
        public enum LiteralType {
            NIL, TRUE, FALSE, TABLE
        }

        public final @NotNull LiteralType v;
        public final int slot;

        public Literal(OpNode from, @NotNull LiteralType v, int slot) {
            super(from);
            this.v = v;
            this.slot = slot;
        }

        public static Literal TRUE(OpNode from, int slot) {
            return new Literal(from, LiteralType.TRUE, slot);
        }

        public static Literal FALSE(OpNode from, int slot) {
            return new Literal(from, LiteralType.FALSE, slot);
        }

        public static Literal NIL(OpNode from, int slot) {
            return new Literal(from, LiteralType.NIL, slot);
        }

        public static Literal TABLE(OpNode from, int slot) {
            return new Literal(from, LiteralType.TABLE, slot);
        }

        public LiteralType get() {
            return v;
        }

        @Override
        String resolve(LuaValue[] ksts, Upvaldesc[] upvalues) {
            return switch (v) {
                case NIL -> "nil";
                case TRUE -> "true";
                case FALSE -> "false";
                case TABLE -> "{ ... }";
            };
        }

        @Override
        public <T> T accept(TimelineItemVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Slot extends TimelineItem {
        public final int index;

        public Slot(OpNode from, int index) {
            super(from);
            this.index = index;
        }

        @Override
        String resolve(LuaValue[] ksts, Upvaldesc[] upvalues) {
            return "{" + index + "}";
        }

        @Override
        public <T> T accept(TimelineItemVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MultiSlot extends TimelineItem {
        public final TreeMap<Integer, SlotAlias> slots;

        public MultiSlot(TreeMap<Integer, SlotAlias> slots) {
            super(null);
            this.slots = slots;
        }

        @Override
        String resolve(LuaValue[] ksts, Upvaldesc[] upvalues) {
            return "*deprecated*";
        }

        @Override
        public <T> T accept(TimelineItemVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Index extends TimelineItem {
        public final TimelineItem indexed;

        public Index(OpNode from, TimelineItem indexee) {
            super(from);
            this.indexed = indexee;
        }

        @Override
        String resolve(LuaValue[] ksts, Upvaldesc[] upvalues) {
            return "[" + indexed.resolve(ksts, upvalues) + "]";
        }

        @Override
        public <T> T accept(TimelineItemVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class InnerResolve extends TimelineItem {

        public final ArrayList<Timeline> sources;

        public InnerResolve(ArrayList<Timeline> sources) {
            this.sources = sources;
        }

        @Override
        String resolve(LuaValue[] ksts, Upvaldesc[] upvalues) {
            return "[inner resolve: " + sources.size() + " timelines / " + sources.stream().mapToInt(Timeline::size)
                    .sum() + " items]";
        }

        @Override
        public <T> T accept(TimelineItemVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class BinaryOperator extends TimelineItem {
        public final String operator;
        public final TimelineItem left;
        public final TimelineItem right;

        public BinaryOperator(OpNode from, String operator, TimelineItem left, TimelineItem right) {
            super(from);
            this.operator = operator;
            this.left = left;
            this.right = right;
        }

        @Override
        String resolve(LuaValue[] ksts, Upvaldesc[] upvalues) {
            return left.resolve(ksts, upvalues) + " " + operator + " " + right.resolve(ksts, upvalues);
        }

        @Override
        public <T> T accept(TimelineItemVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class CallResult extends TimelineItem {
        public final TimelineItem funcDescr;
        public final int returnId;

        public CallResult(int returnId, TimelineItem funcDescr) {
            this(null, returnId, funcDescr);
        }

        public CallResult(OpNode from, int returnId, TimelineItem funcDescr) {
            super(from);
            this.returnId = returnId;
            this.funcDescr = funcDescr;
        }

        @Override
        String resolve(LuaValue[] ksts, Upvaldesc[] upvalues) {
            return "[" + funcDescr.resolve(ksts, upvalues) + "(...) result #" + returnId + "]";
        }

        @Override
        public <T> T accept(TimelineItemVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Closure extends TimelineItem {
        public final int kpi;

        public Closure(OpNode from, int kpi) {
            super(from);
            this.kpi = kpi;
        }

        @Override
        String resolve(LuaValue[] ksts, Upvaldesc[] upvalues) {
            return "closure #" + kpi;
        }

        @Override
        public <T> T accept(TimelineItemVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public record SlotAlias(int slot, int pcAt) {
    }

    public static TreeMap<Integer, SlotAlias> slotAliasTo(List<OpNode> map,
                                                          OpNode endpoint,
                                                          int slotTarget,
                                                          HashSet<Integer> visitedPCs) {
        if (visitedPCs.contains(endpoint.pc)) {
            return new TreeMap<>();
        }
        TreeMap<Integer, SlotAlias> aliases = new TreeMap<>();

        int inboundSlot = slotTarget;

        if (endpoint.slotWrites.contains(slotTarget)) {
            if (endpoint.opcode == OP_MOVE) {
                aliases.put(endpoint.pc, new SlotAlias(GETARG_A(endpoint.instruction), endpoint.pc + 1));
                aliases.put(endpoint.pc, new SlotAlias(GETARG_B(endpoint.instruction), endpoint.pc));
                inboundSlot = GETARG_B(endpoint.instruction);
            }
        }
        List<Integer> actionable = endpoint.inbounds.stream().filter(x -> !(x < 0 || x > map.size())).toList();
        if (actionable.size() == 1) {
            HashSet<Integer> visitedClone = new HashSet<>(visitedPCs);
            visitedClone.add(endpoint.pc);
            aliases.putAll(slotAliasTo(map, map.get(actionable.get(0)), inboundSlot, visitedClone));
        } else {
            aliases.put(endpoint.pc, new SlotAlias(inboundSlot, endpoint.pc));
        }
        return aliases;
    }

    public static ArrayList<Timeline> dataFlowTo(List<OpNode> map,
                                                 OpNode endpoint,
                                                 @Nullable OpNode cameFrom,
                                                 int box) {
        return dataFlowTo(map, endpoint, cameFrom, box, new ArrayList<>());
    }

    public static ArrayList<Timeline> dataFlowTo(List<OpNode> map,
                                                 OpNode endpoint,
                                                 @Nullable OpNode cameFrom,
                                                 int box,
                                                 ArrayList<Integer> visited) {
        if (visited.contains(endpoint.pc)) {
            return arraylist(Timeline.of(TimelineItem.PRUNE));
        }
        ArrayList<Timeline> timelines = new ArrayList<>();
        ArrayList<Integer> clobberedSlots = new ArrayList<>();
        // each Timeline goes from first event -> last event (chronological); when traversing backwards, insert all items
        // at the front of the list
        if (endpoint.slotWrites.contains(box)) {
            switch (endpoint.opcode) {
                case OP_MOVE -> timelines.add(Timeline.of(GETARG_B(endpoint.instruction)));
                case OP_LOADK -> {
                    return arraylist(Timeline.of(new Constant(endpoint, GETARG_Bx(endpoint.instruction))));
                }
                case OP_LOADKX -> {
                    OpNode extra = map.get(endpoint.pc + 1);
                    return arraylist(Timeline.of(new Constant(endpoint, GETARG_Ax(extra.instruction))));
                }
                case OP_LOADBOOL -> {
                    Literal bool = GETARG_B(endpoint.instruction) != 0 ? Literal.TRUE(endpoint, box) : Literal.FALSE(endpoint,
                            box
                    );
                    return arraylist(Timeline.of(bool));
                }
                case OP_LOADNIL -> {
                    // we checked slotWrites already, so we don't have to do that again
                    return arraylist(Timeline.of(Literal.NIL(endpoint, box)));
                }
                case OP_GETUPVAL -> {
                    return arraylist(Timeline.of(new Upvalue(endpoint, GETARG_B(endpoint.instruction))));
                }
                case OP_GETTABUP -> {
                    return arraylist(Timeline.of(new Upvalue(endpoint, GETARG_B(endpoint.instruction)),
                            new Index(endpoint, registerOrConst(map, endpoint, GETARG_C(endpoint.instruction)))
                    ));
                }
                case OP_GETTABLE -> timelines.add(Timeline.of(GETARG_B(endpoint.instruction),
                        new Index(endpoint, registerOrConst(map, endpoint, GETARG_C(endpoint.instruction)))
                ));
                case OP_NEWTABLE -> {
                    return arraylist(Timeline.of(Literal.TABLE(endpoint, box)));
                }
                case OP_SELF -> {
                    // Not sure how to handle this with the current implementation.
                    return arraylist(Timeline.of(Message.of("unhandled SELF-instr")));
                }
                case OP_ADD, OP_SUB, OP_MUL, OP_DIV, OP_MOD, OP_POW -> {
                    return arraylist(Timeline.of(registerOrConst(map,
                            endpoint,
                            GETARG_B(endpoint.instruction)
                    ).suffix(" " + OP_SYMBOLS.get(endpoint.opcode) + " ")
                            .concat(registerOrConst(map, endpoint, GETARG_C(endpoint.instruction)))));
                }
                case OP_UNM, OP_NOT, OP_LEN -> timelines.add(Timeline.of(GETARG_B(endpoint.instruction),
                        new UnaryOperator(endpoint, OP_SYMBOLS.get(endpoint.opcode))
                ));
                case OP_CONCAT -> {
                    // we need to follow the chain of concatenations
                    int B = GETARG_B(endpoint.instruction);
                    int C = GETARG_C(endpoint.instruction);
                    if (C - B > 1) {
                        return arraylist(Timeline.of(Message.of("<concatenation of " + (C - B) + " values>")));
                    }

                    TreeMap<Integer, SlotAlias> leftSrc = slotAliasTo(map, endpoint, B, new HashSet<>());
                    leftSrc.put(endpoint.pc, new SlotAlias(B, endpoint.pc));
                    TreeMap<Integer, SlotAlias> rightSrc = slotAliasTo(map, endpoint, C, new HashSet<>());
                    rightSrc.put(endpoint.pc, new SlotAlias(C, endpoint.pc));
                    return arraylist(Timeline.of(new BinaryOperator(endpoint,
                            "..",
                            new MultiSlot(leftSrc),
                            new MultiSlot(rightSrc)
                    )));
                }
                case OP_TESTSET -> {
                    // our first branching instruction!
                    String toMatch = GETARG_C(endpoint.instruction) != 0 ? "true" : "false";
                    String toNotMatch = GETARG_C(endpoint.instruction) != 0 ? "false" : "true";
                    if (cameFrom == null) {
                        // No result information
                        timelines.add(Timeline.of(GETARG_B(endpoint.instruction),
                                new Slot(endpoint, GETARG_A(endpoint.instruction)).prefix("⇆ ")
                                        .suffix(" is " + toNotMatch)
                        ));
                        timelines.add(Timeline.of(box,
                                new Slot(endpoint, GETARG_A(endpoint.instruction)).prefix("⇆ ").suffix(" is " + toMatch)
                        ));
                    } else {
                        // We know where we came from...
                        if (cameFrom.pc == endpoint.pc + 2) { // the expression evaluated to FALSE (i.e. pc++ was taken)
                            timelines.add(Timeline.of(box,
                                    new Slot(endpoint, GETARG_A(endpoint.instruction)).prefix("⇉ ")
                                            .suffix(" is " + toMatch)
                            ));
                        } else { // the expression evaluated to TRUE
                            timelines.add(Timeline.of(GETARG_B(endpoint.instruction),
                                    new Slot(endpoint, GETARG_A(endpoint.instruction)).prefix("⇉ ")
                                            .suffix(" is " + toNotMatch)
                            ));
                        }
                    }
                }
                case OP_CALL -> {
                    ArrayList<Timeline> functionSource = dataFlowTo(map,
                            map.get(endpoint.pc - 1),
                            endpoint,
                            GETARG_A(endpoint.instruction)
                    );
                    int argn = box - GETARG_A(endpoint.instruction) + 1;
                    return arraylist(Timeline.of(new CallResult(argn, new InnerResolve(functionSource))));
                }
                case OP_FORLOOP -> {
                    return arraylist(Timeline.of(Message.of("unhandled FORLOOP-instr")));
                }
                case OP_FORPREP -> timelines.add(Timeline.of(box,
                        new Slot(endpoint, GETARG_A(endpoint.instruction) + 2).prefix("-= ").suffix(" (forprep rewind)")
                ));
                case OP_TFORCALL -> {
                    return arraylist(Timeline.of(Message.of("[??? from function in slot " + GETARG_A(endpoint.instruction) + " (iterator)]")));
                }
                case OP_TFORLOOP -> {
                    return arraylist(Timeline.of(Message.of("unhandled TFORLOOP-instr")));
                }
                case OP_CLOSURE -> {
                    return arraylist(Timeline.of(new Closure(endpoint, GETARG_Bx(endpoint.instruction))));
                }
                case OP_VARARG -> {
                    return arraylist(Timeline.of(Message.of("...vararg assignment")));
                }
                default -> {
                    return arraylist(Timeline.of(Message.of("unhandled instr " + endpoint.opcode)));
                }
            }
        }
        if (endpoint.pc == 0) {
            if (timelines.isEmpty()) {
                timelines.add(new Timeline());
            }
            for (Timeline t : timelines) {
                TreeMap<Integer, SlotAlias> slotInfo = slotAliasTo(map, endpoint, box, new HashSet<>());
                t.add(0, new MultiSlot(slotInfo).prefix("param::"));
            }
        }

        visited = new ArrayList<>(visited);
        visited.add(endpoint.pc);
        ArrayList<Timeline> extendedTimelines = new ArrayList<>();
        List<Integer> actionable = endpoint.inbounds.stream().filter(x -> !(x < 0 || x > map.size())).toList();
        if (actionable.isEmpty()) {
            timelines = new ArrayList<>(timelines.stream().filter(x -> !x.contains(TimelineItem.PRUNE)).toList());
            return timelines;
        }
        for (int parent : actionable) {
            if (parent < 0 || parent > map.size()) continue; // -1 = Entrypoint
            OpNode previous = map.get(parent);
            if (timelines.isEmpty()) {
                extendedTimelines.addAll(dataFlowTo(map, previous, endpoint, box, visited));
            }
            for (Timeline t : timelines) {
                if (t.box == -1) {
                    extendedTimelines.add(t);
                    continue;
                }
                ArrayList<Timeline> sub = dataFlowTo(map, previous, endpoint, t.box, visited);
                for (Timeline subTimeline : sub) {
                    Timeline newTimeline = new Timeline(subTimeline);
                    newTimeline.addAll(t);
                    extendedTimelines.add(newTimeline);
                }
            }
        }
        extendedTimelines = new ArrayList<>(extendedTimelines.stream().filter(x -> !x.contains(TimelineItem.PRUNE))
                .toList());
        return extendedTimelines;
    }
}
