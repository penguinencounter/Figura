package org.figuramc.figura.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.figuramc.figura.backend2.ForgeNetworking;

@Mod("figura")
public class FiguraModForge {
    // dummy empty mod class, we are client only
    public FiguraModForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FiguraModClientNeoForge.initClient();
        }
        else {
            FiguraModServerForge.initServer();
        }
        ForgeNetworking.init();
    }
}
