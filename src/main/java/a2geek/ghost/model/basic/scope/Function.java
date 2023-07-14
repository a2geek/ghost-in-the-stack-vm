package a2geek.ghost.model.basic.scope;

import a2geek.ghost.model.basic.DataType;
import a2geek.ghost.model.basic.Scope;
import a2geek.ghost.model.basic.Symbol;

import java.util.List;
import java.util.stream.Collectors;

public class Function extends Subroutine {
    private DataType dataType;
    private String exitLabel;

    public Function(Scope parent, Symbol.Builder func, List<Symbol.Builder> parameters) {
        super(parent, func.name(), parameters);
        addLocalSymbol(func.type(Type.RETURN_VALUE));
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
                findByType(Type.PARAMETER).stream()
                        .map(Symbol::name)
                        .collect(Collectors.joining(", ")),
                statementsAsString());
    }
}
