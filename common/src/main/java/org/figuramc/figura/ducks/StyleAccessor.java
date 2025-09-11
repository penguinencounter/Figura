package org.figuramc.figura.ducks;

import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

public interface StyleAccessor {
    void setObfuscated(Boolean bool);

    Style figura$withObfuscated(Boolean obfuscated);
}
