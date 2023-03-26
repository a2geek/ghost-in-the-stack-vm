package a2geek.ghost.target.ghost;

import java.util.ArrayList;
import java.util.List;

public class CodeBlock {
    private List<Instruction> codeSegment = new ArrayList<>();
    private List<Instruction> dataSegment = new ArrayList<>();

    public List<Instruction> getInstructions() {
        var instructions = new ArrayList<>(codeSegment);
        instructions.addAll(dataSegment);
        return instructions;
    }
    public void emit(Opcode opcode) {
        codeSegment.add(new Instruction(null, opcode, null, null, null));
    }
    public void emit(Opcode opcode, Integer arg) {
        codeSegment.add(new Instruction(null, opcode, null, arg, null));
    }
    public void emit(String label) {
        codeSegment.add(new Instruction(label, null, null, null, null));
    }
    public void emit(Opcode opcode, String label) {
        codeSegment.add(new Instruction(label, opcode, null, null, null));
    }
    public String emitConstant(String suggestedLabel, String string) {
        var existing = dataSegment.stream()
                .filter(inst -> string.equals(inst.string()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.map(Instruction::label).get();
        }
        Instruction inst = new Instruction(suggestedLabel, null, Directive.STRING, null, string);
        dataSegment.add(inst);
        return suggestedLabel;
    }
}
