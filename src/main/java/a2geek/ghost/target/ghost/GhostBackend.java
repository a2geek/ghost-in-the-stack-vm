package a2geek.ghost.target.ghost;

import a2geek.asm.api.service.AssemblerService;
import a2geek.asm.api.service.AssemblerState;
import a2geek.asm.api.util.AssemblerException;
import a2geek.asm.api.util.Sources;
import a2geek.ghost.command.util.ByteFormatter;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.target.TargetBackend;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GhostBackend implements TargetBackend {
    @Override
    public Code generate(Program program) {
        CodeGenerationVisitor codeGenerationVisitor = new CodeGenerationVisitor();
        codeGenerationVisitor.visit(program);

        List<Instruction> instructions = codeGenerationVisitor.getInstructions();
        return new GhostCode(program, instructions);
    }

    @Override
    public Code optimize(Code code, OptimizationFlags optimizations) {
        if (code instanceof GhostCode(Program program, List<Instruction> instructions)) {
            if (optimizations.labels()) {
                LabelOptimizer.optimize(instructions);
            }
            if (optimizations.peephole()) {
                int loops = 5;      // arbitrarily picking maximum number of passes
                while (loops > 0 && PeepholeOptimizer.optimize(instructions) > 0) {
                    if (optimizations.labels()) {
                        // This gets impacted by the peephole optimizer as well
                        LabelOptimizer.optimize(instructions);
                    }
                    loops--;
                }
            }
            return new GhostCode(program, instructions);
        }
        throw new RuntimeException("unexpected Code type: " + code.getClass().getName());
    }

    @Override
    public Binary assemble(Code code) {
        if (code instanceof GhostCode(Program program, List<Instruction> instructions)) {
            ByteArrayOutputStream binary = new ByteArrayOutputStream();

            // Generate the interpreter with the built-in assembler
            AssemblerState interpreter = null;
            try {
                interpreter = AssemblerService.assemble(Sources.get(() -> getClass().getResourceAsStream("/asm/interp.asm")));
            } catch (AssemblerException | IOException ex) {
                interpreter = AssemblerState.get();
                interpreter.getLog().forEach(System.out::println);
                throw new RuntimeException(ex);
            }
            binary.writeBytes(interpreter.getOutput().toByteArray());

            // Assembly first pass: Figure out label values
            Map<String,Integer> addrs = new HashMap<>();
            int addr = 0;
            for (Instruction instruction : instructions) {
                if (instruction.opcode() == null && instruction.label() != null) {
                    addrs.put(instruction.label(), addr);
                }
                addr += instruction.size();
            }

            // Assembly second pass: Generate output
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            addr = 0;
            for (Instruction instruction : instructions) {
                byte[] data = instruction.assemble(addrs);
                binary.writeBytes(data);
                //
                ByteFormatter bf = ByteFormatter.from(data);
                if (instruction.opcode() != null) {
                    pw.printf("%04x: %-10.10s  %s\n", addr, bf.get(data.length), instruction);
                }
                else if (instruction.directive() != null) {
                    int lineaddr = addr;
                    pw.printf("%04x: %-10.10s  %s\n", lineaddr, bf.get(3), instruction);
                    lineaddr+= 3;
                    while (bf.hasMore()) {
                        pw.printf("%04x: %s\n", lineaddr, bf.get(8));
                        lineaddr += 8;
                    }
                }
                else {
                    pw.printf("%04x: %-10.10s  %s\n", addr, "", instruction);
                }
                //
                addr += instruction.size();
            }
            return new GhostBinary(program, binary.toByteArray(), sw.toString());
        }
        throw new RuntimeException("unexpected Code type: " + code.getClass().getName());
    }

    record GhostCode(Program program, List<Instruction> instructions) implements Code {
        @Override
        public void writeSource(Path path) {
            try {
                Files.write(path, instructions.toString().getBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    record GhostBinary(Program program, byte[] binary, String listing) implements Binary {
        @Override
        public void writeSource(Path path) {
            try {
                Files.write(path, listing().getBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void writeSymbols(Path path) {
            var fmt = "| %-20.20s | %-5.5s | %-10.10s | %-10.10s | %-10.10s | %-20.20s | %-15.15s | %-20.20s | %-20.20s |\n";
            var scopes = new ArrayList<Scope>();
            scopes.addLast(program);
            try (
                var bw = Files.newBufferedWriter(path);
                var pw = new PrintWriter(bw)
            ) {
                pw.printf(fmt, "Name", "Temp?", "SymType", "DeclType", "DataType", "Scope", "Dimensions", "Default", "TargetName");
                while (!scopes.isEmpty()) {
                    var scope = scopes.removeFirst();
                    scope.getLocalSymbols().forEach(symbol -> {
                        pw.printf(fmt, symbol.name(), symbol.temporary() ? "Temp" : "-",
                                symbol.symbolType(), symbol.declarationType(),
                                ifNull(symbol.dataType(), "-n/a-"), scope.getName(),
                                symbol.dimensions().isEmpty() ? "N/A" : symbol.dimensions(),
                                ifNull(symbol.defaultValues(),"-none-"),
                                ifNull(symbol.targetName(), "-"));
                        if (symbol.scope() != null) {
                            scopes.addLast(symbol.scope());
                        }
                    });
                }
            }
            catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        private String ifNull(Object value, String defaultValue) {
            return value == null ? defaultValue : value.toString();
        }

        @Override
        public byte[] getBytes() {
            return binary();
        }
    }
}
