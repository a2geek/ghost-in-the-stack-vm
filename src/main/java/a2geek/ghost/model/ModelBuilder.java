package a2geek.ghost.model;

import a2geek.ghost.antlr.ParseUtil;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.ForFrame;
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
    private Function<String,String> arrayNameStrategy = s -> s;
    private Function<String,String> controlCharsFn = s -> s;
    private Stack<Scope> scope = new Stack<>();
    private Stack<StatementBlock> statementBlock = new Stack<>();
    private Set<String> librariesIncluded = new HashSet<>();
    private boolean trace = false;
    private boolean boundsCheck = true;
    private boolean includeLibraries = true;
    /** Traditional FOR/NEXT statement frame. */
    private Map<Symbol, ForFrame> forFrames = new HashMap<>();
    /** Track array dimensions */
    private Map<Symbol, Expression> arrayDims = new HashMap<>();

    public ModelBuilder(Function<String,String> caseStrategy) {
        this.caseStrategy = caseStrategy;
        Program program = new Program(caseStrategy);
        this.scope.push(program);
        this.statementBlock.push(program);
    }
    private ModelBuilder(ModelBuilder parent) {
        this(parent.caseStrategy);
        this.trace = parent.trace;
        this.boundsCheck = parent.boundsCheck;
        this.includeLibraries = parent.includeLibraries;
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
    public String fixArrayName(String name) {
        return arrayNameStrategy.apply(name);
    }
    public String fixControlChars(String value) {
        return controlCharsFn.apply(value);
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
    }
    public void setBoundsCheck(boolean boundsCheck) {
        this.boundsCheck = boundsCheck;
    }
    public void setIncludeLibraries(boolean includeLibraries) {
        this.includeLibraries = includeLibraries;
    }
    public void setArrayNameStrategy(Function<String,String> arrayNameStrategy) {
        this.arrayNameStrategy = arrayNameStrategy;
    }
    public void setControlCharsFn(Function<String,String> controlCharsFn) {
        this.controlCharsFn = controlCharsFn;
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
    public boolean isCurrentScope(Class<? extends Scope> clazz) {
        return clazz.isAssignableFrom(this.scope.peek().getClass());
    }
    public <T extends Scope> Optional<T> findScope(Class<T> clazz) {
        for (int i = this.scope.size() - 1; i >= 0; i--) {
            if (clazz.isAssignableFrom(this.scope.get(i).getClass())) {
                return Optional.of(clazz.cast(this.scope.get(i)));
            }
        }
        return Optional.empty();
    }
    public <T extends StatementBlock> Optional<T> findBlock(Class<T> clazz) {
        for (int i=this.statementBlock.size()-1; i>=0; i--) {
            if (clazz.isAssignableFrom(this.statementBlock.get(i).getClass())) {
                return Optional.of(clazz.cast(this.statementBlock.get(i)));
            }
        }
        return Optional.empty();
    }

    public Optional<Symbol> findSymbol(String name) {
        return this.scope.peek().findSymbol(fixCase(name));
    }
    public Symbol addVariable(String name, DataType dataType) {
        return this.scope.peek().addLocalVariable(fixCase(name), dataType);
    }
    public Symbol addVariable(String name, Scope.Type type, DataType dataType) {
        return this.scope.peek().addLocalVariable(fixCase(name), type, dataType);
    }
    public Symbol addArrayVariable(String name, DataType dataType, int numDimensions) {
        return this.scope.peek().addArrayVariable(fixArrayName(name), dataType, numDimensions);
    }
    public Symbol addConstant(String name, Expression value) {
        return this.scope.peek().addLocalConstant(fixCase(name), value);
    }

    public ForFrame forFrame(Symbol symbol) {
        return forFrames.computeIfAbsent(symbol, r -> new ForFrame(r, scope.peek()));
    }

    public void trace(String fmt, Object... args) {
        if (trace) {
            System.out.printf(fmt, args);
            System.out.println();
        }
    }

    public void uses(String libname) {
        if (!includeLibraries || librariesIncluded.contains(libname)) {
            return;
        }
        trace("loading library: %s", libname);
        librariesIncluded.add(libname);
        String name = String.format("/library/%s.bas", libname);
        try (InputStream inputStream = getClass().getResourceAsStream(name)) {
            if (inputStream == null) {
                throw new RuntimeException("unknown library: " + libname);
            }
            ModelBuilder libraryModel = new ModelBuilder(this);
            libraryModel.setIncludeLibraries(false);
            Program library = ParseUtil.basicToModel(CharStreams.fromStream(inputStream), libraryModel);
            // at this time a library is simply a collection of subroutines and functions.
            boolean noStatements = library.getStatements().isEmpty();
            boolean onlyConstants = library.getLocalSymbols().stream().noneMatch(ref -> ref.type() != Scope.Type.CONSTANT);
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

    public boolean isFunction(String name) {
        var id = fixCase(name);
        return FunctionExpression.isLibraryFunction(id)
            || FunctionExpression.isIntrinsicFunction(id)
            || getProgram().findScope(id).map(s -> s instanceof a2geek.ghost.model.scope.Function).orElse(false);
    }

    public FunctionExpression callFunction(String name, List<Expression> params) {
        var id = fixCase(name);
        if (FunctionExpression.isLibraryFunction(id)) {
            FunctionExpression.Descriptor descriptor = FunctionExpression.getDescriptor(id).orElseThrow();
            var requiredParameterCount = descriptor.parameterTypes().length;
            if (params.size() != requiredParameterCount) {
                var msg = String.format("function '%s' requires %d parameters", id, requiredParameterCount);
                throw new RuntimeException(msg);
            }
            uses(descriptor.library());
            id = fixCase(descriptor.fullName());
        }

        Optional<Scope> scope = getProgram().findScope(id);
        if (scope.isPresent()) {
            if (scope.get() instanceof a2geek.ghost.model.scope.Function fn) {
                var requiredParameterCount = fn.findByType(Scope.Type.PARAMETER).size();
                if (params.size() != requiredParameterCount) {
                    var msg = String.format("function '%s' requires %d parameters", id, requiredParameterCount);
                    throw new RuntimeException(msg);
                }
                return new FunctionExpression(fn, params);
            }
        }
        else if (FunctionExpression.isIntrinsicFunction(id)) {
            return new FunctionExpression(id, params);
        }

        throw new RuntimeException("function does not exist: " + id);
    }

    public void assignStmt(VariableReference ref, Expression expr) {
        AssignmentStatement assignmentStatement = new AssignmentStatement(ref, expr);
        addStatement(assignmentStatement);
    }
    public void ifStmt(Expression expr, StatementBlock trueStatements, StatementBlock falseStatements) {
        IfStatement statement = new IfStatement(expr, trueStatements, falseStatements);
        addStatement(statement);
    }

    public void forBegin(Symbol symbol, Expression start, Expression end, Expression step) {
        ForNextStatement forStatement = new ForNextStatement(symbol, start, end, step);
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

    public void loopBegin(DoLoopStatement.Operation op, Expression test) {
        DoLoopStatement doStatement = new DoLoopStatement(op, test);
        pushStatementBlock(doStatement);
    }
    public void loopEnd() {
        if (popStatementBlock() instanceof DoLoopStatement doStatement) {
            addStatement(doStatement);
        }
        else {
            throw new RuntimeException("expecting do loop statement on stack");
        }
    }

    public void exitStmt(String op) {
        addStatement(new ExitStatement(op));
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

    public void pokeStmt(String op, Expression addr, Expression value) {
        PokeStatement pokeStatement = new PokeStatement(op, addr, value);
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

    public void addDimArray(Symbol symbol, Expression size, List<Expression> defaultValues) {
        DimStatement dimStatement = new DimStatement(symbol, size, defaultValues);
        addStatement(dimStatement);
        arrayDims.put(symbol, size);
    }
    public Expression getArrayDim(Symbol symbol) {
        if (!arrayDims.containsKey(symbol)) {
            throw new RuntimeException("Array was not DIMmed: " + symbol.name());
        }
        return arrayDims.get(symbol);
    }

    public void checkArrayBounds(Symbol symbol, Expression index, int linenum) {
        if (!boundsCheck) {
            return;
        }
        // IF index > ubound(array) THEN
        //    PRINT "Array index out of bounds ";symbol;" at line "; lineno
        //    END
        // END IF
        var errorBlock = pushStatementBlock(new StatementBlock());
        callLibrarySubroutine("out_of_bounds",
            new StringConstant(symbol.name()),
            new IntegerConstant(linenum));
        popStatementBlock();
        var arrayLength = new ArrayLengthFunction(this, symbol);
        ifStmt(new BinaryExpression(index, arrayLength, ">"), errorBlock, null);
    }
}
