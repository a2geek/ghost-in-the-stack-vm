package a2geek.ghost.antlr;

import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.VariableReference;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.*;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.*;

public abstract class StatementTester {
    private Function<String,String> caseStrategy;
    private Function<String,String> arrayNameStrategy;
    private int statementNumber;

    public StatementTester(
        Function<String,String> caseStrategy,
        Function<String,String> arrayNameStrategy
    ) {
        this.caseStrategy = caseStrategy;
        this.arrayNameStrategy = arrayNameStrategy;
    }

    abstract Optional<Symbol> findSymbol(String name);
    abstract List<Statement> getStatements();
    abstract Optional<Scope> findScope(String name);

    public String fixCase(String value) {
        return caseStrategy.apply(value);
    }
    public String fixArrayName(String name) {
        return arrayNameStrategy.apply(name);
    }

    private String lineNumberLabel(int lineNumber) {
        return String.format("L%d_", lineNumber);
    }

    public StatementTester hasSymbol(String name, DataType dataType, Scope.Type scopeType) {
        name = fixCase(name);
        var symbol = findSymbol(name);
        assertTrue(symbol.isPresent(), "variable not found: " + name);
        assertEquals(dataType, symbol.get().dataType(), name);
        assertEquals(scopeType, symbol.get().type(), name);
        return this;
    }

    public StatementTester hasArrayReference(String name, DataType dataType, Scope.Type scopeType, int numDimensions) {
        name = fixCase(name);
        var symbol = findSymbol(fixArrayName(name));
        assertTrue(symbol.isPresent(), "array variable not found: " + name);
        assertEquals(dataType, symbol.get().dataType(), name);
        assertEquals(scopeType, symbol.get().type(), name);
        assertEquals(numDimensions, symbol.get().numDimensions());
        return this;
    }

    public StatementTester hasSymbol(VariableReference expr) {
        return hasSymbol(expr.getSymbol());
    }
    public StatementTester hasSymbol(Symbol symbol) {
        return hasSymbol(symbol.name(), symbol.dataType(), symbol.type());
    }

    public void fail(String message) {
        Assertions.fail(message);
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
        var pattern = Pattern.compile(String.format("_%s\\d+", label), Pattern.CASE_INSENSITIVE);
        assertThat(stmt.getLabel().name(), matchesPattern(pattern));
        return this;
    }

    public StatementTester assignment(String varName, Expression expr) {
        var stmt = nextStatement(AssignmentStatement.class);
        assertFalse(stmt.getVar().isArray());
        assertEquals(fixCase(varName), stmt.getVar().getSymbol().name());
        assertEquals(expr, stmt.getExpr());
        return this;
    }

    public StatementTester arrayAssignment(String varName, Expression index, Expression expr) {
        varName = fixCase(varName);
        var stmt = nextStatement(AssignmentStatement.class);
        assertEquals(fixArrayName(varName), stmt.getVar().getSymbol().name());
        return this;
    }

    public StatementTester call(Expression addr) {
        var stmt = nextStatement(CallStatement.class);
        assertEquals(addr, stmt.getExpr());
        return this;
    }

    public StatementTester callLibrarySub(String name, Expression... params) {
        name = fixCase(name);
        var descriptor = CallSubroutine.getDescriptor(name);
        assertTrue(descriptor.isPresent(), "subroutine not found: " + name);
        var stmt = nextStatement(CallSubroutine.class);
        var fullName = descriptor.get().fullName().toUpperCase();
        assertEquals(fullName, stmt.getName());
        assertEquals(descriptor.get().parameterTypes().length, params.length);
        for (int i=0; i<params.length; i++) {
            assertEquals(params[i], stmt.getParameters().get(i));
        }
        return this;
    }

    public StatementTester callSub(String name, Expression... params) {
        var stmt = nextStatement(CallSubroutine.class);
        assertEquals(fixCase(name), stmt.getName());
        assertEquals(stmt.getParameters().size(), params.length);
        for (int i=0; i<params.length; i++) {
            assertEquals(params[i], stmt.getParameters().get(i));
        }
        return this;
    }

    public StatementTester dimStmt(String name, Expression expr) {
        name = fixCase(name);
        var stmt = nextStatement(DimStatement.class);
        assertEquals(fixArrayName(name), stmt.getSymbol().name());
        assertEquals(expr, stmt.getExpr());
        return this;
    }

    public StatementTester dimDefaultStmt(String name, List<Expression> defaultValues) {
        name = fixCase(name);
        var stmt = nextStatement(DimStatement.class);
        assertEquals(fixArrayName(name), stmt.getSymbol().name());
        assertEquals(DataType.INTEGER, stmt.getSymbol().dataType());
        assertEquals(defaultValues, stmt.getDefaultValues());
        return this;
    }

    public StatementTester end() {
        nextStatement(EndStatement.class);
        return this;
    }

    public StatementTester gosub(int lineNumber) {
        return gosub(lineNumberLabel(lineNumber));
    }
    public StatementTester gosub(String label) {
        var stmt = nextStatement(GotoGosubStatement.class);
        assertEquals("gosub", stmt.getOp());
        var pattern = Pattern.compile(String.format("_%s\\d+", label), Pattern.CASE_INSENSITIVE);
        assertThat(stmt.getLabel().name(), matchesPattern(pattern));
        return this;
    }

    public StatementTester gotoStmt(int lineNumber) {
        return gotoStmt(lineNumberLabel(lineNumber));
    }
    public StatementTester gotoStmt(String label) {
        var stmt = nextStatement(GotoGosubStatement.class);
        assertEquals("goto", stmt.getOp());
        var pattern = Pattern.compile(String.format("_%s\\d+", label), Pattern.CASE_INSENSITIVE);
        assertThat(stmt.getLabel().name(), matchesPattern(pattern));
        return this;
    }

    public StatementTester skipIfStmt() {
        nextStatement(IfStatement.class);
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

    public StatementTester endBlock() {
        throw new RuntimeException("not a do...loop statement");
    }

    public StatementTester poke(String op, Expression addr, Expression value) {
        var stmt = nextStatement(PokeStatement.class);
        assertEquals(op, stmt.getOp());
        assertEquals(addr, stmt.getA());
        assertEquals(value, stmt.getB());
        return this;
    }

    public StatementTester pop() {
        nextStatement(PopStatement.class);
        return this;
    }

    public StatementTester returnStmt(Expression expr) {
        var stmt = nextStatement(ReturnStatement.class);
        if (expr == null) {
            if (stmt.getExpr() != null) {
                throw new RuntimeException(
                    "expecting no return value: " + stmt);
            }
        }
        else {
            if (!expr.equals(stmt.getExpr())) {
                throw new RuntimeException(
                    "return expression does not match: " + stmt);
            }
        }
        return this;
    }

    /** Validate that all parameters are in the correct order and have correct name and type. */
    void checkParameters(Scope scope, List<Symbol> parameters) {
        List<Symbol> symbols = scope.findByType(Scope.Type.PARAMETER);
        assertEquals(parameters.size(), symbols.size());
        for (int i=0; i<parameters.size(); i++) {
            Symbol symbol = symbols.get(i);
            Symbol parameter = parameters.get(i);
            assertEquals(fixCase(parameter.name()), fixCase(symbol.name()));
            assertEquals(parameter.dataType(), symbol.dataType());
            assertEquals(parameter.numDimensions(), symbol.numDimensions());
        }
    }

    public StatementTester subScope(String name, List<Symbol> parameters) {
        var scope = findScope(fixCase(name));
        if (scope.isPresent()) {
            if (scope.get() instanceof Subroutine sub) {
                checkParameters(sub, parameters);
                return new ScopeTester(this, sub);
            }
            else {
                throw new RuntimeException("scope expected to be a subroutine: " + name);
            }
        }
        throw new RuntimeException("scope not found: " + name);
    }
    public StatementTester functionScope(String name, List<Symbol> parameters,
                                         DataType returnType) {
        var scope = findScope(fixCase(name));
        if (scope.isPresent()) {
            if (scope.get() instanceof a2geek.ghost.model.scope.Function function) {
                assertEquals(returnType, function.getDataType());
                checkParameters(function, parameters);
                return new ScopeTester(this, function);
            }
            else {
                throw new RuntimeException("scope expected to be a function: " + name);
            }
        }
        throw new RuntimeException("scope not found: " + name);
    }
    public StatementTester endScope() {
        throw new RuntimeException("not in a scope");
    }

    public static class ScopeTester extends StatementTester {
        private StatementTester parent;
        private Scope scope;

        public ScopeTester(StatementTester parent, Scope scope) {
            super(parent.caseStrategy, parent.arrayNameStrategy);
            this.parent = parent;
            this.scope = scope;
        }
        public ScopeTester(
            Scope scope,
            Function<String,String> caseStrategy,
            Function<String,String> arrayNameStrategy
        ) {
            super(caseStrategy, arrayNameStrategy);
            this.scope = scope;
        }
        @Override
        public Optional<Symbol> findSymbol(String name) {
            return scope.findSymbol(name);
        }
        @Override
        public List<Statement> getStatements() {
            return scope.getStatements();
        }
        @Override
        Optional<Scope> findScope(String name) {
            return scope.findLocalScope(name);
        }
        @Override
        public StatementTester endScope() {
            return parent;
        }
    }

    public static class IfStatementTester extends StatementTester {
        private StatementTester parent;
        private StatementBlock block;
        private StatementBlock nextBlock;

        public IfStatementTester(StatementTester parent, StatementBlock block, StatementBlock nextBlock) {
            super(parent.caseStrategy, parent.arrayNameStrategy);
            this.parent = parent;
            this.block = block;
            this.nextBlock = nextBlock;
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
        Optional<Scope> findScope(String name) {
            return Optional.empty();
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
    }

    public static class StatementBlockTester extends StatementTester {
        private StatementTester parent;
        private StatementBlock block;

        public StatementBlockTester(StatementTester parent, StatementBlock block) {
            super(parent.caseStrategy, parent.arrayNameStrategy);
            this.parent = parent;
            this.block = block;
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
        Optional<Scope> findScope(String name) {
            return Optional.empty();
        }

        @Override
        public StatementTester endBlock() {
            return parent;
        }
    }
}
