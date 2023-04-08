package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.BasicLexer;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.model.scope.Program;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.TokenStream;

import java.util.function.Function;

public class GhostBasicUtil {
    public static Program toModel(CharStream stream, Function<String,String> caseStrategy) {
        BasicLexer lexer = new BasicLexer(stream);
        lexer.addErrorListener(ConsoleErrorListener.INSTANCE);
        TokenStream tokens = new CommonTokenStream(lexer);
        BasicParser parser = new BasicParser(tokens);
        parser.addErrorListener(ConsoleErrorListener.INSTANCE);

        BasicParser.ProgramContext context = parser.program();
        GhostBasicVisitor visitor = new GhostBasicVisitor(caseStrategy);
        visitor.visit(context);
        return visitor.getProgram();
    }
}
