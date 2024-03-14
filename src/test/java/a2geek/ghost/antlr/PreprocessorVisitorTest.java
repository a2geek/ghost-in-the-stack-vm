package a2geek.ghost.antlr;

import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Test;

import static a2geek.ghost.antlr.PreprocessorVisitor.OPTION_REGEX;
import static org.junit.jupiter.api.Assertions.*;

public class PreprocessorVisitorTest {
    public static void verifyExpected(final String source, final String expected) {
        var stream = CharStreams.fromString(source);
        var actual = ParseUtil.preprocessor(stream);
        assertEquals(expected, actual);
    }

    @Test
    public void testRegex() {
        assertTrue(OPTION_REGEX.test("option heap lomem=1234"));
        assertFalse(OPTION_REGEX.test("print 'not option stuff'"));
        assertTrue(OPTION_REGEX.test("""
            option heap lomem=1234
            print "hello world"
            """));
        assertFalse(OPTION_REGEX.test("""
            print "not option"
            print "heap stuff"
            """));
    }

    @Test
    public void testIfElse() {
        verifyExpected("""
            print "start of program"
            #define MODE 1
            #if MODE = 1
            print "this is mode 1"
            #else
            print "this is some other mode"
            #endif
            print "end of program"
            end
            """,
            """
            print "start of program"
            print "this is mode 1"
            print "end of program"
            end
            """);
    }

    @Test
    public void testIfElseIf() {
        verifyExpected("""
            print "start of program"
            #define MODE 2
            #if MODE = 1
            print "this is mode 1"
            #elseif MODE = 2
            print "this is mode 2"
            #endif
            print "end of program"
            end
            """,
            """
            print "start of program"
            print "this is mode 2"
            print "end of program"
            end
            """);
    }

    @Test
    public void testElse() {
        verifyExpected("""
            print "start of program"
            #define MODE 9
            #if MODE = 1
            print "this is mode 1"
            #else
            print "this is not mode 1"
            #endif
            print "end of program"
            end
            """,
            """
            print "start of program"
            print "this is not mode 1"
            print "end of program"
            end
            """);
    }

    @Test
    public void testIfGetsBypassed() {
        verifyExpected("""
            print "start of program"
            #define MODE 9
            #if MODE = 1
            print "this is mode 1"
            #elseif MODE = 2
            print "this is mode 2"
            #endif
            print "end of program"
            end
            """,
            """
            print "start of program"
            print "end of program"
            end
            """);
    }

    @Test
    public void testOptionHeapDetection0() {
        verifyExpected("""
            print "start of program"
            #if defined(option.heap)
            print "heap defined"
            #else
            print "heap not defined"
            #endif
            print "end of program"
            end
            """,
            """
            print "start of program"
            print "heap not defined"
            print "end of program"
            end
            """);
    }

    @Test
    public void testOptionHeapDetection1() {
        verifyExpected("""
            option heap lomem=1234
            print "start of program"
            #if defined(option.heap)
            print "heap defined"
            #else
            print "heap not defined"
            #endif
            print "end of program"
            end
            """,
            """
            option heap lomem=1234
            print "start of program"
            print "heap defined"
            print "end of program"
            end
            """);
    }
}
