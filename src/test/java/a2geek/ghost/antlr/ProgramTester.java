package a2geek.ghost.antlr;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.statement.*;
import org.antlr.v4.runtime.CharStreams;

import static a2geek.ghost.antlr.ExpressionBuilder.constant;
import static org.junit.jupiter.api.Assertions.*;

public class ProgramTester {
    private final Program program;
    private int statementNumber;

    public ProgramTester(String source) {
        program = ParseUtil.integerToModel(CharStreams.fromString(source));
    }

    private String lineNumberLabel(int lineNumber) {
        return String.format("L%d", lineNumber);
    }

    public ProgramTester hasVariable(String name, DataType dataType, Scope.Type scopeType) {
        var ref = program.findVariable(name);
        assertTrue(ref.isPresent(), "variable not found: " + name);
        assertEquals(dataType, ref.get().dataType(), name);
        assertEquals(scopeType, ref.get().type(), name);
        return this;
    }

    public ProgramTester atEnd() {
        // allow automatic consumption of an END statement
        if (program.getStatements().size() > statementNumber) {
            end();
        }

        assertEquals(program.getStatements().size(), statementNumber);
        return this;
    }

    private <T> T nextStatement(Class<T> clazz) {
        Statement stmt = program.getStatements().get(statementNumber++);
        if (!stmt.getClass().isAssignableFrom(clazz)) {
            fail(String.format("expecting class %s at index %d", clazz.getName(), statementNumber));
        }
        return clazz.cast(stmt);
    }

    public ProgramTester lineNumber(int lineNumber) {
        return label(lineNumberLabel(lineNumber));
    }

    public ProgramTester label(String label) {
        var stmt = nextStatement(LabelStatement.class);
        assertEquals(label, stmt.getId());
        return this;
    }

    public ProgramTester assignment(String varName, Expression expr) {
        var stmt = nextStatement(AssignmentStatement.class);
        assertEquals(varName, stmt.getRef().name());
        assertEquals(expr, stmt.getExpr());
        return this;
    }

    public ProgramTester call(Expression addr) {
        var stmt = nextStatement(CallStatement.class);
        assertEquals(addr, stmt.getExpr());
        return this;
    }

    public ProgramTester callSub(String name, Expression... exprs) {
        var descriptor = CallSubroutine.getDescriptor(name);
        assertTrue(descriptor.isPresent(), "subroutine not found: " + name);
        var stmt = nextStatement(CallSubroutine.class);
        var fullName = String.format("%s_%s", descriptor.get().library(), name).toUpperCase();
        assertEquals(fullName, stmt.getName());
        assertEquals(descriptor.get().parameterTypes().length, exprs.length);
        for (int i=0; i<exprs.length; i++) {
            assertEquals(exprs[i], stmt.getParameters().get(i));
        }
        return this;
    }

    public ProgramTester end() {
        nextStatement(EndStatement.class);
        return this;
    }

    public ProgramTester forStmt(String name, Expression start, Expression end) {
        var stmt = nextStatement(ForStatement.class);
        assertEquals(name, stmt.getRef().name());
        assertEquals(start, stmt.getStart());
        assertEquals(end, stmt.getEnd());
        assertEquals(constant(1), stmt.getStep());
        return this;
    }

    public ProgramTester gosub(int lineNumber) {
        var stmt = nextStatement(GotoGosubStatement.class);
        assertEquals("GOSUB", stmt.getOp());
        assertEquals(lineNumberLabel(lineNumber), stmt.getId());
        return this;
    }

    public ProgramTester gotoStmt(int lineNumber) {
        var stmt = nextStatement(GotoGosubStatement.class);
        assertEquals("GOTO", stmt.getOp());
        assertEquals(lineNumberLabel(lineNumber), stmt.getId());
        return this;
    }

    public ProgramTester nextStmt(String name) {
        var stmt = nextStatement(NextStatement.class);
        assertEquals(name, stmt.getRef().name());
        return this;
    }

    public ProgramTester poke(Expression addr, Expression value) {
        var stmt = nextStatement(PokeStatement.class);
        assertEquals(addr, stmt.getA());
        assertEquals(value, stmt.getB());
        return this;
    }

    public ProgramTester returnStmt() {
        var stmt = nextStatement(ReturnStatement.class);
        return this;
    }
}
