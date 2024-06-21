package a2geek.ghost.target.ghost;

import java.util.Map;
import java.util.Objects;

public record Instruction (String label, Opcode opcode, Directive directive, Integer arg, Integer arg2) {
    public boolean isLabelOnly() {
        return label != null && opcode == null && directive == null && arg == null;
    }
    public int size() {
        if (opcode != null) {
            return opcode.getSize();
        }
        else if (directive != null) {
            return directive.size();
        }
        return 0;
    }
    public byte[] assemble(Map<String,Integer> addrs) {
        if (opcode != null) {
            return handleOpcode(addrs);
        }
        else if (directive != null) {
            return directive.generate(addrs);
        }
        return new byte[0];
    }
    byte[] handleOpcode(Map<String,Integer> addrs) {
        Integer value = arg;
        if (value == null && label != null) {
            value = addrs.get(label);
            Objects.requireNonNull(value, "arg or label: " + this);
        }
        if (value == null) {
            return opcode.generate();
        }
        else if (arg2 == null) {
            return opcode.generate(value);
        }
        else {
            return opcode.generate(value, arg2);
        }
    }
    @Override
    public String toString() {
        if (label != null && opcode == null && directive == null && arg == null) {
            return String.format("%s:", label);
        }
        else if (opcode != null) {
            return String.format("\t%s", opcode.format(objectArray(label, arg, arg2)));
        }
        else if (directive != null) {
            return String.format("%s:\t%s", label, directive.format());
        }
        else {
            var message = String.format("Unexpected instruction: %s %s %04d", label, opcode, arg);
            throw new RuntimeException(message);
        }
    }
    private Object[] objectArray(Object ...args) {
        int n = 0;
        for (Object arg : args) {
            if (arg != null) n++;
        }
        Object[] data = new Object[n];
        n = 0;
        for (Object arg : args) {
            if (arg != null) {
                data[n++] = arg;
            }
        }
        return data;
    }
}
