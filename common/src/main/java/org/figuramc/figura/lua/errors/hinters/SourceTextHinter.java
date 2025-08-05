package org.figuramc.figura.lua.errors.hinters;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.figuramc.figura.lua.errors.LuaErrorCapture;
import org.figuramc.figura.utils.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.minecraft.network.chat.Component.literal;
import static net.minecraft.network.chat.Component.translatable;

/**
 * Provides source text as a hint (meant as a replacement for the 'script:' block in the error)
 */
public class SourceTextHinter implements ErrorHinter {
    public static final int COLOR = 0xffffff;
    public static final int CONTEXT = 0x505050;
    public static final int SUSPECT = 0xb0b0b0;
    public static final int ERROR = 0xff8080;
    public static final int HEADER = 0xff9080;
    public static final int HEADER_SUBJECT = 0xffb080;

    private static Component minified() {
        HoverEvent tooltip = new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                translatable(
                        "figura.errors.script_minified.fix",
                        translatable("figura.config.format_script"),
                        translatable("figura.config.format_script.1"),
                        translatable("figura.config.format_script.2")
                )
        );
        return literal("[minified] ")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withHoverEvent(tooltip))
                .append(translatable("figura.errors.script_minified"));
    }

    private static void markUpLines(
            MutableComponent to,
            int firstLineNo,
            int lastLineNo,
            int firstBlame,
            int lastBlame,
            int errorAt,
            List<String> lines) {
        int lineNoLength = String.valueOf(lastLineNo).length();
        int i = firstLineNo;
        boolean skip = false;
        for (Iterator<String> it = lines.iterator(); it.hasNext(); ) {
            String line = it.next();
            if (line.matches("^\\s*$")) {
                i++;
                skip = true;
                continue;
            }
            int markerColor;
            if (i == errorAt) markerColor = ERROR;
            else if (i >= firstBlame && i <= lastBlame) markerColor = SUSPECT;
            else markerColor = CONTEXT;
            String bar;
            if (skip && !it.hasNext()) {
                bar = ":";
            } else if (skip) {
                bar = "╻";
            } else if (!it.hasNext()) {
                bar = "╹";
            } else {
                bar = "┃";
            }
            to.append(Component.literal(String.format("%0" + lineNoLength + "d %s ", i, bar))
                    .withStyle(Style.EMPTY.withColor(markerColor))
                    .append(line));
            if (it.hasNext()) to.append("\n");
            skip = false;
            i++;
        }
    }

    private static @NotNull MutableComponent getFormatted(
            String scriptName,
            int firstLine,
            int lastLine,
            int firstBlameLine,
            int lastBlameLine,
            int lineno,
            List<String> span
    ) {
        MutableComponent result = Component.literal("").withStyle(Style.EMPTY.withColor(COLOR));
        result.append(Component.literal("script '").withStyle(Style.EMPTY.withColor(HEADER))
                .append(Component.literal(scriptName).withStyle(Style.EMPTY.withColor(HEADER_SUBJECT)))
                .append("':\n"));

        markUpLines(
                result,
                firstLine,
                lastLine,
                firstBlameLine,
                lastBlameLine,
                lineno,
                span
        );

        return result;
    }

    @Override
    public int getOrdering() {
        return -1; // First thing
    }

    @Override
    public @Nullable Component getHint(LuaErrorCapture cap) {
        FiguraLuaRuntime runtime = cap.runtime;
        if (runtime.owner.minify) return minified();

        // Get the line information for the current instruction (the one that errored)
        LuaErrorCapture.PCFrame top = cap.getTop();
        List<Integer> linesInFunction = Arrays.stream(top.c.p.lineinfo).boxed().toList();
        int lineno = top.c.p.lineinfo[top.pc];

        // Get the line number for the previous and next lines
        // (due to an issue with LuaJ it looks like statements that span multiple lines all have the same line number)
        int firstLine = linesInFunction.get(Math.max(linesInFunction.indexOf(lineno) - 1, 0));
        int firstBlameLine = firstLine;
        int lastLine = linesInFunction.get(Math.min(Math.min(
                linesInFunction.lastIndexOf(lineno) + 1,
                linesInFunction.size() - 1
        ), lineno + 10));
        int lastBlameLine = lastLine;

        if (firstLine < lineno) {
            firstBlameLine++;
        }
        if (firstLine < lineno - 1) {
            firstLine++;
        }
        if (firstLine > lineno - 1) firstLine = lineno - 1;

        if (lastLine > lineno) {
            lastBlameLine--;
        }
        if (lastLine > lineno + 1) {
            lastLine--;
        }
        if (lastLine < lineno + 1) lastLine = lineno + 1;

        // Get the source text
        String scriptName = top.c.p.source.checkjstring();
        String sourceText = runtime.getScript(scriptName);
        if (sourceText == null)
            return Component.translatable("figura.errors.missing_script").withStyle(ChatFormatting.GRAY);

        List<String> scriptLines = sourceText.lines().toList();
        List<String> span = new ArrayList<>(scriptLines.subList(firstLine - 1, lastLine));

        // Remove redundant blank lines from the start and end
        for (ListIterator<String> it = span.listIterator(); it.hasNext(); ) {
            String item = it.next();
            if (item.matches("^\\s*$")) {
                it.remove();
                firstLine++;
            } else break;
        }
        for (ListIterator<String> it = span.listIterator(span.size()); it.hasPrevious(); ) {
            String item = it.previous();
            if (item.matches("^\\s*$")) {
                it.remove();
                lastLine--;
            } else break;
        }

        return getFormatted(scriptName, firstLine, lastLine, firstBlameLine, lastBlameLine, lineno, span);
    }

    public static Component getContextHint(String scriptName, int lineno, FiguraLuaRuntime runtime) {
        if (runtime.owner.minify) return minified();
        String sourceText = runtime.getScript(scriptName);
        if (sourceText == null)
            return Component.translatable("figura.errors.missing_script").withStyle(ChatFormatting.GRAY);

        List<String> scriptLines = sourceText.lines().toList();

        // TODO: collapse multiple spaces but still display the same number of info lines

        int firstLine = Math.max(lineno - 5, 1);
        int lastLine = Math.min(lineno + 1, scriptLines.size());

        List<String> span = new ArrayList<>(scriptLines.subList(firstLine - 1, lastLine));

        return getFormatted(scriptName, firstLine, lastLine, firstLine, lastLine, lineno, span);
    }
}
