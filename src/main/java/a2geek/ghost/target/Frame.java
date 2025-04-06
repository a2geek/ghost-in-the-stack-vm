package a2geek.ghost.target;

import a2geek.ghost.model.*;
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
            Integer parameterSize,
            Integer localSize,
            Integer frameSize) {

    public static Frame create(Program program) {
        Map<Symbol,Integer> varOffsets = new HashMap<>();
        int varOffset = 0;
        int reservation = 0;
        // program treats global variables as local
        for (var ref : program.findAllLocalScope(is(DeclarationType.GLOBAL).and(in(SymbolType.VARIABLE)))) {
            varOffsets.put(ref, varOffset);
            varOffset += sizeOnStack(ref);
            reservation += sizeOnStack(ref);
        }
        return new Frame(program, varOffsets, 0, reservation, varOffset);
    }
    public static Frame create(Subroutine subroutine, int frameOverhead) {
        Map<Symbol,Integer> varOffsets = new HashMap<>();
        int varOffset = 0;
        int reservation = 0;
        // local variables are at TOS
        for (var ref : subroutine.findAllLocalScope(is(DeclarationType.LOCAL).and(in(SymbolType.VARIABLE)))) {
            varOffsets.put(ref, varOffset);
            varOffset += sizeOnStack(ref);
            reservation += sizeOnStack(ref);
        }
        // frame overhead varies by target
        varOffset += frameOverhead;
        // parameters are above the frame details
        int parameterSize = 0;
        for (var ref : subroutine.findAllLocalScope(in(SymbolType.PARAMETER))) {
            varOffsets.put(ref, varOffset);
            varOffset += sizeOnStack(ref);
            parameterSize += sizeOnStack(ref);
        }
        if (subroutine instanceof Function fn) {
            var refs = fn.findAllLocalScope(in(SymbolType.RETURN_VALUE));
            if (refs.size() != 1) {
                throw new RuntimeException("Expecting function to have 1 return value but have " + refs.size());
            }
            for (var ref : refs) {
                varOffsets.put(ref, varOffset);
                varOffset += sizeOnStack(ref);
            }
        }
        return new Frame(subroutine, varOffsets, parameterSize, reservation, varOffset);
    }
    public static int sizeOnStack(Symbol symbol) {
        if (symbol.numDimensions() > 0) {
            return DataType.ADDRESS.sizeof();
        }
        return symbol.dataType().sizeof();
    }
}
