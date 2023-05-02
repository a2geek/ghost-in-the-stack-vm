package a2geek.ghost.antlr;

import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.IdentifierExpression;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.statement.*;
import org.antlr.v4.runtime.CharStreams;

import java.util.List;
import java.util.Optional;

import static a2geek.ghost.antlr.ExpressionBuilder.constant;
import static org.junit.jupiter.api.Assertions.*;

public abstract class StatementTester {
    public static ProgramTester expect(String source) {
        ModelBuilder model = new ModelBuilder(String::toUpperCase);
        Program program = ParseUtil.integerToModel(CharStreams.fromString(source), model);
        System.out.println(program);
        return new ProgramTester(program);
    }

    private int statementNumber;

    abstract Optional<Symbol> findSymbol(String name);
    abstract List<Statement> getStatements();

    private String lineNumberLabel(int lineNumber) {
        return String.format("L%d", lineNumber);
    }

    public StatementTester hasSymbol(String name, DataType dataType, Scope.Type scopeType) {
        var symbol = findSymbol(name);
        assertTrue(symbol.isPresent(), "variable not found: " + name);
        assertEquals(dataType, symbol.get().dataType(), name);
        assertEquals(scopeType, symbol.get().type(), name);
        return this;
    }

    public StatementTester hasSymbol(IdentifierExpression expr) {
        return hasSymbol(expr.getSymbol());
    }
    public StatementTester hasSymbol(Symbol symbol) {
        return hasSymbol(symbol.name(), symbol.dataType(), symbol.type());
    }

    public StatementTester atEnd() {
        // allow automatic consumption of an END statement
        if (getStatements().size() > statementNumber) {
            end();
        }

        assertEquals(getStatements().size(), statementNumber);
        return this;
    }

    protected <T> T nextStatement(Class<T> clazz) {
        Statement stmt = getStatements().get(statementNumber++);
        if (!stmt.getClass().isAssignableFrom(clazz)) {
            fail(String.format("expecting class %s at index %d but found '%s'",
                clazz.getName(), statementNumber, stmt));
        }
        return clazz.cast(stmt);
    }

    public StatementTester lineNumber(int lineNumber) {
        return label(lineNumberLabel(lineNumber));
    }

    public StatementTester label(String label) {
        var stmt = nextStatement(LabelStatement.class);
        assertEquals(label, stmt.getId());
        return this;
    }

    public StatementTester assignment(String varName, Expression expr) {
        var stmt = nextStatement(AssignmentStatement.class);
        assertEquals(varName, stmt.getSymbol().name());
        assertEquals(expr, stmt.getExpr());
        return this;
    }

    public StatementTester call(Expression addr) {
        var stmt = nextStatement(CallStatement.class);
        assertEquals(addr, stmt.getExpr());
        return this;
    }

    public StatementTester callSub(String name, Expression... exprs) {
        var descriptor = CallSubroutine.getDescriptor(name);
        assertTrue(descriptor.isPresent(), "subroutine not found: " + name);
        var stmt = nextStatement(CallSubroutine.class);
        var fullName = descriptor.get().fullName().toUpperCase();
        assertEquals(fullName, stmt.getName());
        assertEquals(descriptor.get().parameterTypes().length, exprs.length);
        for (int i=0; i<exprs.length; i++) {
            assertEquals(exprs[i], stmt.getParameters().get(i));
        }
        return this;
    }

    public StatementTester end() {
        nextStatement(EndStatement.class);
        return this;
    }

    public StatementTester forStmt(String name, Expression start, Expression end) {
        var stmt = nextStatement(ForStatement.class);
        assertEquals(name, stmt.getSymbol().name());
        assertEquals(start, stmt.getStart());
        assertEquals(end, stmt.getEnd());
        assertEquals(constant(1), stmt.getStep());
        return this;
    }

    public StatementTester gosub(int lineNumber) {
        var stmt = nextStatement(GotoGosubStatement.class);
        assertEquals("gosub", stmt.getOp());
        assertEquals(lineNumberLabel(lineNumber), stmt.getId());
        return this;
    }

    public StatementTester gotoStmt(int lineNumber) {
        var stmt = nextStatement(GotoGosubStatement.class);
        assertEquals("goto", stmt.getOp());
        assertEquals(lineNumberLabel(lineNumber), stmt.getId());
        return this;
    }

    public StatementTester ifStmt(Expression expr) {
        var stmt = nextStatement(IfStatement.class);
        return new IfStatementTester(this, stmt.getTrueStatements(), stmt.getFalseStatements());
    }
    public StatementTester elseStmt() {
        throw new RuntimeException("not an if statement");
    }
    public StatementTester endIf() {
        throw new RuntimeException("not an if statement");
    }

    public StatementTester nextStmt(String name) {
        var stmt = nextStatement(NextStatement.class);
        assertEquals(name, stmt.getSymbol().name());
        return this;
    }

    public StatementTester poke(Expression addr, Expression value) {
        var stmt = nextStatement(PokeStatement.class);
        assertEquals("poke", stmt.getOp());
        assertEquals(addr, stmt.getA());
        assertEquals(value, stmt.getB());
        return this;
    }

    public StatementTester returnStmt() {
        var stmt = nextStatement(ReturnStatement.class);
        return this;
    }

    public static class ProgramTester extends StatementTester {
        private Program program;

        public ProgramTester(Program program) {
            this.program = program;
        }
        @Override
        public Optional<Symbol> findSymbol(String name) {
            return program.findSymbol(name);
        }
        @Override
        public List<Statement> getStatements() {
            return program.getStatements();
        }
    }

    public static class IfStatementTester extends StatementTester {
        private StatementTester parent;
        private StatementBlock block;
        private StatementBlock nextBlock;

        public IfStatementTester(StatementTester parent, StatementBlock block, StatementBlock nextBlock) {
            this.parent = parent;
            this.block = block;
            this.nextBlock = block;
        }

        @Override
        public Optional<Symbol> findSymbol(String name) {
            return parent.findSymbol(name);
        }
        @Override
        public List<Statement> getStatements() {
            return block.getStatements();
        }
        @Override
        public StatementTester elseStmt() {
            if (nextBlock == null) {
                throw new RuntimeException("no else for if statement");
            }
            return new IfStatementTester(parent, nextBlock, null);
        }
        @Override
        public StatementTester endIf() {
            atEnd();
            return parent;
        }
    };
}
