package a2geek.ghost.model.scope;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Reference;
import a2geek.ghost.model.Scope;

public class ForFrame {
    private static int forNumber = 0;
    private Reference varRef;
    private Reference endRef;
    private Reference stepRef;
    private String nextLabel;
    private Reference nextRef;
    private Reference exitRef;

    public ForFrame(Reference varRef, Scope scope) {
        this.varRef = varRef;
        var num = forNumber++;
        this.endRef = scope.addLocalVariable(String.format("for_%s_end%d", varRef.name(), num), varRef.dataType());
        this.stepRef = scope.addLocalVariable(String.format("for_%s_step%d", varRef.name(), num), varRef.dataType());
        this.nextLabel = String.format("do_for_%s_next%d", varRef.name(), num);
        this.nextRef = scope.addLocalVariable(String.format("for_%s_next%d", varRef.name(), num), DataType.INTEGER);
        this.exitRef = scope.addLocalVariable(String.format("for_%s_exit%d", varRef.name(), num), DataType.INTEGER);
    }

    public Reference getVarRef() {
        return varRef;
    }
    public Reference getEndRef() {
        return endRef;
    }
    public Reference getStepRef() {
        return stepRef;
    }
    public String getNextLabel() {
        return nextLabel;
    }
    public Reference getNextRef() {
        return nextRef;
    }
    public Reference getExitRef() {
        return exitRef;
    }
}
