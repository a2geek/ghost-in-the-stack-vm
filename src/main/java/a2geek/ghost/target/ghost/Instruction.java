package a2geek.ghost.target.ghost;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Objects;

public record Instruction (String label, Opcode opcode, Directive directive, Integer arg, Integer arg2, ConstantValue constantValue) {
    public boolean isLabelOnly() {
        return label != null && opcode == null && directive == null && arg == null && constantValue == null;
    }
    public int size() {
        if (opcode != null) {
            return opcode.getSize();
        }
        else if (directive == Directive.CONSTANT) {
            return constantValue.size();
        }
        return 0;
    }
    public byte[] assemble(Map<String,Integer> addrs) {
        if (opcode != null) {
            return handleOpcode(addrs);
        }
        else if (directive != null) {
            return handleDirective(addrs);
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
    byte[] handleDirective(Map<String,Integer> addrs) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        switch (constantValue.constantType()) {
            case STRING_VALUE -> {
                bytes.write(constantValue.string().length());
                for (byte ch : constantValue.string().getBytes()) {
                    bytes.write(ch|0x80);
                }
                bytes.write(0);
            }
            case INTEGER_ARRAY -> {
                // BASIC arrays ignore the zeroth element, so 1,2,3,4 is length of 3
                int size = constantValue.integerArray().size()-1;
                bytes.write(size & 0xff);
                bytes.write(size >> 8 & 0xff);
                for (int value : constantValue.integerArray()) {
                    bytes.write(value & 0xff);
                    bytes.write(value >> 8 & 0xff);
                }
            }
            case LABEL_ARRAY_LESS_1 -> {
                // BASIC arrays ignore the zeroth element, so 1,2,3,4 is length of 3
                int size = constantValue.stringArray().size()-1;
                bytes.write(size & 0xff);
                bytes.write(size >> 8 & 0xff);
                for (String label : constantValue.stringArray()) {
                    Integer value = addrs.get(label);
                    if (value == null) {
                        throw new RuntimeException("label not found: " + label);
                    }
                    value = value - 1;
                    bytes.write(value & 0xff);
                    bytes.write(value >> 8 & 0xff);
                }
            }
            default -> {
                throw new RuntimeException("Unsupported directive: " + directive);
            }
        }
        return bytes.toByteArray();
    }
    @Override
    public String toString() {
        if (label != null && opcode == null && directive == null && arg == null) {
            return String.format("%s:", label);
        }
        else if (opcode != null) {
            return String.format("\t%s", opcode.format(objectArray(label, arg, arg2)));
        }
        else if (label != null && directive == Directive.CONSTANT) {
            return String.format("%s:\t%s %s", label, directive, constantValue);
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
