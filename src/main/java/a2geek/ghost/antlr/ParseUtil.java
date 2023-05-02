package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.BasicLexer;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.antlr.generated.IntegerLexer;
import a2geek.ghost.antlr.generated.IntegerParser;
import a2geek.ghost.model.ModelBuilder;
import a2geek.ghost.model.scope.Program;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.TokenStream;

public class ParseUtil {
    public static Program basicToModel(CharStream stream, ModelBuilder model) {
        BasicLexer lexer = new BasicLexer(stream);
        lexer.addErrorListener(ConsoleErrorListener.INSTANCE);
        TokenStream tokens = new CommonTokenStream(lexer);
        BasicParser parser = new BasicParser(tokens);
        parser.addErrorListener(ConsoleErrorListener.INSTANCE);

        BasicParser.ProgramContext context = parser.program();
        GhostBasicVisitor visitor = new GhostBasicVisitor(model);
        visitor.visit(context);
        return visitor.getModel().getProgram();
    }

    public static Program integerToModel(CharStream stream, ModelBuilder model) {
        IntegerLexer lexer = new IntegerLexer(stream);
        lexer.addErrorListener(ConsoleErrorListener.INSTANCE);
        TokenStream tokens = new CommonTokenStream(lexer);
        IntegerParser parser = new IntegerParser(tokens);
        parser.addErrorListener(ConsoleErrorListener.INSTANCE);

        IntegerParser.ProgramContext context = parser.program();
        IntegerBasicVisitor visitor = new IntegerBasicVisitor(model);
        visitor.visit(context);
        return visitor.getModel().getProgram();
    }
}
