package org.figuramc.figura.mixin.font;

import net.minecraft.network.chat.Style;
import org.figuramc.figura.ducks.StyleAccessor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Style.class)
public class StyleMixin implements StyleAccessor {

    @Final
    @Mutable
    @Shadow
    private Boolean obfuscated;

    @Override
    public Style figura$withObfuscated(Boolean obfuscated) {
        this.obfuscated = obfuscated;
        return (Style) (Object) this;
    }

    @Override
    public void setObfuscated(Boolean obfuscated) {
        this.obfuscated = obfuscated;
    }
}
