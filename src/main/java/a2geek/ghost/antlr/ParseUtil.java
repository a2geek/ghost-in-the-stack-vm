package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.*;
import a2geek.ghost.model.CompilerConfiguration;
import a2geek.ghost.model.ModelBuilder;
import a2geek.ghost.model.scope.Program;
import org.antlr.v4.runtime.*;

public class ParseUtil {
    public static String preprocessor(CharStream stream, CompilerConfiguration config) {
        PreprocessorLexer lexer = new PreprocessorLexer(stream);
        TrackingErrorListener errorListener = new TrackingErrorListener();
        lexer.addErrorListener(errorListener);
        TokenStream tokens = new CommonTokenStream(lexer);
        PreprocessorGrammar parser = new PreprocessorGrammar(tokens);
        parser.addErrorListener(errorListener);

        PreprocessorGrammar.SourceContext context = parser.source();
        errorListener.check();

        PreprocessorVisitor visitor = new PreprocessorVisitor(config);
        visitor.visit(context);
        return visitor.getCode();
    }

    public static Program basicToModel(CharStream stream, ModelBuilder model) {
        stream = CharStreams.fromString(preprocessor(stream, model.getConfig()), stream.getSourceName());
        BasicLexer lexer = new BasicLexer(stream);
        TrackingErrorListener errorListener = new TrackingErrorListener();
        lexer.addErrorListener(errorListener);
        TokenStream tokens = new CommonTokenStream(lexer);
        BasicParser parser = new BasicParser(tokens);
        parser.addErrorListener(errorListener);

        BasicParser.ProgramContext context = parser.program();
        errorListener.check();

        GhostBasicVisitor visitor = new GhostBasicVisitor(model);
        visitor.visit(context);
        return visitor.getModel().getProgram();
    }

    public static Program integerToModel(CharStream stream, ModelBuilder model) {
        IntegerLexer lexer = new IntegerLexer(stream);
        TrackingErrorListener errorListener = new TrackingErrorListener();
        lexer.addErrorListener(errorListener);
        TokenStream tokens = new CommonTokenStream(lexer);
        IntegerParser parser = new IntegerParser(tokens);
        parser.addErrorListener(errorListener);

        IntegerParser.ProgramContext context = parser.program();
        errorListener.check();

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
