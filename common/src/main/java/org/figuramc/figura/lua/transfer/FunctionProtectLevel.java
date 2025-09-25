package org.figuramc.figura.lua.transfer;

/**
 * Defines various protection schemes for inter-avatar functions.
 */
public enum FunctionProtectLevel {
    /**
     * Default, plus:
     * <ul>
     *     <li>Apply protection rules to functions sent and received</li>
     * </ul>
     */
    MAXIMUM,
    /**
     * Low, plus:
     * <ul>
     *     <li>Remove metatables from received tables</li>
     *     <li>Copy vectors and matrices</li>
     *     <li>Copy sent tables instead of sharing them</li>
     * </ul>
     */
    DEFAULT,
    /**
     * <ul>
     *     <li>Repackage userdata (i.e. prevent Figura type metatable leakage)</li>
     *     <li>Wrap sent tables (linkage)</li>
     * </ul>
     */
    LOW,
    /**
     * Do nothing
     */
    NOTHING;

    public static final String hint;

    static {
        StringBuilder s = new StringBuilder();
        int i = 0;
        for (FunctionProtectLevel value : FunctionProtectLevel.values()) {
            if (i++ > 0) s.append(", ");
            s.append(value.name());
        }
        hint = s.toString();
    }
}
