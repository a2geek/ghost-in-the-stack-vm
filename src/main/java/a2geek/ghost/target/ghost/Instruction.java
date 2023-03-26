package a2geek.ghost.target.ghost;


import java.io.ByteArrayOutputStream;
import java.util.Map;

public record Instruction (String label, Opcode opcode, Directive directive, Integer arg, String string) {
    public boolean isLabelOnly() {
        return label != null && opcode == null && arg == null;
    }
    public int size() {
        if (opcode != null) {
            return opcode.argc + 1;
        }
        else if (directive != null) {
            return string.length() + 1;
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
        Integer value = arg;
        if (value == null) {
            value = addrs.get(label);
        }
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
        switch (directive) {
            case STRING -> {
                for (byte ch : string.getBytes()) {
                    bytes.write(ch|0x80);
                }
                bytes.write(0);
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
        else if (label != null && directive != null && string != null) {
            return String.format("%s:\t%s \"%s\"", label, directive, string);
        }
        else {
            var message = String.format("Unexpected instruction: %s %s %04d", label, opcode, arg);
            throw new RuntimeException(message);
        }
    }
}
