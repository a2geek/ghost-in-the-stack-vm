package a2geek.ghost.target;

import a2geek.asm.api.service.AssemblerState;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.target.TargetBackend.Binary;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record AssemblerBinary(Program program, AssemblerState result) implements Binary {
    @Override
    public void writeSource(Path path) {
        try (
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
        ) {
            result.getLog().forEach(pw::println);
            // cheap address table - maybe this should be a part of the Assembler API
            result.getVariables().forEach((name,addr) -> {
                pw.printf("%04X  %s\n", addr, name);
            });
            pw.flush();
            Files.write(path, sw.toString().getBytes());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public byte[] getBytes() {
        return result().getOutput().toByteArray();
    }
}
