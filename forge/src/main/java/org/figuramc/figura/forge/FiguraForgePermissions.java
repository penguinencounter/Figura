package org.figuramc.figura.forge;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.FiguraPermissions;
import org.figuramc.figura.server.utils.Pair;

import java.util.HashMap;

public class FiguraForgePermissions {

    private static final HashMap<String, String> registeredPermission = new HashMap<String, String>() {{
        FiguraPermissions.PERMISSIONS_LIST.forEach((pair) -> {
            put(pair.left(), createNode(pair));
        });
    }};

    public static String createNode(Pair<String, Boolean> pair) {
        String name = pair.left();
        boolean defaultVal = pair.right();
        return PermissionAPI.registerNode(FiguraModServer.MOD_ID+"."+name, DefaultPermissionLevel.OP, "");
    }

    @SuppressWarnings("unchecked")
    public static String getPermission(String permission) {
        return registeredPermission.get(permission);
    }
}
