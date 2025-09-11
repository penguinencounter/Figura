package org.figuramc.figura.server;

import org.figuramc.figura.server.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class FiguraPermissions {
    public static final List<Pair<String, Boolean>> PERMISSIONS_LIST;

    static {
        PERMISSIONS_LIST = new ArrayList<>();
        PERMISSIONS_LIST.add(Pair.of("figura.avatars.immortalize", false));
        PERMISSIONS_LIST.add(Pair.of("figura.avatars.set", false));
    }
}
