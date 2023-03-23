package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.StatementBlock;

public class ForStatement extends StatementBlock implements Statement {
    private String id;
    private Expression start;
    private Expression end;
    private Expression step;

    public ForStatement(String id, Expression start, Expression end, Expression step) {
        this.id = id;
        setStart(start);
        setEnd(end);
        setStep(step);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Expression getStart() {
        return start;
    }
    public void setStart(Expression start) {
        start.mustBe(Expression.Type.INTEGER);
        this.start = start;
    }

    public Expression getEnd() {
        return end;
    }
    public void setEnd(Expression end) {
        end.mustBe(Expression.Type.INTEGER);
        this.end = end;
    }

    public Expression getStep() {
        return step;
    }
    public void setStep(Expression step) {
        step.mustBe(Expression.Type.INTEGER);
        this.step = step;
    }

    @Override
    public String toString() {
        String stepText = "";
        if (step != null) {
            stepText = String.format("STEP %s ", step);
        }
        return String.format("FOR %s = %s TO %s %s: %s : NEXT %s",
                id, start, end, stepText, statementsAsString(), id);
    }
}
