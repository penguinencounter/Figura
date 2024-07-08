package org.figuramc.figura.lua;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.luaj.vm2.*;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static net.minecraft.network.chat.Component.empty;
import static net.minecraft.network.chat.Component.literal;

public class ErrorMessageHelper {

    private static final Style DEFAULT = Style.EMPTY.withColor(0xffffff).withItalic(false).withUnderlined(false)
            .withBold(false).withStrikethrough(false).withObfuscated(false).withClickEvent(null).withHoverEvent(null)
            .withInsertion(null).withFont(Style.DEFAULT_FONT);
    private static final Style ANNOTATE_SOURCE_ADDR = Style.EMPTY.withColor(0x777777).withItalic(false)
            .withUnderlined(true);
    private static final Style CALL_SOURCE_ADDR = Style.EMPTY.withColor(0xb26bab).withItalic(false);
    private static final Style MESSAGE = Style.EMPTY.withColor(0x78f2c6).withItalic(true);
    private static final Style NOTE = Style.EMPTY.withColor(0x655445).withItalic(true);
    private static final Style NOTE_TEMPSLOT = NOTE.withHoverEvent(new HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            literal("Couldn't detect a local variable in this slot.\n").append(literal(
                    "Note: this can happen even if the values are\n").withStyle(NOTE)
                    .append("actually local variables."))
    ));
    private static final Style LITERAL = Style.EMPTY.withColor(0xd55fde).withItalic(true);
    private static final Style CONST_NIL = LITERAL;
    private static final Style CONST_BOOLEAN = LITERAL;
    private static final Style CONST_NUMBER = Style.EMPTY.withColor(0xd19a66);
    private static final Style CONST_STRING = Style.EMPTY.withColor(0x98c379);
    private static final Style CONST_OTHER = Style.EMPTY.withColor(0x61afef).withUnderlined(true);
    private static final Style UPVALUE = Style.EMPTY.withColor(0xef596f).withItalic(true);
    private static final Style LOCALVAR = Style.EMPTY.withColor(0x82add3).withUnderlined(true);
    private static final Style SLOTVAR = Style.EMPTY.withColor(0x04bbdd);
    private static final Style LOCALVAR_EXPIRED = Style.EMPTY.withColor(0x5b7388).withStrikethrough(true);
    private static final Style RETURN_IDX = LOCALVAR;
    private static final Style INNER_SOLVE = Style.EMPTY.withColor(0x2f9f70).withItalic(true);
    private static final Style PROBLEM = Style.EMPTY.withColor(0xed5040).withBold(true);
    private ArrayList<Component> out_to;

    public static MutableComponent analyze(FiguraLuaRuntime runtime, FiguraLuaRuntime.ErrorFrame errorFrame) {
        ErrorMessageHelper helper = new ErrorMessageHelper();
        ArrayList<Component> out = new ArrayList<>();
        helper.configureOutput(out);
        MutableComponent result = empty();

        String message = errorFrame.message();
        if (message == null) return null;
        if (errorFrame.p() == null) return null;

        List<LuaTrace.OpNode> graph = LuaTrace.traceInstructions(errorFrame.p());
        LuaTrace.OpNode errorAt = graph.get(errorFrame.frame().get_pc());
        if (message.contains("attempt to index ? (a nil value)")) {
            helper.attemptIndexNonIndexable("nil", graph, errorAt, errorFrame.p(), !runtime.owner.minify);
        } else if (message.contains("attempt to index ? (a number value)")) {
            helper.attemptIndexNonIndexable("number", graph, errorAt, errorFrame.p(), !runtime.owner.minify);
        } else if (message.contains("attempt to index ? (a boolean value)")) {
            helper.attemptIndexNonIndexable("boolean", graph, errorAt, errorFrame.p(), !runtime.owner.minify);
        } else if (message.contains("attempt to index ? (a function value)")) {
            helper.attemptIndexNonIndexable("function", graph, errorAt, errorFrame.p(), !runtime.owner.minify);
        }

        if (!out.isEmpty()) {
            int n = out.size();
            for (Component component : out) {
                result.append(component);
                if (--n > 0) result.append("\n");
            }
        }
        return result;
    }

    private static MutableComponent combine(MutableComponent... components) {
        MutableComponent result = empty();
        for (MutableComponent component : components) {
            result.append(component);
        }
        return result;
    }

    public void configureOutput(ArrayList<Component> to) {
        out_to = to;
    }

    public void attemptIndexNonIndexable(String blame,
                                         List<LuaTrace.OpNode> graph,
                                         LuaTrace.OpNode errorAt,
                                         Prototype function,
                                         boolean doLinesMatch) {
        TopLevelMultilineVisitor lv = new TopLevelMultilineVisitor(function, doLinesMatch);

        if (out_to == null) throw new IllegalStateException("Output not configured");
        out_to.add(literal("The following values may be " + blame + ":").withStyle(MESSAGE));
        if (!doLinesMatch) out_to.add(literal("(line numbers unavailable due to minification)").withStyle(MESSAGE));

        if (errorAt.opcode == Lua.OP_GETTABUP) {
            LuaString name = function.upvalues[Lua.GETARG_B(errorAt.instruction)].name;
            out_to.add(literal(name.tojstring()).withStyle(UPVALUE));
            LuaTrace.LOGGER.info("Attempt to index " + blame + " value in upvalue {}", name);
        } else if (errorAt.opcode == Lua.OP_GETTABLE) {
            int blameSlot = Lua.GETARG_A(errorAt.instruction); // This will be redirected to slot B in the dataflow analysis
            ArrayList<LuaTrace.Timeline> multiverse = LuaTrace.dataFlowTo(graph, errorAt, null, blameSlot);
            if (!multiverse.isEmpty()) {
                for (LuaTrace.Timeline timeline : multiverse) {
                    if (multiverse.size() > 1) {
                        out_to.add(literal("Option #" + (multiverse.indexOf(timeline) + 1)).withStyle(MESSAGE));
                    }
                    ArrayList<Integer> indexStack = new ArrayList<>();
                    int i = 0;
                    for (LuaTrace.TimelineItem item : timeline) {
                        if (item instanceof LuaTrace.Index) {
                            indexStack.add(i);
                        }
                        i++;
                    }

                    if (!indexStack.isEmpty()) {
                        int lastLI = -2;
                        MutableComponent collector = empty();
                        List<LuaTrace.TimelineItem> relevant = timeline.subList(
                                0,
                                indexStack.get(indexStack.size() - 1)
                        );
                        LuaTrace.TimelineItem lastItem = null;
                        int indent = -2;
                        final int INDENT_PER_LEVEL = 4;

                        for (LuaTrace.TimelineItem item : relevant) {
                            int thisLI = -1;
                            if (item.source != null) thisLI = item.source.lineinfo;
                            if (thisLI != lastLI) {
                                lastLI = thisLI;
                                if (!collector.getSiblings().isEmpty() && lastItem != null) {
                                    lv.attachLineInfo(lastItem, collector);
                                    out_to.add(collector);
                                    indent += INDENT_PER_LEVEL;
                                    collector = empty().append(literal(" ".repeat(max(indent, 0)) + "┗━ "));
                                }
                            }
                            lastItem = item;
                            collector.append(item.accept(lv));
                        }
                        if (!collector.getSiblings().isEmpty()) {
//                            assert lastItem != null;
                            collector.append(literal("  ")).append(literal(blame + "?").withStyle(PROBLEM));
                            lv.attachLineInfo(lastItem, collector);
                            out_to.add(collector);
                        }
                    } else {
                        throw new IllegalStateException("uhh what is this again?");
                    }
                }
            } else {
                out_to.add(literal("⇒ Analysis failed: Could not determine source").withStyle(MESSAGE));
            }
        } else {
            out_to.add(literal("⇒ Analysis failed: Unexpected error location").withStyle(MESSAGE));
        }
    }

    private static final class TopLevelMultilineVisitor implements LuaTrace.TimelineItemVisitor<MutableComponent> {
        private final Prototype proto;
        private final LuaValue[] kst;
        private final Upvaldesc[] up;
        private final Prototype[] innerP;
        private final boolean doLinesMatch;

        public TopLevelMultilineVisitor(Prototype proto, boolean doLinesMatch) {
            this.proto = proto;
            this.kst = proto.k;
            this.up = proto.upvalues;
            this.innerP = proto.p;
            this.doLinesMatch = doLinesMatch;
        }

        public void attachLineInfo(LuaTrace.TimelineItem item, MutableComponent component) {
            MutableComponent prefix = empty();
            if (item.source != null) {
                int lineNo = item.source.lineinfo;
                String source = item.source.sourceFile;
                if (source != null) {
                    prefix.append(literal(source).withStyle(ANNOTATE_SOURCE_ADDR)).append(" ");
                }
                if (lineNo >= 0 && doLinesMatch) {
                    prefix.append(literal("line " + lineNo).withStyle(ANNOTATE_SOURCE_ADDR)).append(" ");
                }
            } else {
                prefix.append(literal("no source").withStyle(ANNOTATE_SOURCE_ADDR)).append(" ");
            }
            prefix.append("    ");
            component.getSiblings().add(0, prefix);
        }

        private LocVars detectLocalVar(int slot, int pc) {
            LocVars[] locs = proto.locvars;
            int n = 0;
            for (int i = 0; i < locs.length && locs[i].startpc <= pc; i++) {
                if (pc < locs[i].endpc) {
                    if (n++ == slot) {
                        return locs[i];
                    }
                }
            }
            return null;
        }

        @Override
        public MutableComponent visitGeneric(LuaTrace.TimelineItem item) {
            LuaTrace.LOGGER.warn("Unhandled item in timeline; using default resolver: {}", item);
            return literal(item.resolve(kst, up)).withStyle(DEFAULT);
        }

        @Override
        public MutableComponent visit(LuaTrace.Concatenation item) {
            LuaTrace.LOGGER.warn("Concatenations are deprecated; please add dedicated types in the tracer: {}", item);
            return item.before.accept(this).append(item.after.accept(this));
        }

        @Override
        public MutableComponent visit(LuaTrace.Message item) {
            return literal("*" + item.message + "*").withStyle(MESSAGE);
        }

        @Override
        public MutableComponent visit(LuaTrace.UnaryOperator item) {
            return literal(" ←" + item.message).withStyle(LITERAL.withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    literal("Represents transforming the previous value with this Lua operator.")
            )));
        }

        @Override
        public MutableComponent visit(LuaTrace.Constant item) {
            LuaValue val = kst[item.index];
            return switch (val.type()) {
                case LuaValue.TNIL -> literal("nil").withStyle(CONST_NIL);
                case LuaValue.TBOOLEAN -> literal(val.toboolean() ? "true" : "false").withStyle(CONST_BOOLEAN);
                case LuaValue.TNUMBER -> literal(val.tojstring()).withStyle(CONST_NUMBER);
                case LuaValue.TSTRING ->
                        literal("\"").withStyle(CONST_STRING).append(literal(val.tojstring())).append(literal("\""));
                default -> literal("(").withStyle(DEFAULT).append(literal(val.tojstring()).withStyle(CONST_OTHER))
                        .append(literal(")"));
            };
        }

        @Override
        public MutableComponent visit(LuaTrace.Upvalue item) {
            Upvaldesc uv = up[item.index];
            if (uv.name.equals(LuaString.valueOf("_ENV"))) {
                return literal("(_ENV)").withStyle(NOTE.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        literal("This is the global environment. Global variables index this table.")
                )));
            }
            return literal(uv.name.tojstring()).withStyle(UPVALUE);
        }

        @Override
        public MutableComponent visit(LuaTrace.Literal item) {
            String type = switch (item.v) {
                case NIL -> "<nil>";
                case TRUE -> "<true>";
                case FALSE -> "<false>";
                case TABLE -> "<table>";
            };
            MutableComponent typeComp = literal(type).withStyle(LITERAL.withUnderlined(false)
                    .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            literal("This value was assigned directly to the variable.")
                    )));
            if (item.source != null) {
                // +1 because the startpc is one instruction after value load
                LocVars maybeLocal = detectLocalVar(item.slot, item.source.pc + 1);
                if (maybeLocal != null) {
                    return literal(maybeLocal.varname.tojstring()).withStyle(LOCALVAR).append(typeComp);
                }
            }
            return literal("?").withStyle(DEFAULT).append(typeComp);
        }

        @Override
        public MutableComponent visit(LuaTrace.Slot item) {
            if (item.source != null) {
                LocVars local = detectLocalVar(item.index, item.source.pc);
                if (local != null) {
                    return literal(local.varname.tojstring()).withStyle(LOCALVAR);
                }
            }
            return literal("(temp)").withStyle(NOTE_TEMPSLOT).append(literal("slot" + item.index).withStyle(SLOTVAR));
        }

        @Override
        public MutableComponent visit(LuaTrace.Index item) {
            if (item.indexed instanceof LuaTrace.Constant constVal) {
                LuaValue val = kst[constVal.index];
                if (val.isstring() && val.tojstring().matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
                    return literal(".").withStyle(DEFAULT).append(literal(val.tojstring()).withStyle(CONST_STRING));
                }
            }
            return literal("[").withStyle(DEFAULT).append(item.indexed.accept(this)).append("]");
        }

        @Override
        public MutableComponent visit(LuaTrace.InnerResolve item) {
            if (item.sources.size() > 1) {
                MutableComponent all = empty();
                for (List<LuaTrace.TimelineItem> source : item.sources) {
                    MutableComponent one = empty();
                    for (LuaTrace.TimelineItem sourceItem : source) {
                        one.append(sourceItem.accept(this));
                    }
                    if (!all.getSiblings().isEmpty()) all.append("\n");
                    all.append(one);
                }
                return literal("*" + item.sources.size() + " options*").withStyle(INNER_SOLVE.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        all
                )));
            }
            if (item.sources.isEmpty()) return literal("*no source*").withStyle(MESSAGE);
            MutableComponent result = empty();
            for (LuaTrace.TimelineItem source : item.sources.get(0)) {
                result.append(source.accept(this));
            }
            return result;
        }

        @Override
        public MutableComponent visit(LuaTrace.BinaryOperator item) {
            return combine(
                    item.left.accept(this),
                    literal(" "),
                    literal(item.operator).withStyle(LITERAL),
                    literal(" "),
                    item.right.accept(this)
            );
        }

        @Override
        public MutableComponent visit(LuaTrace.CallResult item) {
            return combine(
                    item.funcDescr.accept(this),
                    literal("( ... )").withStyle(NOTE),
                    literal("→ #" + item.returnId).withStyle(RETURN_IDX)
            );
        }

        @Override
        public MutableComponent visit(LuaTrace.Closure item) {
            Prototype thisP = innerP[item.kpi];
            return combine(
                    literal("fn@").withStyle(NOTE),
                    literal(thisP.shortsource()).withStyle(CALL_SOURCE_ADDR),
                    literal(":").withStyle(CALL_SOURCE_ADDR),
                    literal(String.valueOf(thisP.linedefined)).withStyle(CALL_SOURCE_ADDR),
                    literal("-").withStyle(CALL_SOURCE_ADDR),
                    literal(String.valueOf(thisP.lastlinedefined)).withStyle(CALL_SOURCE_ADDR)
            );
        }

        @Override
        public MutableComponent visit(LuaTrace.MultiSlot multiSlot) {
            LocVars lastLocal = null;
            LuaTrace.SlotAlias finalEntry = multiSlot.slots.lastEntry().getValue();
            ArrayList<MutableComponent> history = new ArrayList<>();
            for (LuaTrace.SlotAlias alias : multiSlot.slots.values()) {
                LocVars local = detectLocalVar(alias.slot(), alias.pcAt());
                if (local != null) {
                    if (finalEntry.pcAt() >= local.endpc || finalEntry.pcAt() < local.startpc) {
                        history.add(combine(
                                literal(local.varname.tojstring()).withStyle(LOCALVAR_EXPIRED),
                                literal(" (not in scope)").withStyle(NOTE)
                        ));
                    } else {
                        lastLocal = local;
                        history.add(combine(
                                literal(local.varname.tojstring()).withStyle(LOCALVAR),
                                literal(" (in scope)").withStyle(NOTE_TEMPSLOT)
                        ));
                    }
                } else {
                    history.add(combine(
                            literal("slot" + alias.slot()).withStyle(SLOTVAR),
                            literal(" (temp)").withStyle(NOTE_TEMPSLOT)
                    ));
                }
            }

            MutableComponent output = empty();
            if (lastLocal == null) {
                output.append(literal("(temp)").withStyle(NOTE_TEMPSLOT));
                output.append(literal("slot" + finalEntry.slot()).withStyle(LOCALVAR));
            } else {
                output.append(literal(lastLocal.varname.tojstring()).withStyle(LOCALVAR));
            }
            
            if (multiSlot.slots.size() > 1) {
                MutableComponent tooltip = empty();
                tooltip.append(literal("This slot has multiple names:\n").withStyle(NOTE));
                int n = history.size();
                for (MutableComponent component : history) {
                    tooltip.append(component);
                    if (--n > 0) tooltip.append("\n");
                }
                output.append(literal("✎").withStyle(LOCALVAR.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        tooltip
                ))));
            }
            return output;
        }
    }
}
