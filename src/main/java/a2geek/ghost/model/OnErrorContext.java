package a2geek.ghost.model;

public record OnErrorContext (Symbol gotoAddress, Symbol framePointer, Symbol stackPointer) {
    public static OnErrorContext createPrimary(Scope program) {
        var gotoAddress = program.addLocalSymbol(Symbol.variable("_ONERR_TARGET", SymbolType.VARIABLE).dataType(DataType.ADDRESS));
        var framePointer = program.addLocalSymbol(Symbol.variable("_ONERR_FP", SymbolType.VARIABLE).dataType(DataType.ADDRESS));
        var stackPointer = program.addLocalSymbol(Symbol.variable("_ONERR_SP", SymbolType.VARIABLE).dataType(DataType.ADDRESS));
        return new OnErrorContext(gotoAddress, framePointer, stackPointer);
    }
    public static OnErrorContext createCopy(Scope sub) {
        var gotoAddress = sub.addLocalSymbol(Symbol.variable("_ONERR_TARGET_COPY", SymbolType.VARIABLE).dataType(DataType.ADDRESS));
        var framePointer = sub.addLocalSymbol(Symbol.variable("_ONERR_FP_COPY", SymbolType.VARIABLE).dataType(DataType.ADDRESS));
        var stackPointer = sub.addLocalSymbol(Symbol.variable("_ONERR_SP_COPY", SymbolType.VARIABLE).dataType(DataType.ADDRESS));
        return new OnErrorContext(gotoAddress, framePointer, stackPointer);
    }
}
