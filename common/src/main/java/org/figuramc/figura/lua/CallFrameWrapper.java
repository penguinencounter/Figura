package org.figuramc.figura.lua;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.lib.DebugLib;

import java.lang.reflect.Field;
import java.util.HashMap;

public class CallFrameWrapper {
    private final DebugLib.CallFrame target;

    public CallFrameWrapper(DebugLib.CallFrame of) {
        target = of;
    }

    public LuaFunction get_f() {
        try {
            return (LuaFunction) f.get(target);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public int get_pc() {
        try {
            return pc.getInt(target);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Field f;
    private static final Field pc;

    static {
        Field[] fields = DebugLib.CallFrame.class.getDeclaredFields();
        HashMap<String, Field> fieldMap = new HashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            fieldMap.put(field.getName(), field);
        }
        f = fieldMap.get("f");
        pc = fieldMap.get("pc");
    }
}
