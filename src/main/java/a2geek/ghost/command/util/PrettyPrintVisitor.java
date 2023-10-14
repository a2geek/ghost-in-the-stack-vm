package a2geek.ghost.command.util;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.StatementBlock;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.IfStatement;
import a2geek.ghost.model.statement.LabelStatement;

import java.util.stream.Collectors;

public class PrettyPrintVisitor {
    public static String format(Program program) {
        PrettyPrintVisitor visitor = new PrettyPrintVisitor(4);
        visitor.formatProgram(program);
        return visitor.sb.toString();
    }

    private int indent = 0;
    private final int indentIncrement;
    private StringBuilder sb = new StringBuilder();

    private PrettyPrintVisitor(int indentIncrement) {
        this.indentIncrement = indentIncrement;
    }

    public void formatProgram(Program program) {
        sb.append(String.format("%s:", program.getName()).indent(0));
        indent += indentIncrement;
        formatScope(program);
        formatStatementBlock(program);
        sb.append("\n");
        indent -= indentIncrement;
        program.getScopes().forEach(this::dispatch);
    }

    public void dispatch(Scope scope) {
        if (scope instanceof Function func) {
            formatFunction(func);
        }
        else if (scope instanceof Subroutine sub) {
            formatSubroutine(sub);
        }
        else {
            throw new RuntimeException("unexpected scope type: " + scope.getClass().getName());
        }
    }

    public void formatFunction(Function func) {
        sb.append(String.format("FUNCTION %s(%s) AS %s", func.getName(),
                formatParameters(func), func.getDataType()).indent(0));
        indent += indentIncrement;
        formatScope(func);
        formatStatementBlock(func);
        indent -= indentIncrement;
        sb.append("END FUNCTION".indent(0));
        sb.append("\n");
    }

    public void formatSubroutine(Subroutine sub) {
        sb.append(String.format("SUB %s(%s)", sub.getName(), formatParameters(sub)).indent(0));
        indent += indentIncrement;
        formatScope(sub);
        formatStatementBlock(sub);
        indent -= indentIncrement;
        sb.append("END SUB".indent(0));
        sb.append("\n");
    }

    public void formatScope(Scope scope) {
        scope.getLocalSymbols().stream()
                // Only variables
                .filter(s -> s.type() == Scope.Type.GLOBAL || s.type() == Scope.Type.LOCAL)
                // Strings should have their own DIM statement at this time
                .filter(s -> s.dataType() != DataType.STRING)
                // Arrays should have their own DIM statement
                .filter(s -> s.numDimensions() == 0)
                .forEach(s -> {
                    var fmt = String.format("DIM %s AS %s", s.name(), s.dataType()).indent(indent);
                    sb.append(fmt);
                });
    }

    public void formatStatementBlock(StatementBlock block) {
        block.getStatements().forEach(statement -> {
            if (statement instanceof LabelStatement label) {
                sb.append(String.format("%s:", label.getLabel().name()).indent(0));
            }
            else if (statement instanceof IfStatement ifStatement) {
                sb.append(String.format("IF %s THEN", ifStatement.getExpression()).indent(indent));
                indent += indentIncrement;
                formatStatementBlock(ifStatement.getTrueStatements());
                if (ifStatement.hasFalseStatements()) {
                    sb.append("ELSE".indent(indent - indentIncrement));
                    formatStatementBlock(ifStatement.getFalseStatements());
                }
                indent -= indentIncrement;
                sb.append("END IF".indent(indent));
            }
            else {
                sb.append(statement.toString().indent(indent));
            }
        });
    }

    public String formatParameters(Scope scope) {
        return scope.getLocalSymbols().stream()
                .filter(s -> s.type() == Scope.Type.PARAMETER)
                .map(s -> String.format("%s AS %s", s.name(), s.dataType()))
                .collect(Collectors.joining(", "));
    }
}
