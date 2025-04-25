package a2geek.ghost.target.mos6502;

import a2geek.ghost.model.scope.Program;
import a2geek.ghost.target.AssemblerCode;
import a2geek.ghost.target.TargetBackend;

public class MOS6502Backend implements TargetBackend {
    @Override
    public AssemblerCode generate(Program program) {
        CodeGenerationVisitor codeGenerationVisitor = new CodeGenerationVisitor();
        codeGenerationVisitor.visit(program);

        return new AssemblerCode(program, codeGenerationVisitor.getInstructions(), this::optimize);
    }

    public AssemblerCode optimize(AssemblerCode code) {
        return  code;
    }
}
