package org.figuramc.figura.neoforge;


import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.FiguraPermissions;
import org.figuramc.figura.server.utils.Pair;

import java.util.HashMap;

public class FiguraForgePermissions {

    private static final HashMap<String, PermissionNode<?>> registeredPermission = new HashMap<>() {{
        FiguraPermissions.PERMISSIONS_LIST.forEach((pair) -> {
            put(pair.left(), createNode(pair));
        });
    }};

    public static PermissionNode<Boolean> createNode(Pair<String, Boolean> pair) {
        String name = pair.left();
        boolean defaultVal = pair.right();
        return new PermissionNode<>(FiguraModServer.MOD_ID, name, PermissionTypes.BOOLEAN, (a, b, c) -> defaultVal);
    }

    @SubscribeEvent
    public void registerPermissions(PermissionGatherEvent.Nodes event) {
        event.addNodes(registeredPermission.values());
    }

    @SuppressWarnings("unchecked")
    public static PermissionNode<Boolean> getPermission(String permission) {
        return (PermissionNode<Boolean>) registeredPermission.get(permission);
    }
}
