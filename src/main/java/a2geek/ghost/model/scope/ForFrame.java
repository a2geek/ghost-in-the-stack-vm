package a2geek.ghost.model.scope;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.Scope;

public class ForFrame {
    private static int forNumber = 0;
    private Symbol varRef;
    private Symbol endRef;
    private Symbol stepRef;
    private Symbol nextRef;
    private Symbol exitRef;

    public ForFrame(Symbol varRef, Scope scope) {
        this.varRef = varRef;
        var num = forNumber++;
        this.endRef = scope.addLocalVariable(String.format("for_%s_end%d", varRef.name(), num), varRef.dataType());
        this.stepRef = scope.addLocalVariable(String.format("for_%s_step%d", varRef.name(), num), varRef.dataType());
        this.nextRef = scope.addLocalVariable(String.format("for_%s_next%d", varRef.name(), num), DataType.INTEGER);
        this.exitRef = scope.addLocalVariable(String.format("for_%s_exit%d", varRef.name(), num), DataType.INTEGER);
    }

    public Symbol getVarRef() {
        return varRef;
    }
    public Symbol getEndRef() {
        return endRef;
    }
    public Symbol getStepRef() {
        return stepRef;
    }
    public Symbol getNextRef() {
        return nextRef;
    }
    public Symbol getExitRef() {
        return exitRef;
    }
}
