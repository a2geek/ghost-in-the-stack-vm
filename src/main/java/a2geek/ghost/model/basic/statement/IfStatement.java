package a2geek.ghost.model.basic.statement;

import a2geek.ghost.model.basic.DataType;
import a2geek.ghost.model.basic.Expression;
import a2geek.ghost.model.basic.Statement;
import a2geek.ghost.model.basic.StatementBlock;

public class IfStatement implements Statement {
    private Expression expression;
    private StatementBlock trueStatements;
    private StatementBlock falseStatements;

    public IfStatement(Expression expression, StatementBlock trueStatements, StatementBlock falseStatement) {
        setExpression(expression);
        this.trueStatements = trueStatements;
        this.falseStatements = falseStatement;
    }

    public Expression getExpression() {
        return expression;
    }
    public void setExpression(Expression expression) {
        expression.mustBe(DataType.INTEGER, DataType.BOOLEAN);
        this.expression = expression;
    }

    public StatementBlock getTrueStatements() {
        return trueStatements;
    }

    public StatementBlock getFalseStatements() {
        return falseStatements;
    }

    public boolean hasFalseStatements() {
        return falseStatements != null && !falseStatements.isEmpty();
    }

    @Override
    public String toString() {
        String elseSegment = "";
        if (hasFalseStatements()) {
            elseSegment = String.format(": ELSE %s ", falseStatements.statementsAsString());
        }
        return String.format("IF %s THEN %s %s : END IF", expression.toString(),
            trueStatements.statementsAsString(), elseSegment);
    }
}
