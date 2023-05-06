package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.BasicLexer;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.antlr.generated.IntegerLexer;
import a2geek.ghost.antlr.generated.IntegerParser;
import a2geek.ghost.model.ModelBuilder;
import a2geek.ghost.model.scope.Program;
import org.antlr.v4.runtime.*;

public class ParseUtil {
    private static final TrackingErrorListener ERROR_LISTENER = new TrackingErrorListener();
    public static Program basicToModel(CharStream stream, ModelBuilder model) {
        BasicLexer lexer = new BasicLexer(stream);
        lexer.addErrorListener(ERROR_LISTENER);
        TokenStream tokens = new CommonTokenStream(lexer);
        BasicParser parser = new BasicParser(tokens);
        parser.addErrorListener(ERROR_LISTENER);

        BasicParser.ProgramContext context = parser.program();
        ERROR_LISTENER.check();

        GhostBasicVisitor visitor = new GhostBasicVisitor(model);
        visitor.visit(context);
        return visitor.getModel().getProgram();
    }

    public static Program integerToModel(CharStream stream, ModelBuilder model) {
        IntegerLexer lexer = new IntegerLexer(stream);
        lexer.addErrorListener(ERROR_LISTENER);
        TokenStream tokens = new CommonTokenStream(lexer);
        IntegerParser parser = new IntegerParser(tokens);
        parser.addErrorListener(ERROR_LISTENER);

        IntegerParser.ProgramContext context = parser.program();
        ERROR_LISTENER.check();

        IntegerBasicVisitor visitor = new IntegerBasicVisitor(model);
        visitor.visit(context);
        return visitor.getModel().getProgram();
    }

    public static class TrackingErrorListener extends ConsoleErrorListener {
        private int counter;

        public void check() {
            if (counter > 0) {
                throw new RuntimeException("compilation halted due to parse errors");
            }
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            counter++;
            super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
        }
    }
}
