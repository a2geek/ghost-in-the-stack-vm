package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.StatementBlock;

public class DoLoopStatement extends StatementBlock implements Statement {
    private Operation op;
    private Expression expr;

    public DoLoopStatement(Operation op, Expression expr) {
        this.op = op;
        this.expr = expr;
    }

    public Operation getOp() {
        return op;
    }

    public Expression getExpr() {
        return expr;
    }
    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    @Override
    public String toString() {
        var fmt = switch (op) {
            case WHILE -> "WHILE %1$s : %2$s : END WHILE";
            case DO_WHILE -> "DO WHILE %1$s : %2$s : LOOP";
            case DO_UNTIL -> "DO UNTIL %1$s : %2$s : LOOP";
            case LOOP_WHILE -> "DO : %2$s : LOOP WHILE %1$s";
            case LOOP_UNTIL -> "DO : %2$s : LOOP UNTIL %1$s";
        };
        return String.format(fmt, expr.toString(), statementsAsString());
    }

    public enum Operation {
        WHILE,
        DO_WHILE,
        DO_UNTIL,
        LOOP_WHILE,
        LOOP_UNTIL
    }
}
