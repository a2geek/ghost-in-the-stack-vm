package a2geek.ghost.model.statement;

import a2geek.ghost.model.*;

public class ForStatement extends StatementBlock implements Statement {
    private Reference ref;
    private Expression start;
    private Expression end;
    private Expression step;

    public ForStatement(Reference ref, Expression start, Expression end, Expression step) {
        this.ref = ref;
        setStart(start);
        setEnd(end);
        setStep(step);
    }

    public Reference getRef() {
        return ref;
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
                ref.name(), start, end, stepText, statementsAsString(),
                ref.name());
    }
}
