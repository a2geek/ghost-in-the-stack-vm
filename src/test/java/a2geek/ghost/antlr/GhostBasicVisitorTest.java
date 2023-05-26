package a2geek.ghost.antlr;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.ModelBuilder;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.statement.DoLoopStatement;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Test;

import static a2geek.ghost.antlr.ExpressionBuilder.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GhostBasicVisitorTest {
    public static StatementTester.ProgramTester expect(String source) {
        ModelBuilder model = new ModelBuilder(String::toUpperCase);
        Program program = ParseUtil.basicToModel(CharStreams.fromString(source), model);
        System.out.println(program);
        return new StatementTester.ProgramTester(program,
            String::toUpperCase, s -> s);
    }

    @Test
    public void testDimStatement() {
        expect("dim a as integer, b as boolean, c(10) as integer")
            .hasSymbol("a", DataType.INTEGER, Scope.Type.GLOBAL)
            .hasSymbol("b", DataType.BOOLEAN, Scope.Type.GLOBAL)
            .hasArrayReference("c", DataType.INTEGER, Scope.Type.GLOBAL, 1)
            .dimStmt("c", constant(10))
            .atEnd();
    }

    @Test
    public void testAssignment() {
        expect("a = 5")
            .hasSymbol("a", DataType.INTEGER, Scope.Type.GLOBAL)
            .assignment("a", constant(5))
            .atEnd();
    }

    @Test
    public void testLabel() {
        expect("alabel:")
            .label("alabel")
            .atEnd();
    }

    @Test
    public void testIfShort() {
        expect("if true then a=10")
            .ifStmt(constant(true))
                .assignment("a", constant(10))
            .endIf()
            .atEnd();
    }

    @Test
    public void testIfElse() {
        var a = identifier("a", DataType.INTEGER, Scope.Type.GLOBAL);
        expect("""
                if a=10 then
                    a=5
                else
                    a=10
                end if
                """)
            .ifStmt(binary("=", a, constant(10)))
                .assignment("a", constant(5))
            .elseStmt()
                .assignment("a", constant(10))
            .endIf()
            .atEnd();
    }

    @Test
    public void testGr() {
        expect("gr")
            .callSub("gr")
            .atEnd();
    }

    @Test
    public void testDoLoop1() {
        expect("""
                do while true
                    a = 10
                loop""")
            .doLoop(DoLoopStatement.Operation.DO_WHILE, constant(true))
                .assignment("a", constant(10))
            .endBlock()
            .atEnd();
    }

    @Test
    public void testDoLoop2() {
        expect("""
                do
                    a = 10
                loop while true""")
            .doLoop(DoLoopStatement.Operation.LOOP_WHILE, constant(true))
                .assignment("a", constant(10))
            .endBlock()
            .atEnd();
    }

    @Test
    public void testForNext() {
        expect("""
                for i = 1 to 10
                    a = i
                next i""")
            .forNext("i", constant(1), constant(10))
                .assignment("a", identifier("I", DataType.INTEGER, Scope.Type.GLOBAL))
            .endBlock()
            .atEnd();
    }

    @Test
    public void testWhile() {
        expect("""
                while true
                    a = 10
                end while""")
            .doLoop(DoLoopStatement.Operation.WHILE, constant(true))
                .assignment("a", constant(10))
            .endBlock()
            .atEnd();
    }

    @Test
    public void testRepeat() {
        expect("""
                repeat
                    a = 10
                until true""")
            .doLoop(DoLoopStatement.Operation.REPEAT, constant(true))
                .assignment("a", constant(10))
            .endBlock()
            .atEnd();
    }

    @Test
    public void testExit() {
        expect("""
                while true
                    exit while
                end while""")
            .doLoop(DoLoopStatement.Operation.WHILE, constant(true))
                .exitStmt("while")
            .endBlock()
            .atEnd();
    }

    @Test
    public void testExit_outsideOfBlock() {
        assertThrows(
            RuntimeException.class,
            () -> {
                expect("exit for")
                    .exitStmt("for")
                    .atEnd();
            }
        );
    }

    @Test
    public void testColor() {
        expect("color=5")
            .callSub("color", constant(5))
            .atEnd();
    }

    @Test
    public void testPlot() {
        expect("plot 5,6")
            .callSub("plot", constant(5), constant(6))
            .atEnd();
    }

    @Test
    public void testVlin() {
        expect("vlin 1,2 at 3")
            .callSub("vlin", constant(1), constant(2), constant(3))
            .atEnd();
    }

    @Test
    public void testHlin() {
        expect("hlin 1,2 at 3")
            .callSub("hlin", constant(1), constant(2), constant(3))
            .atEnd();
    }

    @Test
    public void testEnd() {
        expect("end")
            .atEnd();
    }

    @Test
    public void testHome() {
        expect("home")
            .callSub("home")
            .atEnd();
    }

    @Test
    public void testPrint() {
        expect("print 5, \"HELLO\";")
            .callSub("integer", constant(5))
            .callSub("comma")
            .callSub("string", constant("HELLO"))
            .atEnd();
    }

    @Test
    public void testPoke() {
        expect("poke 1234,5 : pokew 2345,6")
            .poke("poke", constant(1234), constant(5))
            .poke("pokew", constant(2345), constant(6))
            .atEnd();
    }

    @Test
    public void testCallStatement() {
        expect("call 0x300")
            .call(constant(0x300))
            .atEnd();
    }

    @Test
    public void testGoto() {
        expect("goto label")
            .gotoStmt("label")
            .atEnd();
    }

    @Test
    public void testGosub() {
        expect("gosub label")
            .gosub("label")
            .atEnd();
    }

    @Test
    public void testReturn() {
        expect("return 1 : return")
            .returnStmt(constant(1))
            .returnStmt(null)
            .atEnd();
    }

    @Test
    public void testText() {
        expect("text")
            .callSub("text")
            .atEnd();
    }

    @Test
    public void testVtab() {
        expect("vtab 5")
            .callSub("vtab", constant(5))
            .atEnd();
    }

    @Test
    public void testHtab() {
        expect("htab 5")
            .callSub("htab", constant(5))
            .atEnd();
    }
}
