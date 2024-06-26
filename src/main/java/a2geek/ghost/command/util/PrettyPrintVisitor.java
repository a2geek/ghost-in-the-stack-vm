package a2geek.ghost.command.util;

import a2geek.ghost.model.*;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.IfStatement;
import a2geek.ghost.model.statement.LabelStatement;

import java.util.List;
import java.util.stream.Collectors;

import static a2geek.ghost.model.Symbol.in;
import static a2geek.ghost.model.Symbol.is;
import static java.util.function.Predicate.not;

public class PrettyPrintVisitor {
    public static String format(Program program) {
        return format(program, config());
    }
    public static String format(Program program, Config config) {
        PrettyPrintVisitor visitor = new PrettyPrintVisitor(config, 4);
        visitor.formatProgram(program);
        return visitor.sb.toString();
    }

    private final Config config;
    private int indent = 0;
    private final int indentIncrement;
    private final StringBuilder sb = new StringBuilder();

    private PrettyPrintVisitor(Config config, int indentIncrement) {
        this.config = config;
        this.indentIncrement = indentIncrement;
    }

    public void formatProgram(Program program) {
        sb.append(String.format("%s:", program.getName()).indent(0));
        indent += indentIncrement;
        formatScope(program);
        formatStatementBlock(program);
        sb.append("\n");
        indent -= indentIncrement;
        program.findAllLocalScope(in(SymbolType.FUNCTION,SymbolType.SUBROUTINE).and(not(is(DeclarationType.INTRINSIC)))).forEach(symbol -> {
            if (!config.includeModules && symbol.name().contains(".")) {
                return;
            }
            dispatch(symbol.scope());
        });
    }

    public void dispatch(Scope scope) {
        switch (scope) {
            case Function func -> formatFunction(func);
            case Subroutine sub -> formatSubroutine(sub);
            default -> throw new RuntimeException("unexpected scope type: " + scope.getClass().getName());
        }
    }

    public void formatFunction(Function func) {
        sb.append(String.format("FUNCTION %s(%s) AS %s", func.getFullPathName(),
                formatParameters(func), func.getDataType()).indent(0));
        indent += indentIncrement;
        formatScope(func);
        formatStatementBlock(func);
        indent -= indentIncrement;
        sb.append("END FUNCTION".indent(0));
        sb.append("\n");
    }

    public void formatSubroutine(Subroutine sub) {
        sb.append(String.format("SUB %s(%s)", sub.getFullPathName(), formatParameters(sub)).indent(0));
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
                .filter(s -> s.symbolType() == SymbolType.VARIABLE)
                .filter(s -> s.declarationType() != DeclarationType.INTRINSIC)
                .forEach(s -> {
                    var fmt = String.format("DIM %s AS %s = %s", s.name(), s.dataType(), s.defaultValues()).indent(indent);
                    sb.append(fmt);
                });
    }

    public void formatStatementBlock(StatementBlock block) {
        formatStatementList(block.getInitializationStatements());
        formatStatementList(block.getStatements());
    }
    public void formatStatementList(List<Statement> statements) {
        for (int i=0; i<statements.size(); i++) {
            sb.append(String.format("%4d: ", i));
            formatStatement(statements.get(i));
        }
    }
    public void formatStatement(Statement statement) {
        switch (statement) {
            case LabelStatement label -> {
                sb.append(String.format("%s:", label.getLabel().name()).indent(0));
            }
            case IfStatement ifStatement -> {
                var postfix = "";
                if (ifStatement.getSource() != SourceType.CODE) {
                    postfix = " **";
                }
                sb.append(String.format("IF %s THEN%s", ifStatement.getExpression(), postfix).indent(indent));
                indent += indentIncrement;
                formatStatementBlock(ifStatement.getTrueStatements());
                if (ifStatement.hasFalseStatements()) {
                    sb.append("ELSE".indent(indent - indentIncrement + 6));
                    formatStatementBlock(ifStatement.getFalseStatements());
                }
                indent -= indentIncrement;
                sb.append("END IF".indent(indent + 6));
            }
            default -> {
                sb.append(statement.toString().indent(indent));
            }
        }
    }

    public String formatParameters(Scope scope) {
        return scope.getLocalSymbols().stream()
                .filter(s -> s.symbolType() == SymbolType.PARAMETER)
                .map(s -> {
                    if (s.defaultValues() == null || s.defaultValues().isEmpty()) {
                        return String.format("%s %s AS %s", s.passingMode(), s.name(), s.dataType());
                    }
                    return String.format("%s %s AS %s = %s", s.passingMode(), s.name(), s.dataType(),
                        s.defaultValues().getFirst());
                })
                .collect(Collectors.joining(", "));
    }

    public static Config config() {
        return new Config();
    }

    public static class Config {
        private boolean includeModules = true;

        public Config includeModules(boolean flag) {
            this.includeModules = flag;
            return this;
        }
    }
}
