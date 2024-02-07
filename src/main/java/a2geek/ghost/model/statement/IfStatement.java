package a2geek.ghost.model.statement;

import a2geek.ghost.model.*;

public class IfStatement implements Statement {
    private Expression expression;
    private final StatementBlock trueStatements;
    private final StatementBlock falseStatements;
    private final SourceType sourceType;

    public IfStatement(Expression expression, StatementBlock trueStatements, StatementBlock falseStatement) {
        this(expression, trueStatements, falseStatement, SourceType.CODE);
    }
    public IfStatement(Expression expression, StatementBlock trueStatements, StatementBlock falseStatement, SourceType sourceType) {
        setExpression(expression);
        this.trueStatements = trueStatements;
        this.falseStatements = falseStatement;
        this.sourceType = sourceType;
    }

    @Override
    public SourceType getSource() {
        return sourceType;
    }

    public Expression getExpression() {
        return expression;
    }
    public void setExpression(Expression expression) {
        this.expression = expression.checkAndCoerce(DataType.INTEGER);
    }

    public StatementBlock getTrueStatements() {
        return trueStatements;
    }

    public StatementBlock getFalseStatements() {
        return falseStatements;
    }

    public boolean hasTrueStatements() {
        return trueStatements != null && !trueStatements.isEmpty();
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
