package org.figuramc.figura.utils.fabric;

import net.fabricmc.loader.api.FabricLoader;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.utils.PlatformUtils;
import org.luaj.vm2.ast.Str;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class PlatformUtilsImpl {
    public static Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    public static String getFiguraModVersionString() {
        return FabricLoader.getInstance().getModContainer(FiguraMod.MOD_ID).get().getMetadata().getVersion().getFriendlyString();
    }

    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static String getModVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).get().getMetadata().getVersion().getFriendlyString();
    }

    public static PlatformUtils.ModLoader getModLoader() {
        return PlatformUtils.ModLoader.FABRIC;
    }

    public static InputStream loadFileFromRoot(String path) throws IOException {
        Path file = FabricLoader.getInstance().getModContainer(FiguraMod.MOD_ID).get().getPath(path);
        return Files.newInputStream(file);
    }
}
