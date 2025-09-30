package org.figuramc.figura.forge;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.FiguraPermissionNodes;
import org.figuramc.figura.server.FiguraPermissions;
import org.figuramc.figura.server.utils.Pair;

import java.util.HashMap;

public class FiguraForgePermissions {

    private static final HashMap<FiguraPermissionNodes, String> registeredPermission = new HashMap<FiguraPermissionNodes, String>() {{
        FiguraPermissions.PERMISSIONS_LIST.forEach((pair) -> {
            put(pair.left(), createNode(pair));
        });
    }};
    private static final HashMap<FiguraPermissionNodes, String> registeredOption = new HashMap<FiguraPermissionNodes, String>() {{
        FiguraPermissions.OPTIONS_LIST.forEach((pair) -> {
            put(pair.left(), createOption(pair));
        });
    }};

    public static String createNode(Pair<FiguraPermissionNodes, Boolean> pair) {
        String name = pair.left().toString();
        boolean defaultVal = pair.right();
        return (PermissionAPI.getPermissionHandler() instanceof FiguraPermissionHandler) ? ((FiguraPermissionHandler) PermissionAPI.getPermissionHandler()).registerNode(FiguraModServer.MOD_ID+"."+name, defaultVal,"") : null;
    }

    public static String createOption(Pair<FiguraPermissionNodes, String> pair) {
        String name = pair.left().toString();
        String defaultVal = pair.right();
        return (PermissionAPI.getPermissionHandler() instanceof FiguraPermissionHandler) ? ((FiguraPermissionHandler) PermissionAPI.getPermissionHandler()).registerNode(FiguraModServer.MOD_ID+"."+name, defaultVal,"") : null;
    }

    @SuppressWarnings("unchecked")
    public static String getPermission(FiguraPermissionNodes permission) {
        return registeredPermission.get(permission);
    }

    @SuppressWarnings("unchecked")
    public static String getOption(FiguraPermissionNodes option) {
        return registeredOption.get(option);
    }
}
