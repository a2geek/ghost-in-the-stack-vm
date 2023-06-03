package a2geek.ghost.target.ghost;

import a2geek.ghost.model.DataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CodeBlock {
    private List<Instruction> codeSegment = new ArrayList<>();
    private List<Instruction> dataSegment = new ArrayList<>();

    public List<Instruction> getInstructions() {
        var instructions = new ArrayList<>(codeSegment);
        instructions.addAll(dataSegment);
        return instructions;
    }
    public void emit(Opcode opcode) {
        Objects.requireNonNull(opcode, "opcode");
        codeSegment.add(new Instruction(null, opcode, null, null, null));
    }
    public void emit(Opcode opcode, Integer arg) {
        Objects.requireNonNull(opcode, "opcode");
        Objects.requireNonNull(arg, "arg");
        codeSegment.add(new Instruction(null, opcode, null, arg, null));
    }
    public void emit(String label) {
        Objects.requireNonNull(label, "label");
        codeSegment.add(new Instruction(label, null, null, null, null));
    }
    public void emit(Opcode opcode, String label) {
        Objects.requireNonNull(opcode, "opcode");
        Objects.requireNonNull(label, "label");
        codeSegment.add(new Instruction(label, opcode, null, null, null));
    }
    public String emitConstant(String suggestedLabel, String string) {
        Objects.requireNonNull(suggestedLabel, "suggestedLabel");
        Objects.requireNonNull(string, "string");
        var existing = dataSegment.stream()
                .filter(inst -> inst.directive() == Directive.CONSTANT)
                .filter(inst -> inst.constantValue().dataType() == DataType.STRING)
                .filter(inst -> string.equals(inst.constantValue().string()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.map(Instruction::label).get();
        }
        Instruction inst = new Instruction(suggestedLabel, null, Directive.CONSTANT, null,
            ConstantValue.with(string));
        dataSegment.add(inst);
        return suggestedLabel;
    }
    public String emitConstant(String suggestedLabel, List<Integer> integerArray) {
        Objects.requireNonNull(suggestedLabel, "suggestedLabel");
        Objects.requireNonNull(integerArray, "integerArray");
        var existing = dataSegment.stream()
            .filter(inst -> inst.directive() == Directive.CONSTANT)
            .filter(inst -> inst.constantValue().dataType() == DataType.INTEGER)
            .filter(inst -> integerArray.equals(inst.constantValue().integerArray()))
            .findFirst();
        if (existing.isPresent()) {
            return existing.map(Instruction::label).get();
        }
        Instruction inst = new Instruction(suggestedLabel, null, Directive.CONSTANT, null,
            ConstantValue.with(integerArray));
        dataSegment.add(inst);
        return suggestedLabel;
    }
}
