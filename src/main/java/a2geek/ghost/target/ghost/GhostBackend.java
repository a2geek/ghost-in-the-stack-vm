package a2geek.ghost.target.ghost;

import a2geek.asm.api.service.AssemblerService;
import a2geek.asm.api.service.AssemblerState;
import a2geek.asm.api.util.AssemblerException;
import a2geek.asm.api.util.Sources;
import a2geek.ghost.command.util.ByteFormatter;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.target.TargetBackend;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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

    record GhostCode(Program program, List<Instruction> instructions) implements Code {
        @Override
        public Code optimize(OptimizationFlags optimizations) {
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

        @Override
        public Binary assemble() {
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
        public byte[] getBytes() {
            return binary();
        }
    }
}
