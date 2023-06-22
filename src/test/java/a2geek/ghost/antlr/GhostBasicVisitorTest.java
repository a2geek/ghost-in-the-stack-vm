package a2geek.ghost.antlr;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.ModelBuilder;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.expression.VariableReference;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.statement.DoLoopStatement;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static a2geek.ghost.antlr.ExpressionBuilder.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GhostBasicVisitorTest {
    private static ModelBuilder model;

    public static StatementTester.ScopeTester expect(String source) {
        model = new ModelBuilder(String::toUpperCase);
        Program program = ParseUtil.basicToModel(CharStreams.fromString(source), model);
        System.out.println(program);
        return new StatementTester.ScopeTester(program,
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
    public void testUbound() {
        var arrayRef = Symbol.variable(model.fixCase("a"), Scope.Type.GLOBAL)
                .dataType(DataType.INTEGER)
                .dimensions(1)
                .build();
        expect("dim a(10) as integer, b as integer : b = ubound(a)")
                .hasSymbol("a", DataType.INTEGER, Scope.Type.GLOBAL)
                .hasSymbol("b", DataType.INTEGER, Scope.Type.GLOBAL)
                .dimStmt("a", constant(10))
                .assignment("b", ubound(model, arrayRef))
                .atEnd();
    }

    @Test
    public void testArrayReference() {
        expect("dim a(10) as integer : a(5) = a(4) + 3")
            .hasArrayReference("a", DataType.INTEGER, Scope.Type.GLOBAL, 1)
            .dimStmt("a", constant(10))
            .skipIfStmt()
            .skipIfStmt()   // because we don't really optimize them A(5) should be sufficient but we check A(4) as well
            .arrayAssignment("a", constant(5),
                    binary("+", arrayReference("a", DataType.INTEGER, Scope.Type.GLOBAL, constant(4)),
                            constant(3)))
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
            .callLibrarySub("gr")
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
            .callLibrarySub("color", constant(5))
            .atEnd();
    }

    @Test
    public void testPlot() {
        expect("plot 5,6")
            .callLibrarySub("plot", constant(5), constant(6))
            .atEnd();
    }

    @Test
    public void testVlin() {
        expect("vlin 1,2 at 3")
            .callLibrarySub("vlin", constant(1), constant(2), constant(3))
            .atEnd();
    }

    @Test
    public void testHlin() {
        expect("hlin 1,2 at 3")
            .callLibrarySub("hlin", constant(1), constant(2), constant(3))
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
            .callLibrarySub("home")
            .atEnd();
    }

    @Test
    public void testPrint() {
        expect("print 5, \"HELLO\";")
            .callLibrarySub("integer", constant(5))
            .callLibrarySub("comma")
            .callLibrarySub("string", constant("HELLO"))
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
            .callLibrarySub("text")
            .atEnd();
    }

    @Test
    public void testVtab() {
        expect("vtab 5")
            .callLibrarySub("vtab", constant(5))
            .atEnd();
    }

    @Test
    public void testHtab() {
        expect("htab 5")
            .callLibrarySub("htab", constant(5))
            .atEnd();
    }

    @Test
    public void testSubDeclaration() {
        var expectedParameters = Arrays.asList(
            // These are in reverse order
            Symbol.variable("b", Scope.Type.PARAMETER).dataType(DataType.INTEGER).build(),
            Symbol.variable("a", Scope.Type.PARAMETER).dataType(DataType.INTEGER).build()
        );
        expect("""
                sub doSomething(a as integer, b as integer)
                    a = 1
                end sub
                
                doSomething(1,2)
                """)
            .subScope("doSomething", expectedParameters)
                .assignment("a", constant(1))
            .endScope()
            .callSub("doSomething", constant(1), constant(2))
            .atEnd();
    }

    @Test
    public void testFunctionDeclaration() {
        var expectedParameters = Arrays.asList(
            // These are in reverse order
            Symbol.variable("b", Scope.Type.PARAMETER).dataType(DataType.INTEGER).build(),
            Symbol.variable("a", Scope.Type.PARAMETER).dataType(DataType.INTEGER).build()
        );
        expect("""
                function addThings(a as integer, b as integer)
                    return a + b
                end function
                """)
            .functionScope("addThings", expectedParameters, DataType.INTEGER)
                .returnStmt(binary("+",
                    identifier("A", DataType.INTEGER, Scope.Type.PARAMETER),
                    identifier("B", DataType.INTEGER, Scope.Type.PARAMETER)))
            .endScope()
            .atEnd();
    }

    @Test
    public void testSubArrayParameter() {
        var expectedParameters = Arrays.asList(
            Symbol.variable("a", Scope.Type.PARAMETER).dataType(DataType.INTEGER).dimensions(1).build()
        );
        var arraySymbol = new VariableReference(Symbol.variable("A", Scope.Type.GLOBAL)
            .dataType(DataType.INTEGER)
            .dimensions(1)
            .build());
        expect("""
                sub addArray(a() as integer)
                    ' code goes here
                    return
                end sub
                
                dim a(10) as integer
                addArray(a)
                """)
            .subScope("addArray", expectedParameters)
                .hasArrayReference("a", DataType.INTEGER, Scope.Type.PARAMETER, 1)
                .returnStmt(null)
            .endScope()
            .hasArrayReference("a", DataType.INTEGER, Scope.Type.GLOBAL, 1)
            .dimStmt("a", constant(10))
            .callSub("addArray", arraySymbol)
            .atEnd();
    }
}
