package a2geek.ghost.antlr;

import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.IntegerConstant;
import a2geek.ghost.model.expression.PlaceholderExpression;
import a2geek.ghost.model.expression.VariableReference;
import a2geek.ghost.model.scope.Program;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        // Note: DIM generates (potentially) dynamic code, so not testing generated DIM code.
        expect("dim a as integer, b as boolean, c(10) as integer")
            .hasSymbol("a", DataType.INTEGER, SymbolType.VARIABLE, DeclarationType.GLOBAL)
            .hasSymbol("b", DataType.BOOLEAN, SymbolType.VARIABLE, DeclarationType.GLOBAL)
            .hasArrayReference("c", DataType.INTEGER, SymbolType.VARIABLE, DeclarationType.GLOBAL, 1);
    }

    @Test
    public void testOptionStrictMode() {
        assertThrows(RuntimeException.class, () ->
            expect("""
                    option strict
                    a = a + 1
                    """));
    }

    @Test
    public void testUbound() {
        var arrayRef = Symbol.variable(model.fixCase("a"), SymbolType.VARIABLE)
                .dataType(DataType.INTEGER)
                .declarationType(DeclarationType.GLOBAL)
                .dimensions(List.of(new IntegerConstant(10)))
                .build();
        // Note: DIM generates (potentially) dynamic code, so not testing generated DIM code.
        expect("dim a(10) as integer, b as integer : b = ubound(a,1)")
                .hasSymbol("a", DataType.INTEGER, SymbolType.VARIABLE, DeclarationType.GLOBAL)
                .hasSymbol("b", DataType.INTEGER, SymbolType.VARIABLE, DeclarationType.GLOBAL)
                .assignment("a", null)
                .poke("pokew", VariableReference.with(arrayRef).plus(IntegerConstant.ZERO), constant(10))
                .assignment("b", ubound(arrayRef,1))
                .atEnd();
    }

    @Test
    public void testArrayReference() {
        var arrayRef = Symbol.variable(model.fixCase("a"), SymbolType.VARIABLE)
                .dataType(DataType.INTEGER)
                .declarationType(DeclarationType.GLOBAL)
                .dimensions(List.of(new IntegerConstant(10)))
                .build();
        // Note: DIM generates (potentially) dynamic code, so not testing generated DIM code.
        expect("dim a(10) as integer : a(5) = a(4) + 3")
            .hasArrayReference("a", DataType.INTEGER, SymbolType.VARIABLE, DeclarationType.GLOBAL, 1)
            .assignment("a", null)
            .poke("pokew", VariableReference.with(arrayRef).plus(IntegerConstant.ZERO), constant(10))
            .skipIfStmt()
            .skipIfStmt()   // because we don't really optimize them A(5) should be sufficient but we check A(4) as well
            .arrayAssignment("a", constant(5),
                    binary("+", arrayReference("A", DataType.INTEGER, SymbolType.VARIABLE, DeclarationType.GLOBAL, constant(4), 10),
                            constant(3)))
            .atEnd();
    }

    @Test
    public void testAssignment() {
        expect("a = 5")
            .hasSymbol("a", DataType.INTEGER, SymbolType.VARIABLE, DeclarationType.GLOBAL)
            .assignment("a", constant(5))
            .atEnd();
    }

    @Test
    public void testLabel() {
        // Note that labels must be in a separate line (or else it could look like a method invocation
        // like "GR:PLOT X,Y" where "GR:" becomes a label!
        expect("alabel:\n")
            .label("alabel")
            .atEnd();
    }

    @Test
    public void testIfShort() {
        // Note that the if "short" statement requires an explicit newline to terminate the statement
        expect("if true then a=10\n")
            .ifStmt(constant(true))
                .assignment("a", constant(10))
            .endIf()
            .atEnd();
    }

    @Test
    public void testIfElse() {
        var a = identifier("a", DataType.INTEGER, SymbolType.VARIABLE, DeclarationType.GLOBAL);
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
        expect("lores.gr")
            .callLibrarySub("gr")
            .atEnd();
    }

    @Test
    @Disabled
    public void testDoLoop1() {
//        expect("""
//                do while true
//                    a = 10
//                loop""")
//            .doLoop(DoLoopStatement.Operation.DO_WHILE, constant(true))
//                .assignment("a", constant(10))
//            .endBlock()
//            .atEnd();
    }

    @Test
    @Disabled
    public void testDoLoop2() {
//        expect("""
//                do
//                    a = 10
//                loop while true""")
//            .doLoop(DoLoopStatement.Operation.LOOP_WHILE, constant(true))
//                .assignment("a", constant(10))
//            .endBlock()
//            .atEnd();
    }

    @Test
    @Disabled
    public void testForNext() {
//        expect("""
//                for i = 1 to 10
//                    a = i
//                next i""")
//            .forNext("i", constant(1), constant(10))
//                .assignment("a", identifier("I", DataType.INTEGER, Scope.Type.GLOBAL))
//            .endBlock()
//            .atEnd();
    }

    @Test
    @Disabled
    public void testWhile() {
//        expect("""
//                while true
//                    a = 10
//                end while""")
//            .doLoop(DoLoopStatement.Operation.WHILE, constant(true))
//                .assignment("a", constant(10))
//            .endBlock()
//            .atEnd();
    }

    @Test
    @Disabled
    public void testRepeat() {
//        expect("""
//                repeat
//                    a = 10
//                until true""")
//            .doLoop(DoLoopStatement.Operation.REPEAT, constant(true))
//                .assignment("a", constant(10))
//            .endBlock()
//            .atEnd();
    }

    @Test
    @Disabled
    public void testExit() {
//        expect("""
//                while true
//                    exit while
//                end while""")
//            .doLoop(DoLoopStatement.Operation.WHILE, constant(true))
//                .exitStmt("while")
//            .endBlock()
//            .atEnd();
    }

    @Test
    public void testExit_outsideOfBlock() {
        assertThrows(
            RuntimeException.class,
            () -> {
                expect("exit for")
                    .fail("'exit for' must be in a FOR ... NEXT statement");
            }
        );
    }

    @Test
    public void testColor() {
        expect("lores.color(5)")
            .callLibrarySub("color", constant(5))
            .atEnd();
    }

    @Test
    public void testPlot() {
        expect("lores.plot(5,6)")
            .callLibrarySub("plot", constant(5), constant(6))
            .atEnd();
    }

    @Test
    public void testVlin() {
        expect("lores.vlin(1,2,3)")
            .callLibrarySub("vlin", constant(1), constant(2), constant(3))
            .atEnd();
    }

    @Test
    public void testHlin() {
        expect("lores.hlin(1,2,3)")
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
        expect("text.home")
            .callLibrarySub("home")
            .atEnd();
    }

    @Test
    public void testPrint() {
        expect("print 5, \"HELLO\";")
            .callLibrarySub("print_integer", constant(5))
            .callLibrarySub("print_comma")
            .callLibrarySub("print_string", constant("HELLO"))
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
        expect("text.text")
            .callLibrarySub("text")
            .atEnd();
    }

    @Test
    public void testVtab() {
        expect("text.vtab(5)")
            .callLibrarySub("vtab", constant(5))
            .atEnd();
    }

    @Test
    public void testHtab() {
        expect("text.htab(5)")
            .callLibrarySub("htab", constant(5))
            .atEnd();
    }

    @Test
    public void testSubDeclaration() {
        var expectedParameters = Arrays.asList(
            // These are in reverse order
            Symbol.variable("b", SymbolType.PARAMETER).dataType(DataType.INTEGER).build(),
            Symbol.variable("a", SymbolType.PARAMETER).dataType(DataType.INTEGER).build()
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
            Symbol.variable("b", SymbolType.PARAMETER).dataType(DataType.INTEGER)
                    .declarationType(DeclarationType.LOCAL).build(),
            Symbol.variable("a", SymbolType.PARAMETER).dataType(DataType.INTEGER)
                    .declarationType(DeclarationType.LOCAL).build()
        );
        expect("""
                function addThings(a as integer, b as integer)
                    return a + b
                end function
                """)
            .functionScope("addThings", expectedParameters, DataType.INTEGER)
                .returnStmt(binary("+",
                    identifier("A", DataType.INTEGER, SymbolType.PARAMETER, DeclarationType.LOCAL),
                    identifier("B", DataType.INTEGER, SymbolType.PARAMETER, DeclarationType.LOCAL)))
            .endScope()
            .atEnd();
    }

    @Test
    public void testSubArrayParameter() {
        var expectedParameters = Collections.singletonList(
                Symbol.variable("a", SymbolType.PARAMETER).dataType(DataType.INTEGER)
                      .dimensions(List.of(PlaceholderExpression.of(DataType.INTEGER))).build()
        );
        var arraySymbol = new VariableReference(Symbol.variable("A", SymbolType.VARIABLE)
            .dataType(DataType.INTEGER)
            .declarationType(DeclarationType.GLOBAL)
            .dimensions(List.of(new IntegerConstant(10)))
            .build());
        // Note: DIM generates (potentially) dynamic code, so not testing generated DIM code.
        expect("""
                sub addArray(a() as integer)
                    ' code goes here
                    return
                end sub
                
                dim a(10) as integer
                addArray(a)
                """)
            .subScope("addArray", expectedParameters)
                .hasArrayReference("a", DataType.INTEGER, SymbolType.PARAMETER, DeclarationType.LOCAL, 1)
                .returnStmt(null)
            .endScope()
            .hasArrayReference("a", DataType.INTEGER, SymbolType.VARIABLE, DeclarationType.GLOBAL, 1)
            .assignment("a", null)
            .poke("pokew", arraySymbol.plus(IntegerConstant.ZERO), constant(10))
            .callSub("addArray", arraySymbol)
            .atEnd();
    }

    @Test
    public void testPrivateVisibility() {
        assertThrows(
                RuntimeException.class,
                () -> {
                    expect("memory.setpriorptr(0x300, 0x310, 2)");
                }
        );
    }

    @Test
    public void testExportPrivateShouldFail() {
        assertThrows(
                RuntimeException.class,
                () -> {
                    expect("""
                            private export sub invalid()
                                return
                            end sub
                            """);
                }
        );
    }
}
