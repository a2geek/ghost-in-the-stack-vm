package a2geek.ghost.model.statement;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PrintStatement implements Statement {
    private List<Action> actions = new ArrayList<>();

    public List<Action> getActions() {
        return actions;
    }

    public void addNewlineAction() {
        actions.add(new PrintNewlineAction());
    }
    public void addCommaAction() {
        actions.add(new PrintCommaAction());
    }
    public void addIntegerAction(Expression expr) {
        actions.add(new PrintIntegerAction(expr));
    }
    @Override
    public String toString() {
        return "PRINT " + actions.stream().map(Action::toString).collect(Collectors.joining(""));
    }

    public interface Action {

    }
    public class PrintNewlineAction implements Action {
        @Override
        public String toString() {
            return "";
        }
    }
    public class PrintCommaAction implements Action {
        @Override
        public String toString() {
            return ",";
        }
    }
    public class PrintIntegerAction implements Action {
        private Expression expr;

        public PrintIntegerAction(Expression expr) {
            setExpr(expr);
        }

        public Expression getExpr() {
            return expr;
        }
        public void setExpr(Expression expr) {
            expr.mustBe(Expression.Type.INTEGER);
            this.expr = expr;
        }

        @Override
        public String toString() {
            return expr.toString();
        }
    }
}
