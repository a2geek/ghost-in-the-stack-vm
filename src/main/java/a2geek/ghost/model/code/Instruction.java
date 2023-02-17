package a2geek.ghost.model.code;


public record Instruction (String label, Opcode opcode, Integer arg) {
    @Override
    public String toString() {
        if (label != null && opcode == null && arg == null) {
            return String.format("%s:", label);
        }
        else if (label == null && opcode != null && arg == null) {
            return String.format("\t%s", opcode);
        }
        else if (label == null && opcode != null && arg != null) {
            return String.format("\t%s %04X", opcode, arg);
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
