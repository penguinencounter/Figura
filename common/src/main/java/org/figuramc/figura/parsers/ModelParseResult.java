package org.figuramc.figura.parsers;

import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.Objects;

//dummy class containing the return object of the parser
// auto-converted record
public final class ModelParseResult {
    private final CompoundTag textures;
    private final List<CompoundTag> animationList;
    private final CompoundTag modelNbt;

    public ModelParseResult(CompoundTag textures, List<CompoundTag> animationList, CompoundTag modelNbt) {
        this.textures = textures;
        this.animationList = animationList;
        this.modelNbt = modelNbt;
    }

    public CompoundTag textures() {
        return textures;
    }

    public List<CompoundTag> animationList() {
        return animationList;
    }

    public CompoundTag modelNbt() {
        return modelNbt;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        ModelParseResult that = (ModelParseResult) obj;
        return Objects.equals(this.textures, that.textures) &&
                Objects.equals(this.animationList, that.animationList) &&
                Objects.equals(this.modelNbt, that.modelNbt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(textures, animationList, modelNbt);
    }

    @Override
    public String toString() {
        return "ModelParseResult[" +
                "textures=" + textures + ", " +
                "animationList=" + animationList + ", " +
                "modelNbt=" + modelNbt + ']';
    }

}
