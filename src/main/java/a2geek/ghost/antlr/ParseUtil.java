package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.BasicLexer;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.antlr.generated.IntegerLexer;
import a2geek.ghost.antlr.generated.IntegerParser;
import a2geek.ghost.model.scope.Program;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.TokenStream;

import java.util.function.Consumer;
import java.util.function.Function;

public class ParseUtil {
    public static Program basicToModel(CharStream stream, Function<String,String> caseStrategy) {
        return basicToModel(stream, caseStrategy, (s) -> {
            // nothing to configure here
        });
    }
    public static Program basicToModel(CharStream stream, Function<String,String> caseStrategy, Consumer<GhostBasicVisitor> configurer) {
        BasicLexer lexer = new BasicLexer(stream);
        lexer.addErrorListener(ConsoleErrorListener.INSTANCE);
        TokenStream tokens = new CommonTokenStream(lexer);
        BasicParser parser = new BasicParser(tokens);
        parser.addErrorListener(ConsoleErrorListener.INSTANCE);

        BasicParser.ProgramContext context = parser.program();
        GhostBasicVisitor visitor = new GhostBasicVisitor(caseStrategy);
        configurer.accept(visitor);
        visitor.visit(context);
        return visitor.getModel().getProgram();
    }

    public static Program integerToModel(CharStream stream) {
        IntegerLexer lexer = new IntegerLexer(stream);
        lexer.addErrorListener(ConsoleErrorListener.INSTANCE);
        TokenStream tokens = new CommonTokenStream(lexer);
        IntegerParser parser = new IntegerParser(tokens);
        parser.addErrorListener(ConsoleErrorListener.INSTANCE);

        IntegerParser.ProgramContext context = parser.program();
        IntegerBasicVisitor visitor = new IntegerBasicVisitor();
        visitor.visit(context);
        return visitor.getModel().getProgram();
    }
}
