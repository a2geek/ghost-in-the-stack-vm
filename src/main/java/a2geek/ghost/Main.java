package a2geek.ghost;

import a2geek.ghost.command.CompileCommand;
import picocli.CommandLine;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        int exitCode = new CommandLine(new CompileCommand()).execute(args);
        System.exit(exitCode);
    }
}