package a2geek.ghost.model.statement;

import a2geek.ghost.model.Reference;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.scope.ForFrame;

public class NextStatement implements Statement {
    private static int nextNumber = 0;
    private Reference ref;
    private ForFrame frame;
    private String exitLabel;

    public NextStatement(Reference ref, ForFrame frame) {
        this.ref = ref;
        this.frame = frame;
        this.exitLabel = String.format("do_next_%s_exit%d", ref.name(), nextNumber++);
    }

    public Reference getRef() {
        return ref;
    }
    public ForFrame getFrame() {
        return frame;
    }
    public String getExitLabel() {
        return exitLabel;
    }

    @Override
    public String toString() {
        return String.format("NEXT %s", ref.name());
    }
}
