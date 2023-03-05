package a2geek.ghost;

import java.io.IOException;

import a2geek.ghost.command.CompileCommand;
import picocli.CommandLine;

public class Main {
    public static int main(String[] args) throws IOException {
        int exitCode = new CommandLine(new CompileCommand()).execute(args);
        return exitCode;
    }
}