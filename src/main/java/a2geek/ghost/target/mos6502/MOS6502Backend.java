package a2geek.ghost.target.mos6502;

import a2geek.asm.api.util.LineParts;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.target.AssemblerCode;
import a2geek.ghost.target.TargetBackend;

import java.util.ArrayList;
import java.util.List;

public class MOS6502Backend implements TargetBackend {
    @Override
    public Code generate(Program program) {
        // TODO these are just stubs
        List<LineParts> instructions = new ArrayList<>();
        return new AssemblerCode(program, instructions, (code) -> code);
    }
}
