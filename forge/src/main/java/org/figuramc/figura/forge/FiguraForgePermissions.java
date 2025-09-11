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

    private static final HashMap<FiguraPermissionNodes, String> registeredPermission = new HashMap<String, String>() {{
        FiguraPermissions.PERMISSIONS_LIST.forEach((pair) -> {
            put(pair.left(), createNode(pair));
        });
    }};
    private static final HashMap<FiguraPermissionNodes, PermissionNode<?>> registeredOption = new HashMap<>() {{
        FiguraPermissions.OPTIONS_LIST.forEach((pair) -> {
            put(pair.left(), createOption(pair));
        });
    }};

    public static String createNode(Pair<FiguraPermissionNodes, Boolean> pair) {
        String name = pair.left().toString();
        boolean defaultVal = pair.right();
        return new PermissionNode<>(FiguraModServer.MOD_ID, name, PermissionTypes.BOOLEAN, (a,b,c) -> defaultVal);
    }

    public static PermissionNode<String> createOption(Pair<FiguraPermissionNodes, String> pair) {
        String name = pair.left().toString();
        String defaultVal = pair.right();
        return new PermissionNode<>(FiguraModServer.MOD_ID, name, PermissionTypes.STRING, (a,b,c) -> defaultVal);
    }

    @SubscribeEvent
    public void registerPermissions(PermissionGatherEvent.Nodes event) {
        event.addNodes(registeredPermission.values());
        event.addNodes(registeredOption.values());
    }

    @SuppressWarnings("unchecked")
    public static String getPermission(FiguraPermissionNodes permission) {
        return registeredPermission.get(permission);
    }

    @SuppressWarnings("unchecked")
    public static PermissionNode<String> getOption(FiguraPermissionNodes option) {
        return (PermissionNode<String>) registeredOption.get(option);
    }
}
