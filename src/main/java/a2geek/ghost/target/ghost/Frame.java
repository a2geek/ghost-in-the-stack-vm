package a2geek.ghost.target.ghost;

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
            Integer localSize,
            Integer frameSize) {

    public static Frame create(Program program) {
        Map<Symbol,Integer> varOffsets = new HashMap<>();
        int varOffset = 0;
        int reservation = 0;
        // program treats global variables as local
        for (var ref : program.findAllLocalScope(is(DeclarationType.GLOBAL).and(in(SymbolType.VARIABLE)))) {
            varOffsets.put(ref, varOffset);
            varOffset += sizeOnHeap(ref);
            reservation += sizeOnHeap(ref);
        }
        return new Frame(program, varOffsets, reservation, varOffset);
    }
    public static Frame create(Subroutine subroutine) {
        Map<Symbol,Integer> varOffsets = new HashMap<>();
        int varOffset = 0;
        int reservation = 0;
        // local variables are at TOS
        for (var ref : subroutine.findAllLocalScope(is(DeclarationType.LOCAL).and(in(SymbolType.VARIABLE)))) {
            varOffsets.put(ref, varOffset);
            varOffset += sizeOnHeap(ref);
            reservation += sizeOnHeap(ref);
        }
        // frame overhead: return address (2 bytes) + stack index (2 bytes)
        varOffset += DataType.ADDRESS.sizeof() * 2;
        // parameters are above the frame details
        for (var ref : subroutine.findAllLocalScope(in(SymbolType.PARAMETER))) {
            varOffsets.put(ref, varOffset);
            varOffset += sizeOnHeap(ref);
        }
        if (subroutine instanceof Function fn) {
            var refs = fn.findAllLocalScope(in(SymbolType.RETURN_VALUE));
            if (refs.size() != 1) {
                throw new RuntimeException("Expecting function to have 1 return value but have " + refs.size());
            }
            for (var ref : refs) {
                varOffsets.put(ref, varOffset);
                varOffset += sizeOnHeap(ref);
            }
        }
        return new Frame(subroutine, varOffsets, reservation, varOffset);
    }
    public static int sizeOnHeap(Symbol symbol) {
        if (symbol.numDimensions() > 0) {
            return DataType.ADDRESS.sizeof();
        }
        return symbol.dataType().sizeof();
    }
}
