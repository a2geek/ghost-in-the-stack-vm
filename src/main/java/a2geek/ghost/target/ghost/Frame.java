package a2geek.ghost.target.ghost;

import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;

import java.util.HashMap;
import java.util.Map;

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
        for (var ref : program.findByType(Scope.Type.GLOBAL)) {
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
        for (var ref : subroutine.findByType(Scope.Type.LOCAL)) {
            varOffsets.put(ref, varOffset);
            varOffset += 2;
            reservation += 2;
        }
        // frame overhead: return address (2 bytes) + stack index (1 byte)
        varOffset += 3;
        // parameters are above the frame details
        for (var ref : subroutine.findByType(Scope.Type.PARAMETER)) {
            varOffsets.put(ref, varOffset);
            varOffset += 2;
        }
        if (subroutine instanceof Function fn) {
            var refs = fn.findByType(Scope.Type.RETURN_VALUE);
            if (refs.size() != 1) {
                throw new RuntimeException("Expecting function to have 1 return value but have " + refs.size());
            }
            for (var ref : refs) {
                varOffsets.put(ref, varOffset);
                varOffset += 2;
            }
        }
        // FIXME: We don't track/declare variables, so cannot handle globals yet.
        return new Frame(subroutine, varOffsets, reservation, varOffset);
    }
}
