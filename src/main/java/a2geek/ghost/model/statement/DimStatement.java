package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.Symbol;

public class DimStatement implements Statement {
    private Symbol symbol;
    private Expression expr;

    public DimStatement(Symbol symbol, Expression expr) {
        this.symbol = symbol;
        this.expr = expr;
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
        // We assume the name has an opening parenthesis
        return String.format("DIM %s(%s)", symbol.name(), expr);
    }
}
