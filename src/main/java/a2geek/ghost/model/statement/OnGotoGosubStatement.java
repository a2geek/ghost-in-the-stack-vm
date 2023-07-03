package a2geek.ghost.model.statement;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

import java.util.List;
import java.util.Objects;

public class OnGotoGosubStatement implements Statement {
    private String op;
    private Expression expr;
    private List<String> labels;
    private String altToString;

    public OnGotoGosubStatement(String op, Expression expr, List<String> labels) {
        Objects.requireNonNull(op);
        Objects.requireNonNull(expr);
        Objects.requireNonNull(labels);

        expr.mustBe(DataType.INTEGER, DataType.BOOLEAN);

        this.op = op;
        this.expr = expr;
        this.labels = labels;
    }
    public OnGotoGosubStatement(String op, Expression expr, List<String> labels, String altToString) {
        this(op, expr, labels);

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

    public List<String> getLabels() {
        return labels;
    }

    @Override
    public String toString() {
        if (altToString != null) {
            return altToString;
        }
        return String.format("ON %s %s %s", expr, op.toUpperCase(), String.join(",", labels));
    }
}
