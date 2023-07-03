package a2geek.ghost.target.ghost;


import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Objects;

public record Instruction (String label, Opcode opcode, Directive directive, Integer arg, ConstantValue constantValue) {
    public boolean isLabelOnly() {
        return label != null && opcode == null && arg == null;
    }
    public int size() {
        if (opcode != null) {
            return opcode.argc + 1;
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
        byte[] data = new byte[size()];
        data[0] = opcode.getByteCode();
        if (data.length == 1) {
            return data;
        }
        Integer value = arg;
        if (value == null) {
            value = addrs.get(label);
        }
        Objects.requireNonNull(value, "arg or label: " + this);
        switch (opcode.getArgumentCount()) {
            case 1:
                data[1] = value.byteValue();
                break;
            case 2:
                data[1] = (byte)(value & 0xff);
                data[2] = (byte)(value >> 8 & 0xff);
                break;
        }
        return data;
    }
    byte[] handleDirective(Map<String,Integer> addrs) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        switch (constantValue.constantType()) {
            case STRING_VALUE -> {
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
        else if (label == null && opcode != null && arg == null) {
            return String.format("\t%s", opcode);
        }
        else if (label == null && opcode != null && arg != null) {
            return String.format("\t%s %04X", opcode, arg & 0xffff);
        }
        else if (label != null && opcode != null && arg == null) {
            return String.format("\t%s %s", opcode, label);
        }
        else if (label != null && directive == Directive.CONSTANT) {
            return String.format("%s:\t%s %s", label, directive, constantValue);
        }
        else {
            var message = String.format("Unexpected instruction: %s %s %04d", label, opcode, arg);
            throw new RuntimeException(message);
        }
    }
}
