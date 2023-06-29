package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

import java.util.List;

public class OnGotoGosubStatement implements Statement {
    private String op;
    private Expression expr;
    private List<String> labels;

    public OnGotoGosubStatement(String op, Expression expr, List<String> labels) {
        this.op = op;
        this.expr = expr;
        this.labels = labels;
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
        return String.format("ON %s %s %s", expr, op.toUpperCase(), String.join(",", labels));
    }
}
