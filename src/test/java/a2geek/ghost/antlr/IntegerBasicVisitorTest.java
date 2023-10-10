package a2geek.ghost.antlr;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.ModelBuilder;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.scope.Program;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static a2geek.ghost.antlr.ExpressionBuilder.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IntegerBasicVisitorTest {
    public static StatementTester.ScopeTester expect(String source) {
        ModelBuilder model = new ModelBuilder(String::toUpperCase);
        Program program = ParseUtil.integerToModel(CharStreams.fromString(source), model);
        System.out.println(program);
        return new StatementTester.ScopeTester(program,
            String::toUpperCase, s -> String.format("%s()", s));
    }

    @Test
    public void testCallStatement() {
        expect("10 CALL 0x300")
            .lineNumber(10)
            .call(constant(0x300))
            .atEnd();
    }

    @Test
    public void testClear() {
        // Not implemented
        expect("10 CLR")
            .lineNumber(10)
            .atEnd();
    }

    @Test
    public void testColor() {
        expect("10 COLOR= 5")
            .lineNumber(10)
            .callLibrarySub("color", constant(5))
            .atEnd();
    }

    @Test
    public void testDel() {
        // Not implemented
        expect("10 DEL 10,100")
            .lineNumber(10)
            .atEnd();
    }

    @Test
    public void testDim() {
        // Need to add strings
        expect("10 DIM B(5)")
            .lineNumber(10)
            .dimStmt("B", constant(5))
            .atEnd();
    }

    @Test
    public void testDsp() {
        // Not implemented
        expect("10 DSP X")
            .lineNumber(10)
            .atEnd();
    }

    @Test
    public void testEnd() {
        expect("10 END")
            .lineNumber(10)
            .end()
            .atEnd();
    }

    @Test
    @Disabled
    public void testFor() {
//        expect("10 FOR X = 1 TO 10")
//            .hasSymbol("X", DataType.INTEGER, Scope.Type.GLOBAL)
//            .lineNumber(10)
//            .forStmt("X", constant(1), constant(10))
//            .atEnd();
    }

    @Test
    public void testGosub() {
        expect("10 GOSUB 100")
            .lineNumber(10)
            .gosub(100)
            .atEnd();
    }

    // TODO FIXME need to figure out built-in function testing as well as array references
//    @Test
//    public void testDynamicGosub() {
//        var expr = function("line_index",
//            binary("*", constant(10), identifier("I", DataType.INTEGER, Scope.Type.LOCAL)),
//            arrayReference(IntegerBasicVisitor.LINE_NUMBERS, DataType.INTEGER, Scope.Type.GLOBAL, constant(1)));
//        var labels = Arrays.asList("L10");
//        expect("10 GOSUB 10*I")
//            .hasSymbol("I", DataType.INTEGER, Scope.Type.GLOBAL)
//            .dimDefaultStmt(IntegerBasicVisitor.LINE_NUMBERS, Arrays.asList(constant(10)))
//            .hasArrayReference(IntegerBasicVisitor.LINE_NUMBERS, DataType.INTEGER, Scope.Type.GLOBAL, 1)
//            .lineNumber(10)
//            .onGosub(expr, labels)
//            .atEnd();
//    }

    @Test
    public void testGoto() {
        expect("10 GOTO 100")
            .lineNumber(10)
            .gotoStmt(100)
            .atEnd();
    }

    @Test
    public void testGr() {
        expect("10 GR")
            .lineNumber(10)
            .callLibrarySub("gr")
            .atEnd();
    }

    @Test
    public void testHimem() {
        // Not implemented
        expect("10 HIMEM: 1234")
            .lineNumber(10)
            .atEnd();
    }

    @Test
    public void testHlin() {
        expect("10 HLIN 5,25 AT 10")
            .lineNumber(10)
            .callLibrarySub("hlin", constant(5), constant(25), constant(10))
            .atEnd();
    }

    @Test
    public void testIfGoto() {
        var aRef = identifier("A",DataType.INTEGER, Scope.Type.GLOBAL);
        expect("10 IF A=1 THEN 50: PRINT \"OUTSIDE OF IF\"")
            .hasSymbol(aRef)
            .lineNumber(10)
            .ifStmt(binary("=", aRef, constant(1)))
                .gotoStmt(50)
            .endIf()
            .callLibrarySub("string", constant("OUTSIDE OF IF"))
            .callLibrarySub("newline")
            .atEnd();
    }

    @Test
    public void testIfWithStatement() {
        var aRef = identifier("A",DataType.INTEGER, Scope.Type.GLOBAL);
        expect("10 IF A=1 THEN PRINT \"IN IF\": PRINT \"OUTSIDE OF IF\"")
            .hasSymbol(aRef)
            .lineNumber(10)
            .ifStmt(binary("=", aRef, constant(1)))
                .callLibrarySub("string", constant("IN IF"))
                .callLibrarySub("newline")
            .endIf()
            .callLibrarySub("string", constant("OUTSIDE OF IF"))
            .callLibrarySub("newline")
            .atEnd();
    }

    @Test
    public void testInnum() {
        expect("10 IN# 0")
            .lineNumber(10)
            .callLibrarySub("innum", constant(0))
            .atEnd();
    }

    @Test
    @Disabled
    public void testInput() {
        // To be implemented
        expect("10 INPUT \"PROMPT? \",X,A$")
            .hasSymbol("X", DataType.INTEGER, Scope.Type.GLOBAL)
            .hasSymbol("A$", DataType.STRING, Scope.Type.GLOBAL)
            // TODO
            .atEnd();
    }

    @Test
    public void testLetIntegerAssignment() {
        // TODO string assignment
        // Normal
        expect("10 A = 1")
            .hasSymbol("A", DataType.INTEGER, Scope.Type.GLOBAL)
            .lineNumber(10)
            .assignment("A", constant(1))
            .atEnd();
        // LET keyword
        expect("10 LET A = 1")
            .hasSymbol("A", DataType.INTEGER, Scope.Type.GLOBAL)
            .lineNumber(10)
            .assignment("A", constant(1))
            .atEnd();
    }

    @Test
    public void testArrayReferences() {
        // Normal
        expect("10 A(2) = 1")
                .hasArrayReference("A", DataType.INTEGER, Scope.Type.GLOBAL, 1)
                .lineNumber(10)
                .skipIfStmt()
                .arrayAssignment("A", constant(2), constant(1))
                .atEnd();
        // LET keyword
        expect("10 A = A(1)")
                .hasSymbol("A", DataType.INTEGER, Scope.Type.GLOBAL)
                .hasArrayReference("A", DataType.INTEGER, Scope.Type.GLOBAL, 1)
                .lineNumber(10)
                .skipIfStmt()
                .assignment("A", arrayReference("A", DataType.INTEGER, Scope.Type.GLOBAL, constant(1)))
                .atEnd();
    }

    @Test
    public void testList() {
        // Not implemented
        expect("10 LIST 1,99")
            .lineNumber(10)
            .atEnd();
    }

    @Test
    public void testLomem() {
        // Not implemented
        expect("10 LOMEM: 0x800")
            .lineNumber(10)
            .atEnd();
    }

    @Test
    public void testNotrace() {
        // Not implemented
        expect("10 NOTRACE")
            .lineNumber(10)
            .atEnd();
    }

    @Test
    @Disabled
    public void testNext() {
//        expect("10 NEXT X")
//            .hasSymbol("X", DataType.INTEGER, Scope.Type.GLOBAL)
//            .lineNumber(10)
//            .nextStmt("X")
//            .atEnd();
    }

    @Test
    public void testPlot() {
        expect("10 PLOT 1,1")
            .lineNumber(10)
            .callLibrarySub("plot", constant(1), constant(1))
            .atEnd();
    }

    @Test
    public void testPoke() {
        expect("10 POKE 0x300,1")
            .lineNumber(10)
            .poke("poke", constant(0x300),constant(1))
            .atEnd();
    }

    @Test
    public void testPop() {
        expect("10 POP")
            .lineNumber(10)
            .pop()
            .atEnd();
    }

    @Test
    public void testPrnum() {
        expect("10 PR#3")
            .lineNumber(10)
            .callLibrarySub("prnum", constant(3))
            .atEnd();
    }

    @Test
    public void testPrint() {
        expect("10 PRINT \"X=\",X; : PRINT \"...\"")
            .hasSymbol("X", DataType.INTEGER, Scope.Type.GLOBAL)
            .lineNumber(10)
            .callLibrarySub("string", constant("X="))
            .callLibrarySub("comma")
            .callLibrarySub("integer", identifier("X", DataType.INTEGER, Scope.Type.GLOBAL))
            .callLibrarySub("string", constant("..."))
            .callLibrarySub("newline")
            .atEnd();
    }

    @Test
    public void testRem() {
        expect("10 REM this is a remark")
            .lineNumber(10)
            .atEnd();
    }

    @Test
    public void testMultiline() {
        expect("""
            10 REM first line of comment _
                   second line of comment _
                   last line of comment!
            """)
            .lineNumber(10)
            .atEnd();
    }

    @Test
    public void testReturn() {
        expect("10 RETURN")
            .lineNumber(10)
            .returnStmt(null)
            .atEnd();
    }

    @Test
    public void testRun() {
        assertThrows(RuntimeException.class, () -> {
            expect("10 RUN")
                .lineNumber(10)
                .atEnd();
        });
    }

    @Test
    public void testTab() {
        expect("10 TAB 15")
            .lineNumber(10)
            .callLibrarySub("htab", constant(15))
            .atEnd();
    }

    @Test
    public void testTrace() {
        // Not implemented
        expect("10 TRACE")
            .lineNumber(10)
            .atEnd();
    }

    @Test
    public void testText() {
        expect("10 TEXT")
            .lineNumber(10)
            .callLibrarySub("text")
            .atEnd();
    }

    @Test
    public void testVlin() {
        expect("10 VLIN 1,2 AT 3")
            .lineNumber(10)
            .callLibrarySub("vlin", constant(1), constant(2), constant(3))
            .atEnd();
    }

    @Test
    public void testVtab() {
        expect("10 VTAB 5")
            .lineNumber(10)
            .callLibrarySub("vtab", constant(5))
            .atEnd();
    }
}
