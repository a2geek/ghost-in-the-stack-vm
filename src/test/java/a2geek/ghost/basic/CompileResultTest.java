package a2geek.ghost.basic;

import a2geek.ghost.antlr.ParseUtil;
import a2geek.ghost.command.util.PrettyPrintVisitor;
import a2geek.ghost.model.ModelBuilder;
import a2geek.ghost.model.scope.Program;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This test compiles everything in the tests directory and compares the results to what existed before.
 * Any differences are considered an error. If the differences are ok, delete current listing and rerun to replace.
 */
public class CompileResultTest {
    private static final Path BASIC_DIR = Path.of("src/main/basic");
    private static final Path SOURCE_CODE = BASIC_DIR.resolve("tests");
    private static final Path EXPECTED_OUTPUT = Path.of("src/test/resources/expected");
    private static final PathMatcher BASIC_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*.bas");
    private static final PathMatcher INTEGER_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*.int");

    public static Stream<Path> sourceFiles() throws IOException {
        return Files.find(SOURCE_CODE, 1, (p,a) -> BASIC_MATCHER.matches(p.getFileName()) || INTEGER_MATCHER.matches(p.getFileName()));
    }

    @ParameterizedTest
    @MethodSource({ "sourceFiles" })
    public void test(Path path) throws IOException {
        ModelBuilder model = new ModelBuilder(String::toUpperCase);
        Program.reset();    // reset any global trackers for each program
        CharStream charStream = CharStreams.fromPath(path);
        if (BASIC_MATCHER.matches(path.getFileName())) {
            checkProgram(ParseUtil.basicToModel(charStream, model), path);
        }
        else if (INTEGER_MATCHER.matches(path.getFileName())) {
            checkProgram(ParseUtil.integerToModel(charStream, model), path);
        }
        else {
            throw new RuntimeException("unexpected path for testing: " + path);
        }
    }

    public void checkProgram(Program program, Path source) throws IOException {
        var filename = source.getFileName().toString().replaceFirst("\\.(bas|int)$", ".lst");
        var subpath = BASIC_DIR.relativize(source);
        var expectedIntermediate = EXPECTED_OUTPUT.resolve(subpath).resolveSibling(Path.of(filename));
        var actual = PrettyPrintVisitor.format(program, PrettyPrintVisitor.config().includeModules(false));
        if (Files.exists(expectedIntermediate)) {
            // compare
            var expected = Files.readString(expectedIntermediate);
            assertEquals(expected, actual);
        }
        else {
            // save output
            if (!Files.exists(expectedIntermediate.getParent())) {
                Files.createDirectories(expectedIntermediate.getParent());
            }
            Files.write(expectedIntermediate, actual.getBytes());
        }
    }
}
