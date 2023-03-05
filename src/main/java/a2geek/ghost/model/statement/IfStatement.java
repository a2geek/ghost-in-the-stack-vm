package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.StatementBlock;

public class IfStatement implements Statement {
    private Expression expression;
    private StatementBlock trueStatements;
    private StatementBlock falseStatements;

    public IfStatement(Expression expression, StatementBlock trueStatements, StatementBlock falseStatement) {
        this.expression = expression;
        this.trueStatements = trueStatements;
        this.falseStatements = falseStatement;
    }

    public Expression getExpression() {
        return expression;
    }
    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public StatementBlock getTrueStatements() {
        return trueStatements;
    }

    public StatementBlock getFalseStatements() {
        return falseStatements;
    }

    @Override
    public String toString() {
        return String.format("IF %s THEN %s : ELSE %s : END IF", expression.toString(),
            trueStatements.statementsAsString(), falseStatements.statementsAsString());
    }
}
