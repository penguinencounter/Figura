package org.figuramc.figura.lua.errors;

import org.luaj.vm2.Prototype;

import java.util.*;

public class ProtoCache {
    public final Prototype p;
    public final int EOF;

    public final Map<Integer, Instruction> decoded = new HashMap<>();
    public final Map<Integer, List<Integer>> registerWrites = new HashMap<>();

    public ProtoCache(Prototype p) {
        this.p = p;
        this.EOF = p.code.length;

        for (int pc = 0; pc < p.code.length; pc++) {
            decoded.put(pc, Instruction.of(pc, p.code[pc]));
        }
        for (Instruction i : decoded.values()) {
            i.markOutgoing(decoded);
            Set<Integer> writtenRegisters = i.modifies();
            for (Integer register : writtenRegisters) {
                registerWrites.putIfAbsent(register, new ArrayList<>());
                registerWrites.get(register).add(i.pc);
            }
        }
    }

    public Instruction get(int pc) {
        return decoded.get(pc);
    }

    public int getNextWrite(int reg, int currentPC) {
        if (!registerWrites.containsKey(reg)) return EOF;
        for (int write : registerWrites.get(reg)) {
            if (write > currentPC) return write;
        }
        return EOF;
    }
}
