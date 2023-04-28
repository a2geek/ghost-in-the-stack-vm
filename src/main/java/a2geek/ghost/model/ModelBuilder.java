package a2geek.ghost.model;

import a2geek.ghost.antlr.ParseUtil;
import a2geek.ghost.model.expression.FunctionExpression;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.*;
import org.antlr.v4.runtime.CharStreams;
import org.javatuples.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Function;

/**
 * A shared component to help building the BASIC model between language variants.
 */
public class ModelBuilder {
    private Function<String,String> caseStrategy;
    private Stack<Scope> scope = new Stack<>();
    private Stack<StatementBlock> statementBlock = new Stack<>();
    private Set<String> librariesIncluded = new HashSet<>();
    private boolean includeLibraries = true;

    public ModelBuilder(Function<String,String> caseStrategy) {
        this.caseStrategy = caseStrategy;
        Program program = new Program(caseStrategy);
        this.scope.push(program);
        this.statementBlock.push(program);
    }

    public Program getProgram() {
        if (!scope.isEmpty() && scope.get(0) instanceof Program program) {
            return program;
        }
        throw new RuntimeException("Program not found");
    }
    public Optional<Scope> findScope(String name) {
        return getProgram().findScope(fixCase(name));
    }

    public String fixCase(String id) {
        return caseStrategy.apply(id);
    }

    public void setIncludeLibraries(boolean includeLibraries) {
        this.includeLibraries = includeLibraries;
    }

    public void addStatement(Statement statement) {
        this.statementBlock.peek().addStatement(statement);
    }
    public StatementBlock pushStatementBlock(StatementBlock statementBlock) {
        return this.statementBlock.push(statementBlock);
    }
    public StatementBlock popStatementBlock() {
        return this.statementBlock.pop();
    }
    public void addScope(Scope scope) {
        this.scope.peek().addScope(scope);
    }
    public Scope pushScope(Scope scope) {
        return this.scope.push(scope);
    }
    public Scope popScope() {
        return this.scope.pop();
    }

    public Optional<Reference> findReference(String name) {
        return this.scope.peek().findReference(fixCase(name));
    }
    public Reference addVariable(String name, DataType dataType) {
        return this.scope.peek().addLocalVariable(fixCase(name), dataType);
    }
    public Reference addVariable(String name, Scope.Type type, DataType dataType) {
        return this.scope.peek().addLocalVariable(fixCase(name), type, dataType);
    }
    public Reference addConstant(String name, Expression value) {
        return this.scope.peek().addLocalConstant(fixCase(name), value);
    }

    public void uses(String libname) {
        if (!includeLibraries || librariesIncluded.contains(libname)) {
            return;
        }
        librariesIncluded.add(libname);
        String name = String.format("/library/%s.bas", libname);
        try (InputStream inputStream = getClass().getResourceAsStream(name)) {
            if (inputStream == null) {
                throw new RuntimeException("unknown library: " + libname);
            }
            Program library = ParseUtil.basicToModel(CharStreams.fromStream(inputStream),
                caseStrategy, v -> v.getModel().setIncludeLibraries(false));
            // at this time a library is simply a collection of subroutines and functions.
            boolean noStatements = library.getStatements().isEmpty();
            boolean onlyConstants = library.getLocalReferences().stream().noneMatch(ref -> ref.type() != Scope.Type.CONSTANT);
            if (!noStatements || !onlyConstants) {
                throw new RuntimeException("a library may only contain subroutines, functions, and constants");
            }
            // add subroutines and functions to our program!
            // constants are intentionally left off -- the included code has the reference and we don't want to clutter the namespace
            Program program = getProgram();
            library.getScopes().forEach(program::addScope);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void callLibrarySubroutine(String name, Expression... params) {
        var descriptor = CallSubroutine.getDescriptor(name).orElseThrow();
        uses(descriptor.library());
        callSubroutine(descriptor.fullName(), Arrays.asList(params));
    }

    public void callSubroutine(String name, List<Expression> params) {
        var subName = fixCase(name);
        // We can only validate for the primary program; libraries are trusted and sometimes circular!
        if (includeLibraries) {
            var subScope = getProgram().findScope(subName).orElse(null);
            if (subScope instanceof Subroutine) {
                // TODO validate argument count and types
            } else {
                throw new RuntimeException("subroutine does not exist: " + subName);
            }
        }
        CallSubroutine callSubroutine = new CallSubroutine(subName, params);
        addStatement(callSubroutine);
    }

    public FunctionExpression callFunction(String name, List<Expression> params) {
        var id = fixCase(name);
        if (FunctionExpression.isLibraryFunction(id)) {
            FunctionExpression.Descriptor descriptor = FunctionExpression.getDescriptor(id).orElseThrow();
            uses(descriptor.library());
            id = fixCase(descriptor.fullName());
        }

        Optional<Scope> scope = getProgram().findScope(id);
        if (scope.isPresent()) {
            if (scope.get() instanceof a2geek.ghost.model.scope.Function fn) {
                return new FunctionExpression(fn, params);
            }
        }
        else if (FunctionExpression.isIntrinsicFunction(id)) {
            return new FunctionExpression(id, params);
        }

        throw new RuntimeException("function does not exist: " + id);
    }

    public void assignStmt(Reference ref, Expression expr) {
        AssignmentStatement assignmentStatement = new AssignmentStatement(ref, expr);
        addStatement(assignmentStatement);
    }
    public void ifStmt(Expression expr, StatementBlock trueStatements, StatementBlock falseStatements) {
        IfStatement statement = new IfStatement(expr, trueStatements, falseStatements);
        addStatement(statement);
    }

    public void forBegin(Reference ref, Expression start, Expression end, Expression step) {
        ForNextStatement forStatement = new ForNextStatement(ref, start, end, step);
        pushStatementBlock(forStatement);
    }
    public void forEnd() {
        if (popStatementBlock() instanceof ForNextStatement forStatement) {
            addStatement(forStatement);
        }
        else {
            throw new RuntimeException("expecting for statement on stack");
        }
    }

    public void subDeclBegin(String name, List<Pair<String,DataType>> params) {
        Subroutine sub = new Subroutine(scope.peek(), fixCase(name), params);
        addScope(sub);

        pushScope(sub);
        pushStatementBlock(sub);
    }
    public void subDeclEnd() {
        // TODO does this need to be validated?
        popScope();
        popStatementBlock();
    }

    public void funcDeclBegin(String name, DataType returnType, List<Pair<String,DataType>> params) {
        // FIXME? naming is really awkward due to naming conflicts!
        a2geek.ghost.model.scope.Function func =
            new a2geek.ghost.model.scope.Function(scope.peek(),
                Pair.with(fixCase(name),returnType), params);
        addScope(func);

        pushScope(func);
        pushStatementBlock(func);
    }
    public void funcDeclEnd() {
        // TODO does this need to be validated?
        popScope();
        popStatementBlock();
    }

    public void endStmt() {
        addStatement(new EndStatement());
    }

    public void callAddr(Expression addr) {
        CallStatement callStatement = new CallStatement(addr);
        addStatement(callStatement);
    }

    public void pokeStmt(Expression addr, Expression value) {
        PokeStatement pokeStatement = new PokeStatement(addr, value);
        addStatement(pokeStatement);
    }

    public void labelStmt(String label) {
        LabelStatement labelStatement = new LabelStatement(fixCase(label));
        addStatement(labelStatement);
    }

    public void gotoGosubStmt(String op, String label) {
        GotoGosubStatement gotoGosubStatement = new GotoGosubStatement(op.toLowerCase(), fixCase(label));
        addStatement(gotoGosubStatement);
    }

    public void returnStmt(Expression expr) {
        ReturnStatement returnStatement = new ReturnStatement(expr);
        addStatement(returnStatement);
    }
}
