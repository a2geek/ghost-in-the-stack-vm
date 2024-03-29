package a2geek.ghost.model.scope;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.SymbolType;

import java.util.function.BiFunction;

public class ForFrame {
    private final Symbol varRef;
    private final Symbol endRef;
    private final Symbol stepRef;
    private final Symbol nextRef;
    private final Symbol exitRef;

    public ForFrame(Symbol varRef, Scope scope) {
        this.varRef = varRef;
        var num = Scope.nextSymbolNumber();
        BiFunction<String,DataType,Symbol.Builder> mkBuilder = (fmt, dt) -> Symbol.variable(String.format(fmt, varRef.name(), num), SymbolType.VARIABLE).dataType(dt);
        this.endRef = scope.addLocalSymbol(mkBuilder.apply("for_%s_end%d", varRef.dataType()));
        this.stepRef = scope.addLocalSymbol(mkBuilder.apply("for_%s_step%d", varRef.dataType()));
        this.nextRef = scope.addLocalSymbol(mkBuilder.apply("for_%s_next%d", DataType.ADDRESS));
        this.exitRef = scope.addLocalSymbol(mkBuilder.apply("for_%s_exit%d", DataType.ADDRESS));
    }

    public Symbol getVarRef() {
        return varRef;
    }
    public Symbol getEndRef() {
        return endRef;
    }
    public Symbol getStepRef() {
        return stepRef;
    }
    public Symbol getNextRef() {
        return nextRef;
    }
    public Symbol getExitRef() {
        return exitRef;
    }
}
