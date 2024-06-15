package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.*;
import a2geek.ghost.model.CompilerConfiguration;
import a2geek.ghost.model.ModelBuilder;
import a2geek.ghost.model.scope.Program;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;

import static a2geek.ghost.TrackingLogger.LOGGER;

public class ParseUtil {
    public static String preprocessor(CharStream stream, CompilerConfiguration config) {
        PreprocessorLexer lexer = new PreprocessorLexer(stream);
        lexer.addErrorListener(LOGGER);
        TokenStream tokens = new CommonTokenStream(lexer);
        PreprocessorGrammar parser = new PreprocessorGrammar(tokens);
        parser.addErrorListener(LOGGER);

        PreprocessorGrammar.SourceContext context = parser.source();
        LOGGER.check();

        PreprocessorVisitor visitor = new PreprocessorVisitor(config);
        visitor.visit(context);
        LOGGER.check();

        return visitor.getCode();
    }

    public static Program basicToModel(CharStream stream, ModelBuilder model) {
        stream = CharStreams.fromString(preprocessor(stream, model.getConfig()), stream.getSourceName());
        BasicLexer lexer = new BasicLexer(stream);
        lexer.addErrorListener(LOGGER);
        TokenStream tokens = new CommonTokenStream(lexer);
        BasicParser parser = new BasicParser(tokens);
        parser.addErrorListener(LOGGER);

        BasicParser.ProgramContext context = parser.program();
        LOGGER.check();

        GhostBasicVisitor visitor = new GhostBasicVisitor(model);
        visitor.visit(context);
        LOGGER.check();

        return visitor.getModel().getProgram();
    }

    public static Program integerToModel(CharStream stream, ModelBuilder model) {
        IntegerLexer lexer = new IntegerLexer(stream);
        lexer.addErrorListener(LOGGER);
        TokenStream tokens = new CommonTokenStream(lexer);
        IntegerParser parser = new IntegerParser(tokens);
        parser.addErrorListener(LOGGER);

        IntegerParser.ProgramContext context = parser.program();
        LOGGER.check();

        IntegerBasicVisitor visitor = new IntegerBasicVisitor(model);
        visitor.visit(context);
        LOGGER.check();

        return visitor.getModel().getProgram();
    }

}
