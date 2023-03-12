package a2geek.ghost.model.code;


import java.util.Map;

public record Instruction (String label, Opcode opcode, Integer arg) {
    public boolean isLabelOnly() {
        return label != null && opcode == null && arg == null;
    }
    public int size() {
        return opcode != null ? opcode.argc + 1 : 0;
    }
    public byte[] assemble(Map<String,Integer> addrs) {
        if (opcode == null) {
            return new byte[0];
        }

        byte[] data = new byte[size()];
        data[0] = opcode.getByteCode();
        switch (opcode.getArgumentCount()) {
            case 1:
                data[1] = arg.byteValue();
                break;
            case 2:
                Integer value = arg;
                if (value == null) {
                    value = addrs.get(label);
                }
                data[1] = (byte)(value & 0xff);
                data[2] = (byte)(value >> 8 & 0xff);
                break;
        }
        return data;
    }
    @Override
    public String toString() {
        if (label != null && opcode == null && arg == null) {
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
        else {
            var message = String.format("Unexpected instruction: %s %s %04d", label, opcode, arg);
            throw new RuntimeException(message);
        }
    }
}
