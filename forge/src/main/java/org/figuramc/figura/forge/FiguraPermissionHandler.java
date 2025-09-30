package org.figuramc.figura.forge;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.context.IContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import java.util.*;

public class FiguraPermissionHandler implements IPermissionHandler {
    private static final HashMap<String, DefaultPermissionLevel> PERMISSION_LEVEL_MAP = new HashMap<String, DefaultPermissionLevel>();
    private static final HashMap<String, Node<String>> STRING_MAP = new HashMap<String, Node<String>>();
    private static final HashMap<String, Node<Boolean>> BOOL_MAP = new HashMap<String, Node<Boolean>>();

    static class Node<T> {
        String name;
        T defaultValue;
        Map<GameProfile, T> permissionMap;

        Node(String name, T defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.permissionMap = new HashMap<>();
        }

        void setPermission(GameProfile profile, T value) {
            permissionMap.put(profile, value);
        }

        T hasPermission(GameProfile profile) {
            return permissionMap.getOrDefault(profile, defaultValue);
        }
    }

    @Override
    public void registerNode(String string, DefaultPermissionLevel defaultPermissionLevel, String string2) {
        PERMISSION_LEVEL_MAP.put(string, defaultPermissionLevel);
    }

    public String registerNode(String string, String defaultPermission, String description) {
        STRING_MAP.put(string, new Node<>(string, defaultPermission));
        return string;
    }

    public String registerNode(String string, Boolean defaultPermission, String description) {
        BOOL_MAP.put(string, new Node<>(description, defaultPermission));
        return string;
    }

    @Override
    public Collection<String> getRegisteredNodes() {
        List<String> nodes =  new ArrayList<>(PERMISSION_LEVEL_MAP.keySet());
        nodes.addAll(STRING_MAP.keySet());
        nodes.addAll(BOOL_MAP.keySet());
        return nodes;
    }

    @Override
    public boolean hasPermission(GameProfile gameProfile, String node, @Nullable IContext iContext) {
        if (BOOL_MAP.containsKey(node)) {
            return BOOL_MAP.get(node).hasPermission(gameProfile);
        }

        DefaultPermissionLevel level = getDefaultPermissionLevel(node);
        if(level == DefaultPermissionLevel.NONE)
        {
            return false;
        }
        else if(level == DefaultPermissionLevel.ALL)
        {
            return true;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.getPlayerList().isOp(gameProfile);
    }

    @Override
    public String getNodeDescription(String nodeName) {
        return "";
    }

    public String getOption(GameProfile gameProfile, String perm, Object o) {
        if (STRING_MAP.containsKey(perm)) {
            return STRING_MAP.get(perm).hasPermission(gameProfile);
        }
        return "";
    }

    public DefaultPermissionLevel getDefaultPermissionLevel(String node)
    {
        DefaultPermissionLevel level = PERMISSION_LEVEL_MAP.get(node);
        return level == null ? DefaultPermissionLevel.NONE : level;
    }
}
