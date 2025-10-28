package org.figuramc.figura.neoforge;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.backend2.FSBForge;
import org.figuramc.figura.config.ConfigManager;
import org.figuramc.figura.config.neoforge.ModConfig;
import org.figuramc.figura.gui.neoforge.GuiOverlay;
import org.figuramc.figura.gui.neoforge.GuiUnderlay;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.handlers.s2c.Handlers;
import org.figuramc.figura.server.packets.handlers.s2c.S2CPacketHandler;
import org.figuramc.figura.utils.neoforge.FiguraResourceListenerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@EventBusSubscriber(modid = FiguraMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FiguraModClientNeoForge extends FiguraMod {
    // keybinds stored here
    public static List<KeyMapping> KEYBINDS = new ArrayList<>();

    @SubscribeEvent
    public static void onInitializeClient(FMLClientSetupEvent event) {
        onClientInit();
        ModConfig.registerConfigScreen();
    }

    @SubscribeEvent
    public static void registerResourceListener(RegisterClientReloadListenersEvent event) {
        getResourceListeners().forEach(figuraResourceListener -> event.registerReloadListener((FiguraResourceListenerImpl)figuraResourceListener));
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(new ResourceLocation(FiguraMod.MOD_ID, "figura_overlay"), new GuiOverlay());
        event.registerBelowAll(new ResourceLocation(FiguraMod.MOD_ID, "figura_underlay"), new GuiUnderlay());
    }

    private static final List<ResourceLocation> vanillaOverlays = new ArrayList<>();

    public static void cancelVanillaOverlays(RenderGuiLayerEvent.Pre event) {
        if (event.getName().getNamespace().equals("minecraft")) {
            Entity entity = Minecraft.getInstance().getCameraEntity();
            Avatar avatar = entity == null ? null : AvatarManager.getAvatar(entity);
            if (avatar != null && avatar.luaRuntime != null && !avatar.luaRuntime.renderer.renderHUD) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void registerKeyBinding(RegisterKeyMappingsEvent event) {
        // Config has to be initialized here, so that the keybinds exist on time
        ConfigManager.init();
        for (KeyMapping value : KEYBINDS) {
            if(value != null)
                event.register(value);
        }
    }

    static void initClient() {
        NeoForge.EVENT_BUS.addListener(FiguraModClientNeoForge::cancelVanillaOverlays);
        new FSBForge();
    }

    public static void handlePayload(PayloadWrapper wrapper, IPayloadContext playPayloadContext) {
        Packet source = wrapper.source();
        S2CPacketHandler<Packet> handler = Handlers.getHandler(source.getId());
        if (handler != null) {
            Minecraft.getInstance().execute(() -> handler.handle(source));
        }
    }
}
