package a2geek.ghost;

import a2geek.ghost.antlr.GhostBasicVisitor;
import a2geek.ghost.antlr.generated.BasicLexer;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.model.Program;
import a2geek.ghost.model.visitor.CodeGenerationVisitor;
import a2geek.ghost.model.visitor.MetadataVisitor;
import a2geek.ghost.model.visitor.RewriteVisitor;
import org.antlr.v4.runtime.*;

public class Main {
    public static void main(String[] args) {
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
        program.accept(metadataVisitor);

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
    }
}