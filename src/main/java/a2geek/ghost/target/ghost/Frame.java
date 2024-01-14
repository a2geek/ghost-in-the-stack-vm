package a2geek.ghost.target.ghost;

import a2geek.ghost.model.DeclarationType;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.SymbolType;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;

import java.util.HashMap;
import java.util.Map;

import static a2geek.ghost.model.Symbol.in;
import static a2geek.ghost.model.Symbol.is;

public record Frame(
            Scope scope,
            Map<Symbol,Integer> offsets,
            Integer localSize,
            Integer frameSize) {

    public static Frame create(Program program) {
        Map<Symbol,Integer> varOffsets = new HashMap<>();
        int varOffset = 0;
        int reservation = 0;
        // program treats global variables as local
        for (var ref : program.findAllLocalScope(is(DeclarationType.GLOBAL).and(in(SymbolType.VARIABLE)))) {
            varOffsets.put(ref, varOffset);
            varOffset += 2;
            reservation += 2;
        }
        return new Frame(program, varOffsets, reservation, varOffset);
    }
    public static Frame create(Subroutine subroutine) {
        // FIXME these references will need to be fixed once more types are introduced
        Map<Symbol,Integer> varOffsets = new HashMap<>();
        int varOffset = 0;
        int reservation = 0;
        // local variables are at TOS
        for (var ref : subroutine.findAllLocalScope(is(DeclarationType.LOCAL).and(in(SymbolType.VARIABLE)))) {
            varOffsets.put(ref, varOffset);
            varOffset += 2;
            reservation += 2;
        }
        // frame overhead: return address (2 bytes) + stack index (2 bytes)
        varOffset += 4;
        // parameters are above the frame details
        for (var ref : subroutine.findAllLocalScope(in(SymbolType.PARAMETER))) {
            varOffsets.put(ref, varOffset);
            varOffset += 2;
        }
        if (subroutine instanceof Function fn) {
            var refs = fn.findAllLocalScope(in(SymbolType.RETURN_VALUE));
            if (refs.size() != 1) {
                throw new RuntimeException("Expecting function to have 1 return value but have " + refs.size());
            }
            for (var ref : refs) {
                varOffsets.put(ref, varOffset);
                varOffset += 2;
            }
        }
        return new Frame(subroutine, varOffsets, reservation, varOffset);
    }
}
