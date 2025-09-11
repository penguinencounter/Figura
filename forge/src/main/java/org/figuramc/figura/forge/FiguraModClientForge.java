package org.figuramc.figura.forge;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.SimpleReloadableResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.backend2.FSBForge;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.config.ConfigManager;
import org.figuramc.figura.config.forge.ModConfig;
import org.figuramc.figura.gui.FiguraGui;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.handlers.s2c.Handlers;
import org.figuramc.figura.server.packets.handlers.s2c.S2CPacketHandler;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;
import org.figuramc.figura.utils.forge.FiguraResourceListenerImpl;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = FiguraMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FiguraModClientForge extends FiguraMod {
    // keybinds stored here
    public static List<KeyMapping> KEYBINDS = new ArrayList<>();

    @SubscribeEvent
    public static void onInitializeClient(FMLClientSetupEvent event) {
        NetworkStuff.initializeHttpClient();
        onClientInit();
        ModConfig.registerConfigScreen();
        vanillaOverlays.addAll(Arrays.asList(RenderGameOverlayEvent.ElementType.values()));
    }


    public static void registerResourceListeners() {
        FiguraMod.getResourceListeners().forEach(figuraResourceListener -> ((SimpleReloadableResourceManager)Minecraft.getInstance().getResourceManager()).registerReloadListener((PreparableReloadListener) figuraResourceListener));
    }

    private static final List<RenderGameOverlayEvent.ElementType> vanillaOverlays = new ArrayList<>();

    public static void cancelVanillaOverlays(RenderGameOverlayEvent.Pre event) {
        if (vanillaOverlays.contains(event.getType())) {
            Entity entity = Minecraft.getInstance().getCameraEntity();
            Avatar avatar = entity == null ? null : AvatarManager.getAvatar(entity);
            if (avatar != null && avatar.luaRuntime != null && !avatar.luaRuntime.renderer.renderHUD) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void registerKeyBinding(FMLClientSetupEvent event) {
        // Config has to be initialized here, so that the keybinds exist on time
        ConfigManager.init();
        for (KeyMapping value : KEYBINDS) {
            if(value != null)
                ClientRegistry.registerKeyBinding(value);
        }
    }

    static void initClient() {
        MinecraftForge.EVENT_BUS.addListener(FiguraModClientForge::cancelVanillaOverlays);
        new FSBForge();
    }

    public static void registerPacketListener(Identifier id, EventNetworkChannel channel) {
        S2CPacketHandler<Packet> handler = Handlers.getHandler(id);
        if (handler != null) channel.addListener(new ForgeNetworkListener<>(id, handler));
    }

    private static final class ForgeNetworkListener<P extends Packet> implements Consumer<NetworkEvent> {
        private final Identifier id;
        private final S2CPacketHandler<P> handler;

        private ForgeNetworkListener(Identifier id, S2CPacketHandler<P> handler) {
            this.id = id;
            this.handler = handler;
        }

        @Override
        public void accept(NetworkEvent event) {
            if (event.getPayload() == null) return;
            NetworkEvent.Context ctx = event.getSource().get();
            if (ctx.getDirection().equals(NetworkDirection.PLAY_TO_CLIENT)) {
                try {
                    P packet = handler.serialize(new FriendlyByteBufWrapper(event.getPayload()));
                    Minecraft.getInstance().execute(() -> handler.handle(packet));
                    ctx.setPacketHandled(true);
                }
                catch (Exception e) {
                    FiguraMod.LOGGER.error(String.format("Failed to handle packet %s", id), e);
                }
            }
        }
    }
}
