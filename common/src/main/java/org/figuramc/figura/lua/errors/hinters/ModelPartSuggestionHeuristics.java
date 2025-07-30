package org.figuramc.figura.lua.errors.hinters;

import org.figuramc.figura.model.FiguraModelPart;

import java.text.Collator;
import java.util.List;
import java.util.Locale;

public class ModelPartSuggestionHeuristics {
    public static final Collator EQUALS_IGNORE_CASE = Collator.getInstance(Locale.ENGLISH);

    static {
        EQUALS_IGNORE_CASE.setStrength(Collator.SECONDARY);
    }

    /**
     * Bonus for having exactly the same name as requested.
     * This only really makes sense for the "same name in different parent" hinter, because
     * if the "different name, same parent" hinter were to trigger the code would already be correct
     */
    public static final int EXACT_NAME = 100;

    /**
     * Bonus for having the same name, normalized (lowercase, with all spaces removed)
     */
    public static final int SIMILAR_NAME = 70;

    /**
     * Bonus for each detected "next" name in the chain that is also valid
     */
    public static final int PER_NEXT_OK = 15;

    /**
     * Penalty if the "next" names contain an error
     */
    public static final int NEXT_ERR = -5;


    public static int getScore(String queryName, List<String> next, FiguraModelPart suggestion) {
        String suggestedName = suggestion.name;
        int score = 0;

        if (queryName.equals(suggestedName)) {
            score += EXACT_NAME;
        } else {
            String querySimplified = queryName.replaceAll("\\s", "");
            String suggestSimplified = suggestedName.replaceAll("\\s", "");
            if (EQUALS_IGNORE_CASE.equals(querySimplified, suggestSimplified)) score += SIMILAR_NAME;
        }

        FiguraModelPart at = suggestion;
        for (String key : next) {
            Object result = at.__index(key);
            if (result == null) {
                score += NEXT_ERR;
                break;
            }
            score += PER_NEXT_OK;
            if (result instanceof FiguraModelPart) {
                at = (FiguraModelPart) result;
            } else {
                break;
            }
        }

        return score;
    }
}
