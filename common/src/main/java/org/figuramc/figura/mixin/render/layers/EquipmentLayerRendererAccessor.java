package org.figuramc.figura.mixin.render.layers;

import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentModelSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.equipment.EquipmentModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Function;

@Mixin(EquipmentLayerRenderer.class)
public interface EquipmentLayerRendererAccessor {
    @Accessor("equipmentModels")
    EquipmentModelSet figura$getModels();

    @Accessor("layerTextureLookup")
    Function<EquipmentLayerRenderer.LayerTextureKey, ResourceLocation> layerTextureLookup();

    @Accessor("trimSpriteLookup")
    Function<EquipmentLayerRenderer.TrimSpriteKey, TextureAtlasSprite> trimSpriteLookup();

    @Invoker("getColorForLayer")
    static int getColorForLayer(EquipmentModel.Layer layer, int i) {
        throw new AssertionError();
    }

}
