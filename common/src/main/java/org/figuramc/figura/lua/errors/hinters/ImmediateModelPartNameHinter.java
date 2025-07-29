package org.figuramc.figura.lua.errors.hinters;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.figuramc.figura.lua.errors.AnalysisTools;
import org.figuramc.figura.lua.errors.LuaErrorCapture;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.utils.ColorUtils;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaValue;

import java.util.*;

/**
 * hints for names in the same part
 * <p>
 * TODO: how the heck do we make this translatable holy
 */
public class ImmediateModelPartNameHinter implements ErrorHinter {
    public static final int MAX_CAPITALIZATION = 3;
    public static final int MAX_OTHERS = 12;

    public static String generateCapsHint(String provided, String target) {
        StringBuilder b = new StringBuilder().append("(with ");
        if (provided.length() != target.length())
            throw new IllegalArgumentException("can't generate caps hint for strings of different lengths");
        boolean isFirst = true;
        boolean lastCase = false;
        for (int i = 0; i < provided.length(); i++) {
            String providedChar = provided.substring(i, i + 1);
            String targetChar = target.substring(i, i + 1);
            if (providedChar.equals(targetChar)) continue;

            if (providedChar.toUpperCase(Locale.ENGLISH).equals(targetChar)) {
                if (!isFirst) b.append(", ");
                if (isFirst || !lastCase) b.append("uppercase ");
                b.append(targetChar);
                lastCase = true;
                isFirst = false;
            } else if (providedChar.toLowerCase(Locale.ENGLISH).equals(targetChar)) {
                if (!isFirst) b.append(", ");
                if (isFirst || !lastCase) b.append("lowercase ");
                b.append(targetChar);
                lastCase = false;
                isFirst = false;
            }
        }
        return b.append(")").toString();
    }

    @Override
    public @Nullable Component getHint(List<AnalysisTools.DataflowElement> origin, LuaErrorCapture cap) {
        // Is there enough data?
        if (origin.size() < 2) return null;
        AnalysisTools.DataflowElement parent = origin.get(origin.size() - 2);
        if (parent.valueAtHere == null) return null;

        // Is it the right type?
        if (!parent.valueAtHere.isuserdata(FiguraModelPart.class)) return null;
        FiguraModelPart thePart = (FiguraModelPart) parent.valueAtHere.checkuserdata(FiguraModelPart.class);

        AnalysisTools.DataflowElement errorFrame = origin.get(origin.size() - 1);
        LuaValue k;
        if (errorFrame instanceof AnalysisTools.ConstantIndexTable) {
            k = ((AnalysisTools.ConstantIndexTable) errorFrame).key;
        } else return null;
        if (!k.isstring()) return null;
        String attemptedName = k.checkjstring();
        String lowercaseAttemptedName = attemptedName.toLowerCase(Locale.ENGLISH);

        Set<String> seen = new HashSet<>();
        // capitalization mistakes: name -> details
        Map<String, String> capitalization = new HashMap<>();
        // other name options, sorted in some intelligent way hopefully
        // TODO: sort it actually
        Map<String, Integer> otherOptions = new HashMap<>();
        // name options with more than one matching child part (danger)
        // TODO: highlight these
        Set<String> overloaded = new HashSet<>();

        for (FiguraModelPart child : thePart.children) {
            String name = child.name;
            if (seen.contains(name)) {
                overloaded.add(name);
                continue;
            }
            seen.add(name);

            // Check for matching non-case-sensitive
            if (name.toLowerCase(Locale.ENGLISH).equals(lowercaseAttemptedName) && capitalization.size() < MAX_CAPITALIZATION) {
                capitalization.put(name, generateCapsHint(attemptedName, name));
            } else {
                otherOptions.put(name, 0);
            }
        }

        if (capitalization.isEmpty() && otherOptions.isEmpty()) return null;

        MutableComponent builder = Component.literal("")
                .withStyle(Style.EMPTY.withColor(ColorUtils.Colors.FIGURA_BLUE.hex));
        // TODO: if anything this should be the line that is localized
        builder.append(String.format("Did you mean one of these other parts in %s?", thePart.name)).append("\n");

        Style prominent = Style.EMPTY.withColor(0xb0ffb0);
        Style secondary = Style.EMPTY.withColor(0x608060);
        Style gray = Style.EMPTY.withColor(ChatFormatting.GRAY);
        Style grayer = Style.EMPTY.withColor(0x777777);

        boolean isFirst = true;
        for (Map.Entry<String, String> item : capitalization.entrySet()) {
            if (!isFirst)
                builder.append("\n");
            MutableComponent row = Component.literal(" • ").withStyle(gray);
            row.append(Component.literal(item.getKey()).withStyle(prominent));
            row.append(" ");
            row.append(item.getValue());
            builder.append(row);
            isFirst = false;
        }

        if (!otherOptions.isEmpty()) {
            boolean isStandalone = capitalization.isEmpty();

            if (!isFirst)
                builder.append("\n");
            MutableComponent everythingElse = Component.literal(isStandalone ? " • " : " • other options: ")
                    .withStyle(isStandalone ? gray : grayer);
            Style elementStyle = isStandalone ? prominent : secondary;

            boolean listIsFirst = true;
            List<Map.Entry<String, Integer>> sortable = new ArrayList<>(otherOptions.entrySet());
            sortable.sort(
                    Map.Entry.<String, Integer>comparingByValue()
                            .thenComparing(Map.Entry.comparingByKey())
            );
            int count = 0;
            for (Map.Entry<String, Integer> item : sortable) {
                if (++count > MAX_OTHERS) {
                    everythingElse.append(", " + (sortable.size() - MAX_OTHERS) + " more");
                    break;
                }
                if (!listIsFirst) everythingElse.append(", ");
                everythingElse.append(Component.literal(item.getKey()).withStyle(elementStyle));
                listIsFirst = false;
            }
            builder.append(everythingElse);
        }


        return builder;
    }
}
