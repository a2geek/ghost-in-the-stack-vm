package a2geek.ghost.model.scope;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.Scope;
import org.javatuples.Pair;

import java.util.List;
import java.util.stream.Collectors;

public class Function extends Subroutine {
    private DataType type;
    private String exitLabel;

    public Function(Scope parent, Pair<String,DataType> func, List<Pair<String,DataType>> parameters) {
        super(parent, func.getValue0(), parameters);
        addLocalVariable(func.getValue0(), Type.RETURN_VALUE, func.getValue1());
        this.type = func.getValue1();
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
