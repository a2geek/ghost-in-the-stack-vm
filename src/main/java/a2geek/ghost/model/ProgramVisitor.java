package a2geek.ghost.model;

import a2geek.ghost.model.scope.Program;

public interface ProgramVisitor {
    void visit(Program program);
}
