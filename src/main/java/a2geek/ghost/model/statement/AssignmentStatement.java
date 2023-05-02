package a2geek.ghost.model.statement;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.Statement;

public class AssignmentStatement implements Statement {
    private Symbol symbol;
    private Expression expr;

    public AssignmentStatement(Symbol symbol, Expression expr) {
        this.symbol = symbol;
        this.expr = expr;
        // TODO string is really being evaluated as a pointer to a string.
        expr.mustBe(DataType.INTEGER, DataType.BOOLEAN, DataType.STRING);
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public Expression getExpr() {
        return expr;
    }

    public void setExpr(Expression expr) {
        this.expr = expr;
    }

    @Override
    public String toString() {
        return String.format("%s = %s", symbol.name(), expr);
    }
}
