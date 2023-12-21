package a2geek.ghost.model.scope;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.SymbolType;

import java.util.List;
import java.util.stream.Collectors;

import static a2geek.ghost.model.Symbol.in;

public class Function extends Subroutine {
    private DataType dataType;
    private String exitLabel;

    public Function(Scope parent, Symbol.Builder func, List<Symbol.Builder> parameters) {
        super(parent, func.name(), parameters);
        addLocalSymbol(func.symbolType(SymbolType.RETURN_VALUE));
        this.dataType = func.dataType();
    }

    public DataType getDataType() {
        return dataType;
    }

    public String getExitLabel() {
        return exitLabel;
    }

    public void setExitLabel(String exitLabel) {
        this.exitLabel = exitLabel;
    }

    @Override
    public String toString() {
        return String.format("FUNCTION %s(%s) : %s : END FUNCTION", getName(),
                findAllLocalScope(in(SymbolType.PARAMETER)).stream()
                        .map(Symbol::name)
                        .collect(Collectors.joining(", ")),
                statementsAsString());
    }
}
