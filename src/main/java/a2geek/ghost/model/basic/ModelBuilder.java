package a2geek.ghost.model.basic;

import a2geek.ghost.antlr.ParseUtil;
import a2geek.ghost.model.basic.expression.*;
import a2geek.ghost.model.basic.scope.ForFrame;
import a2geek.ghost.model.basic.scope.Program;
import a2geek.ghost.model.basic.scope.Subroutine;
import a2geek.ghost.model.basic.statement.*;
import org.antlr.v4.runtime.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

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
    /** Tracking a distinct label number globally (regardless of scope) to prevent name collisions. */
    private static int labelNumber;

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

    public void insertStatement(Statement statement) {
        this.statementBlock.peek().insertStatement(statement);
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
        return this.scope.peek().addLocalSymbol(Symbol.variable(name, scope.peek().getType()).dataType(dataType));
    }
    public Symbol addVariable(String name, Scope.Type type, DataType dataType) {
        return this.scope.peek().addLocalSymbol(Symbol.variable(name, type).dataType(dataType));
    }
    public Symbol addArrayVariable(String name, DataType dataType, int numDimensions) {
        return this.scope.peek().addLocalSymbol(
                Symbol.variable(fixArrayName(name), scope.peek().getType())
                      .dataType(dataType)
                      .dimensions(numDimensions));
    }
    public Symbol addConstant(String name, Expression value) {
        return this.scope.peek().addLocalSymbol(Symbol.constant(name, value));
    }
    /** Generate labels for code. The multiple values is to allow grouping of labels (same label number) for complex structures. */
    public List<Symbol> addLabels(String... names) {
        labelNumber+= 1;
        List<Symbol> symbols = new ArrayList<>();
        for (var name : names) {
            var builder = Symbol.label(String.format("_%s%d", name, labelNumber));
            var symbol = this.scope.peek().addLocalSymbol(builder);
            symbols.add(symbol);
        }
        return symbols;
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
            || getProgram().findScope(id).map(s -> s instanceof a2geek.ghost.model.basic.scope.Function).orElse(false);
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
            if (scope.get() instanceof a2geek.ghost.model.basic.scope.Function fn) {
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

    public void subDeclBegin(String name, List<Symbol.Builder> params) {
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

    public void funcDeclBegin(String name, DataType returnType, List<Symbol.Builder> params) {
        // FIXME? naming is really awkward due to naming conflicts!
        a2geek.ghost.model.basic.scope.Function func =
            new a2geek.ghost.model.basic.scope.Function(scope.peek(),
                Symbol.variable(name, Scope.Type.RETURN_VALUE).dataType(returnType), params);
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

    public void labelStmt(Symbol label) {
        LabelStatement labelStatement = new LabelStatement(label);
        addStatement(labelStatement);
    }

    public void gotoGosubStmt(String op, Symbol label) {
        GotoGosubStatement gotoGosubStatement = new GotoGosubStatement(op.toLowerCase(), label);
        addStatement(gotoGosubStatement);
    }
    public void onGotoGosubStmt(String op, Expression expr, Supplier<List<Symbol>> labelFn, String altToString) {
        OnGotoGosubStatement stmt = new OnGotoGosubStatement(op.toLowerCase(), expr,
                labelFn, altToString);
        addStatement(stmt);
    }

    public void onGotoGosubStmt(String op, Expression expr, Supplier<List<Symbol>> labelFn) {
        var statement = new OnGotoGosubStatement(op.toLowerCase(), expr, labelFn);
        addStatement(statement);
    }

    public void returnStmt(Expression expr) {
        ReturnStatement returnStatement = new ReturnStatement(expr);
        addStatement(returnStatement);
    }

    public void insertDimArray(Symbol symbol, Expression size, List<Expression> defaultValues) {
        DimStatement dimStatement = new DimStatement(symbol, size, defaultValues);
        insertStatement(dimStatement);
        arrayDims.put(symbol, size);
    }
    public void addDimArray(Symbol symbol, Expression size, List<Expression> defaultValues) {
        DimStatement dimStatement = new DimStatement(symbol, size, defaultValues);
        addStatement(dimStatement);
        arrayDims.put(symbol, size);
    }
    public Expression getArrayDim(Symbol symbol) {
        if (symbol.type() == Scope.Type.PARAMETER) {
            // Expression has only one required method to implement at this time; may need to adjust in future!
            return symbol::dataType;
        } else if (!arrayDims.containsKey(symbol)) {
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
