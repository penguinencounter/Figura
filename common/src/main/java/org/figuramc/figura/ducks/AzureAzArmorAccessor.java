package org.figuramc.figura.ducks;

import mod.azure.azurelibarmor.rewrite.render.AzProvider;
import net.minecraft.world.item.ItemStack;
import org.figuramc.figura.avatar.Avatar;

public interface AzureAzArmorAccessor {
    Avatar figura$getAvatar();

    AzProvider<ItemStack> figura$getProvider();
}
