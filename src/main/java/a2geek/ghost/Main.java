package a2geek.ghost;

import a2geek.ghost.antlr.GhostBasicVisitor;
import a2geek.ghost.antlr.generated.BasicLexer;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.model.Program;
import a2geek.ghost.model.code.Instruction;
import a2geek.ghost.model.visitor.CodeGenerationVisitor;
import a2geek.ghost.model.visitor.MetadataVisitor;
import a2geek.ghost.model.visitor.RewriteVisitor;
import org.antlr.v4.runtime.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {
        String code = """
                GR
                FOR C = 0 TO 14
                    COLOR= C + 1
                    FOR Y = C * 2 TO C * 2 + 10
                        FOR X = C * 2 TO C * 2 + 10
                        PLOT X,Y
                        NEXT X
                    NEXT Y
                NEXT C
                END
                """;
        //CharStream stream = CharStreams.fromFileName(args[0]);
        CodePointCharStream stream = CharStreams.fromString(code);
        BasicLexer lexer = new BasicLexer(stream);
        lexer.addErrorListener(ConsoleErrorListener.INSTANCE);
        TokenStream tokens = new CommonTokenStream(lexer);
        BasicParser parser = new BasicParser(tokens);
        parser.addErrorListener(ConsoleErrorListener.INSTANCE);

        BasicParser.ProgramContext context = parser.program();
        GhostBasicVisitor visitor = new GhostBasicVisitor();
        visitor.visit(context);

        Program program = visitor.getProgram();

        MetadataVisitor metadataVisitor = new MetadataVisitor();
        metadataVisitor.visit(program);

        System.out.println("=== SOURCE CODE ===");
        System.out.println(code);
        System.out.println("=== MODEL ===");
        System.out.println(program);
        System.out.println("=== VARIABLES ===");
        System.out.println(metadataVisitor.getVariables());

        RewriteVisitor rewriteVisitor = new RewriteVisitor();
        rewriteVisitor.visit(program);

        System.out.println("=== REWRITTEN ===");
        System.out.println(program);

        CodeGenerationVisitor codeGenerationVisitor = new CodeGenerationVisitor(metadataVisitor.getVariables());
        codeGenerationVisitor.visit(program);
        System.out.println("=== CODE ===");
        codeGenerationVisitor.getInstructions().forEach(System.out::println);

        // Assembly first pass: Figure out label values
        Map<String,Integer> addrs = new HashMap<>();
        int addr = 0;
        for (Instruction instruction : codeGenerationVisitor.getInstructions()) {
            if (instruction.isLabelOnly()) {
                addrs.put(instruction.label(), addr);
            }
            addr += instruction.size();
        }

        // Assembly second pass: Generate output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        addr = 0;
        for (Instruction instruction : codeGenerationVisitor.getInstructions()) {
            byte[] data = instruction.assemble(addrs);
            out.writeBytes(data);
            //
            String bytes = null;
            switch (data.length) {
                case 0:
                    bytes = "";
                    break;
                case 1:
                    bytes = String.format("%02x", data[0]);
                    break;
                case 2:
                    bytes = String.format("%02x %02x", data[0], data[1]);
                    break;
                case 3:
                    bytes = String.format("%02x %02x %02x", data[0], data[1], data[2]);
                    break;
            }
            System.out.printf("%04x: %-10.10s  %s\n", addr, bytes, instruction);
            //
            addr += instruction.size();
        }

        Files.write(Path.of("output.bin"), out.toByteArray());
    }
}