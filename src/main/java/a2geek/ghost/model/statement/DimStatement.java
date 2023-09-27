package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.Symbol;

import java.util.List;

public class DimStatement implements Statement {
    private Symbol symbol;
    private Expression expr;
    private List<? extends Expression> defaultValues;

    public DimStatement(Symbol symbol, Expression expr, List<? extends Expression> defaultValues) {
        this.symbol = symbol;
        this.expr = expr;
        this.defaultValues = defaultValues;
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

    public boolean hasDefaultValues() {
        return defaultValues != null && defaultValues.size() > 0;
    }
    public List<? extends Expression> getDefaultValues() {
        return defaultValues;
    }
    public void setDefaultValues(List<Expression> defaultValues) {
        this.defaultValues = defaultValues;
    }

    @Override
    public String toString() {
        // We assume the name has an opening parenthesis
        return String.format("DIM %s(%s)", symbol.name(), expr);
    }
}
