package a2geek.ghost.model;

import a2geek.ghost.model.expression.BinaryExpression;
import a2geek.ghost.model.expression.IdentifierExpression;
import a2geek.ghost.model.expression.IntegerConstant;
import a2geek.ghost.model.statement.*;

public interface Visitor {
    default void visit(AssignmentStatement statement) {}
    default void visit(ColorStatement statement) {}
    default void visit(EndStatement statement) {}
    default void visit(ForStatement statement) {}
    default void visit(GrStatement statement) {}
    default void visit(PlotStatement statement) {}

    default void visit(BinaryExpression expression) {}
    default void visit(IdentifierExpression expression) {}
    default void visit(IntegerConstant expression) {}
}
