package a2geek.ghost.target.ghost;

import a2geek.ghost.model.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CodeBlock {
    private final List<Instruction> codeSegment = new ArrayList<>();
    private final List<Instruction> dataSegment = new ArrayList<>();

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
                .filter(inst -> inst.directive() != null)
                .filter(inst -> inst.directive().type() == Directive.Type.STRING_VALUE)
                .filter(inst -> string.equals(inst.directive().array().getFirst()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.map(Instruction::label).orElseThrow();
        }
        Instruction inst = new Instruction(suggestedLabel, null,
            Directive.with(string), null, null);
        dataSegment.add(inst);
        return suggestedLabel;
    }
    public String emitConstant(String suggestedLabel, List<Integer> integerArray) {
        Objects.requireNonNull(suggestedLabel, "suggestedLabel");
        Objects.requireNonNull(integerArray, "integerArray");
        var existing = dataSegment.stream()
            .filter(inst -> inst.directive() != null)
            .filter(inst -> inst.directive().type() == Directive.Type.INTEGER_ARRAY)
            .filter(inst -> integerArray.equals(inst.directive().array()))
            .findFirst();
        if (existing.isPresent()) {
            return existing.map(Instruction::label).orElseThrow();
        }
        Instruction inst = new Instruction(suggestedLabel, null,
            Directive.with(integerArray), null, null);
        dataSegment.add(inst);
        return suggestedLabel;
    }
    public String emitConstantLabels(String suggestedLabel, List<Symbol> labelArray) {
        Objects.requireNonNull(suggestedLabel, "suggestedLabel");
        Objects.requireNonNull(labelArray, "labelArray");
        var stringArray = labelArray.stream().map(Symbol::name).toList();
        var existing = dataSegment.stream()
                .filter(inst -> inst.directive() != null)
                .filter(inst -> inst.directive().type() == Directive.Type.LABEL_ARRAY_LESS_1)
                .filter(inst -> stringArray.equals(inst.directive().array()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.map(Instruction::label).orElseThrow();
        }
        Instruction inst = new Instruction(suggestedLabel, null,
            Directive.withLabels(stringArray), null, null);
        dataSegment.add(inst);
        return suggestedLabel;
    }
}
