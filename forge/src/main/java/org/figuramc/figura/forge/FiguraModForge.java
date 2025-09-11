package org.figuramc.figura.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.SimpleReloadableResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.backend2.trust.KeyStoreHelper;
import org.figuramc.figura.backend2.ForgeNetworking;

@Mod("figura")
public class FiguraModForge {
    // dummy empty mod class, we are client only
    public FiguraModForge() {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> FiguraModClientForge::initClient);
        DistExecutor.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> FiguraModServerForge::initServer);
        ForgeNetworking.init();
        if (FMLEnvironment.dist == Dist.CLIENT)
            FiguraModClientForge.registerResourceListeners();
    }
}
