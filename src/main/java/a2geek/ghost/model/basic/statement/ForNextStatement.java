package a2geek.ghost.model.basic.statement;

import a2geek.ghost.model.basic.*;

public class ForNextStatement extends StatementBlock implements Statement {
    private Symbol symbol;
    private Expression start;
    private Expression end;
    private Expression step;

    public ForNextStatement(Symbol symbol, Expression start, Expression end, Expression step) {
        this.symbol = symbol;
        setStart(start);
        setEnd(end);
        setStep(step);
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public Expression getStart() {
        return start;
    }
    public void setStart(Expression start) {
        start.mustBe(DataType.INTEGER);
        this.start = start;
    }

    public Expression getEnd() {
        return end;
    }
    public void setEnd(Expression end) {
        end.mustBe(DataType.INTEGER);
        this.end = end;
    }

    public Expression getStep() {
        return step;
    }
    public void setStep(Expression step) {
        step.mustBe(DataType.INTEGER);
        this.step = step;
    }

    @Override
    public String toString() {
        String stepText = "";
        if (step != null) {
            stepText = String.format("STEP %s ", step);
        }
        return String.format("FOR %s = %s TO %s %s: %s : NEXT %s",
                symbol.name(), start, end, stepText, statementsAsString(),
                symbol.name());
    }
}
