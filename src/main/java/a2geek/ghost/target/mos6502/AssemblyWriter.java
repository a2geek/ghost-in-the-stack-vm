package a2geek.ghost.target.mos6502;

import a2geek.asm.api.util.LineParts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AssemblyWriter {
    private final List<LineParts> codeSegment = new ArrayList<>();
    private final List<LineParts> dataSegment = new ArrayList<>();

    public List<LineParts> getInstructions() {
        List<LineParts> instructions = new ArrayList<>(codeSegment);
        instructions.addAll(dataSegment);
        return instructions;
    }

    // internal helpers
    private AssemblyWriter emit(List<LineParts> segment, String label, String opcode, String fmt, Object... args) {
        LineParts parts = new LineParts();
        parts.setLabel(label);
        parts.setOpcode(opcode);
        if (args.length > 0 && args[0] instanceof Integer i) {
            // we need to truncate hex digits
            if (fmt.toLowerCase().contains("%04x")) {
                args[0] = i & 0xffff;
            }
            else if (fmt.toLowerCase().contains("%02x")) {
                args[0] = i & 0xff;
            }
        }
        String value = fmt;
        if (fmt != null) {
            value = String.format(fmt, args);
        }
        parts.setExpression(value);
        segment.add(parts);
        return this;
    }

    public AssemblyWriter comment(String fmt, Object... args) {
        LineParts parts = new LineParts();
        parts.setComment(String.format(fmt, args));
        codeSegment.add(parts);
        return this;
    }

    public AssemblyWriter op(String opcode) {
        return emit(codeSegment, null, opcode, null);
    }
    public AssemblyWriter op(String opcode, String fmt, Object... args) {
        return emit(codeSegment, null, opcode, fmt, args);
    }

    public AssemblyWriter label(String label) {
        return emit(codeSegment, label, null, null);
    }
    public AssemblyWriter assign(String label, String value) {
        return emit(codeSegment, label, "=", value);
    }
    public AssemblyWriter cpu(String cpu) {
        return op(".cpu", cpu);
    }
    public AssemblyWriter org(int addr) {
        return op(".org", "$%x", addr);
    }

    public AssemblyWriter TSX() {
        return op("TSX");
    }
    public AssemblyWriter TXS() {
        return op("TXS");
    }
    public AssemblyWriter RTS() {
        return op("RTS");
    }
    public AssemblyWriter SEC() {
        return op("SEC");
    }
    public AssemblyWriter PLA() {
        return op("PLA");
    }
    public AssemblyWriter PHA() {
        return op("PHA");
    }
    public AssemblyWriter INY() {
        return op("INY");
    }
    public AssemblyWriter CLC() {
        return op("CLC");
    }

    public AssemblyWriter ADC(String fmt, Object... args) {
        return op("ADC", fmt, args);
    }
    public AssemblyWriter LDA(String fmt, Object... args) {
        return op("LDA", fmt, args);
    }
    public AssemblyWriter SBC(String fmt, Object... args) {
        return op("SBC", fmt, args);
    }
    public AssemblyWriter EOR(String fmt, Object... args) {
        return op("EOR", fmt, args);
    }
    public AssemblyWriter STA(String fmt, Object... args) {
        return op("STA", fmt, args);
    }
    public AssemblyWriter STX(String fmt, Object... args) {
        return op("STX", fmt, args);
    }
    public AssemblyWriter LDX(String fmt, Object... args) {
        return op("LDX", fmt, args);
    }
    public AssemblyWriter LDY(String fmt, Object... args) {
        return op("LDY", fmt, args);
    }
    public AssemblyWriter STY(String fmt, Object... args) {
        return op("STY", fmt, args);
    }
    public AssemblyWriter JSR(String fmt, Object... args) {
        return op("JSR", fmt, args);
    }
    public AssemblyWriter JMP(String fmt, Object... args) {
        return op("JMP", fmt, args);
    }
    public AssemblyWriter ORA(String fmt, Object... args) {
        return op("ORA", fmt, args);
    }
    public AssemblyWriter BEQ(String fmt, Object... args) {
        return op("BEQ", fmt, args);
    }
    public AssemblyWriter BNE(String fmt, Object... args) {
        return op("BNE", fmt, args);
    }

    public AssemblyWriter byteVar(String label, String fmt, Object... args) {
        return emit(dataSegment, label, ".byte", fmt, args);
    }
    public AssemblyWriter wordVar(String label, String fmt, Object... args) {
        // TODO likely works, but maybe update Assembler directives to include .word?
        return emit(dataSegment, label, ".addr", fmt, args);
    }
    public String stringConstant(String suggestedLabel, String value) {
        Objects.requireNonNull(suggestedLabel, "suggestedLabel");
        Objects.requireNonNull(value, "value");
        // TODO can we escape quotes that are legitimately in the string?
        String quoted = String.format("\"%s\"", value);
        var existing = dataSegment.stream()
                .filter(parts -> ".string".equals(parts.getOpcode()))
                .filter(parts -> quoted.equals(parts.getExpression()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.map(LineParts::getLabel).orElseThrow();
        }
        emit(dataSegment, suggestedLabel, ".string", quoted);
        return suggestedLabel;
    }
}
