package a2geek.ghost.model.code;

import java.util.ArrayList;
import java.util.List;

public class CodeBlock {
    private List<Instruction> instructions = new ArrayList<>();

    public List<Instruction> getInstructions() {
        return instructions;
    }
    public void emit(Opcode opcode) {
        instructions.add(new Instruction(null, opcode, null));
    }
    public void emit(Opcode opcode, Integer arg) {
        instructions.add(new Instruction(null, opcode, arg));
    }
    public void emit(String label) {
        instructions.add(new Instruction(label, null, null));
    }
    public void emit(Opcode opcode, String label) {
        instructions.add(new Instruction(label, opcode, null));
    }
}
