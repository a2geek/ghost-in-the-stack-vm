package a2geek.ghost.model.scope;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.Symbol;

import java.util.List;
import java.util.stream.Collectors;

public class Function extends Subroutine {
    private DataType type;
    private String exitLabel;

    public Function(Scope parent, Symbol.Builder func, List<Symbol.Builder> parameters) {
        super(parent, func.name(), parameters);
        addLocalSymbol(func.type(Type.RETURN_VALUE));
        this.type = func.dataType();
    }

    public DataType getType() {
        return type;
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
