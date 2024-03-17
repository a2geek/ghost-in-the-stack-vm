package a2geek.ghost;

import a2geek.ghost.command.CompileCommand;
import a2geek.ghost.command.util.ExpressionConverter;
import a2geek.ghost.model.Expression;
import picocli.CommandLine;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        int exitCode = new CommandLine(new CompileCommand())
                .registerConverter(Expression.class, new ExpressionConverter())
                .execute(args);
        System.exit(exitCode);
    }
}