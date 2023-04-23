package a2geek.ghost.model.statement;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Reference;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.scope.ForFrame;

public class ForStatement implements Statement {
    private Reference ref;
    private Expression start;
    private Expression end;
    private Expression step;
    private ForFrame frame;

    public ForStatement(Reference ref, Expression start, Expression end, Expression step, ForFrame frame) {
        this.ref = ref;
        this.frame = frame;
        setStart(start);
        setEnd(end);
        setStep(step);
    }

    public Reference getRef() {
        return ref;
    }
    public ForFrame getFrame() {
        return frame;
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
        return String.format("FOR %s = %s TO %s %s",
                ref.name(), start, end, stepText);
    }
}
