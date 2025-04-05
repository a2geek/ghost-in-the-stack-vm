package a2geek.ghost.target;

import a2geek.asm.api.service.AssemblerService;
import a2geek.asm.api.service.AssemblerState;
import a2geek.asm.api.util.AssemblerException;
import a2geek.asm.api.util.LineParts;
import a2geek.asm.api.util.Sources;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.target.TargetBackend.Code;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public record AssemblerCode(Program program, List<LineParts> instructions, Function<AssemblerCode,AssemblerCode> optimizer) implements Code {
    @Override
    public Code optimize(TargetBackend.OptimizationFlags optimizations) {
        return optimizer.apply(this);
    }

    @Override
    public TargetBackend.Binary assemble() {
        AssemblerState result = null;
        try {
            // TODO - Assembler API likely should allow the List<LineParts> directly.
            String source = instructions.stream().map(LineParts::toString).collect(Collectors.joining("\n"));
            result = AssemblerService.assemble(Sources.get(source));
        } catch (AssemblerException | IOException ex) {
            result = AssemblerState.get();
            result.getLog().forEach(System.out::println);
            throw new RuntimeException(ex);
        }
        return new AssemblerBinary(program, result);
    }

    @Override
    public void writeSource(Path path) {
        try {
            String source = instructions.stream().map(LineParts::toString).collect(Collectors.joining("\n"));
            Files.write(path, source.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
