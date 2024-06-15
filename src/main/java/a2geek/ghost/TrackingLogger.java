package a2geek.ghost;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.Objects;

public class TrackingLogger extends BaseErrorListener {
    public static final TrackingLogger LOGGER = new TrackingLogger();

    private Level level = Level.ERROR;
    private int infoCount;
    private int warningCount;
    private int errorCount;
    private int failCount;

    private TrackingLogger() {
        // prevent construction
    }

    public void setLevel(Level level) {
        Objects.requireNonNull(level);
        this.level = level;
    }

    public void clear() {
        infoCount = 0;
        warningCount = 0;
        errorCount = 0;
        failCount = 0;
    }

    public void summary() {
        System.out.printf("There were %d failures, %d errors, %d warnings, and %d informational messages.\n",
                failCount, errorCount, warningCount, infoCount);
    }

    public void check() {
        if (errorCount > 0) {
            summary();
            throw new RuntimeException("compilation halted due to errors");
        }
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        errorf("line " + line + ":" + charPositionInLine + " " + msg);
    }

    public void failf(String fmt, Object... args) {
        failCount++;
        System.out.printf("FAILURE [compiler bug]: " + fmt + "\n", args);
        throw new RuntimeException("Compiler failure encountered; stopping compile");
    }

    public void errorf(String fmt, Object... args) {
        errorCount++;
        if (level.ordinal() <= Level.ERROR.ordinal()) {
            System.out.printf("ERROR: " + fmt + "\n", args);
        }
    }

    public void warningf(String fmt, Object... args) {
        warningCount++;
        if (level.ordinal() <= Level.WARNING.ordinal()) {
            System.out.printf("WARNING: " + fmt + "\n", args);
        }
    }

    public void infof(String fmt, Object... args) {
        infoCount++;
        if (level.ordinal() <= Level.INFO.ordinal()) {
            System.out.printf("INFO: " + fmt + "\n", args);
        }
    }

    public enum Level {
        INFO,
        WARNING,
        ERROR
    }
}
