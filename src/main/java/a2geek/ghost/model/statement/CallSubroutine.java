package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

import java.util.List;
import java.util.stream.Collectors;

public class CallSubroutine implements Statement {
    private String name;
    private List<Expression> parameters;

    public CallSubroutine(String name, List<Expression> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public List<Expression> getParameters() {
        return parameters;
    }
    public void setParameters(List<Expression> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return String.format("CALL %s(%s)", name,
                parameters.stream().map(Expression::toString).collect(Collectors.joining(", ")));
    }
}
