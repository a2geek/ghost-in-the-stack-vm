package a2geek.ghost.model.basic.statement;

import a2geek.ghost.model.basic.DataType;
import a2geek.ghost.model.basic.Expression;
import a2geek.ghost.model.basic.Statement;
import a2geek.ghost.model.basic.Symbol;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class OnGotoGosubStatement implements Statement {
    private String op;
    private Expression expr;
    private Supplier<List<Symbol>> labelFn;
    private String altToString;

    public OnGotoGosubStatement(String op, Expression expr, Supplier<List<Symbol>> labelFn) {
        Objects.requireNonNull(op);
        Objects.requireNonNull(expr);
        Objects.requireNonNull(labelFn);

        expr.mustBe(DataType.INTEGER, DataType.BOOLEAN);

        this.op = op;
        this.expr = expr;
        this.labelFn = labelFn;
    }
    public OnGotoGosubStatement(String op, Expression expr, Supplier<List<Symbol>> labelFn, String altToString) {
        this(op, expr, labelFn);

        Objects.requireNonNull(altToString);
        this.altToString = altToString;
    }

    public String getOp() {
        return op;
    }

    public Expression getExpr() {
        return expr;
    }
    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    public List<Symbol> getLabels() {
        return labelFn.get();
    }

    @Override
    public String toString() {
        if (altToString != null) {
            return altToString;
        }
        return String.format("ON %s %s %s", expr, op.toUpperCase(),
                labelFn.get().stream().map(Symbol::name).collect(Collectors.joining(",")));
    }
}
