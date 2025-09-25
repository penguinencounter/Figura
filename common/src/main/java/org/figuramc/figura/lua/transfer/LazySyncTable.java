package org.figuramc.figura.lua.transfer;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public class LazySyncTable extends LuaTable {
    private final LuaTable reference;
    private final LuaTransformer outTransform;
    private final LuaTransformer inTransform;

    public LazySyncTable(LuaTable reference, LuaTransformer outTransform, LuaTransformer inTransform) {
        this.reference = reference;
        this.outTransform = outTransform;
        this.inTransform = inTransform;
        super.setmetatable(outTransform.visit(reference.getmetatable()));
    }

    @Override
    public int rawlen() {
        return reference.rawlen();
    }

    @Override
    public LuaValue rawget(int key) {
        return outTransform.visit(reference.rawget(key));
    }

    @Override
    public LuaValue rawget(LuaValue key) {
        return outTransform.visit(reference.rawget(key));
    }

    @Override
    public void rawset(int key, LuaValue value) {
        reference.rawset(key, inTransform.visit(value));
    }

    @Override
    public void rawset(LuaValue key, LuaValue value) {
        reference.rawset(inTransform.visit(key), inTransform.visit(value));
    }

    public LazySyncTable removeMeta() {
        // this does not propagate to the actual table, so it basically "neutralizes" it
        super.setmetatable(null);
        return this;
    }
}
