package a2geek.ghost.model;

import a2geek.ghost.antlr.ParseUtil;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.*;
import org.antlr.v4.runtime.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static a2geek.ghost.model.Symbol.in;
import static a2geek.ghost.model.Symbol.named;

/**
 * A shared component to help building the BASIC model between language variants.
 */
public class ModelBuilder {
    public static final String ERR_LIBRARY = "err";
    public static final String LORES_LIBRARY = "lores";
    public static final String MEMORY_LIBRARY = "memory";
    public static final String MISC_LIBRARY = "misc";
    public static final String MATH_LIBRARY = "math";
    public static final String RUNTIME_LIBRARY = "runtime";
    public static final String STRINGS_LIBRARY = "strings";
    public static final String TEXT_LIBRARY = "text";

    private final Function<String,String> caseStrategy;
    private Function<String,String> arrayNameStrategy = s -> s;
    private Function<String,String> controlCharsFn = s -> s;
    private Stack<Scope> scope = new Stack<>();
    private final Stack<StatementBlock> statementBlock = new Stack<>();
    // TODO determine if this is required any more!
    private final Set<String> librariesIncluded = new HashSet<>();
    private boolean trace = false;
    private boolean boundsCheck = true;
    private String heapFunction;
    /** Track array dimensions */
    private final Map<Symbol, Expression> arrayDims = new HashMap<>();

    public ModelBuilder(Function<String,String> caseStrategy) {
        this.caseStrategy = caseStrategy;
        useStackForHeap();  // default
        Program program = new Program(caseStrategy);
        var onerr = OnErrorContext.createPrimary(program);     // always (assumed)
        program.setOnErrorContext(onerr);
        this.scope.push(program);
        this.statementBlock.push(program);
    }

    public Program getProgram() {
        if (!scope.isEmpty() && scope.getFirst() instanceof Program program) {
            return program;
        }
        throw new RuntimeException("Program not found");
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
    public void enableBoundsCheck(boolean boundsCheck) {
        this.boundsCheck = boundsCheck;
    }
    public void setArrayNameStrategy(Function<String,String> arrayNameStrategy) {
        this.arrayNameStrategy = arrayNameStrategy;
    }
    public void setControlCharsFn(Function<String,String> controlCharsFn) {
        this.controlCharsFn = controlCharsFn;
    }

    public void useStackForHeap() {
        this.heapFunction = "alloc";
    }
    public void useMemoryForHeap(int startAddress) {
        this.heapFunction = "memory.heapalloc";
        this.pushStatementBlock(new StatementBlock());
        pokeStmt("pokew", new IntegerConstant(0x69), new IntegerConstant(startAddress));
        var sb = this.popStatementBlock();
        this.addInitializationStatements(sb);
    }
    public boolean isUsingMemory() {
        return !"alloc".equals(this.heapFunction);
    }

    public void addInitializationStatements(StatementBlock statements) {
        var sb = this.statementBlock.peek();
        statements.getInitializationStatements().forEach(sb::addInitializationStatement);
        statements.getStatements().forEach(sb::addInitializationStatement);
    }
    public void addStatement(Statement statement) {
        this.statementBlock.peek().addStatement(statement);
    }
    public void addStatements(StatementBlock statements) {
        Objects.requireNonNull(statements);
        final var sb = statementBlock.peek();
        statements.getInitializationStatements().forEach(sb::addInitializationStatement);
        statements.getStatements().forEach(sb::addStatement);
    }
    public StatementBlock pushStatementBlock(StatementBlock statementBlock) {
        return this.statementBlock.push(statementBlock);
    }
    public StatementBlock popStatementBlock() {
        return this.statementBlock.pop();
    }
    public Scope pushScope(Scope scope) {
        return this.scope.push(scope);
    }
    public Scope popScope() {
        return this.scope.pop();
    }
    public Scope peekScope() {
        return this.scope.peek();
    }
    public boolean isCurrentScope(Class<? extends Scope> clazz) {
        return clazz.isAssignableFrom(this.scope.peek().getClass());
    }

    public Optional<Symbol> findSymbol(String name) {
        return this.scope.peek().findFirst(named(fixCase(name)));
    }
    public Symbol addVariable(String name, DataType dataType) {
        return this.scope.peek().addLocalSymbol(Symbol.variable(name, SymbolType.VARIABLE).dataType(dataType));
    }
    public Symbol addVariable(String name, SymbolType type, DataType dataType) {
        return this.scope.peek().addLocalSymbol(Symbol.variable(name, type).dataType(dataType));
    }
    public Symbol addArrayVariable(String name, DataType dataType, List<Expression> dimensions) {
        return this.scope.peek().addLocalSymbol(
                Symbol.variable(fixArrayName(name), SymbolType.VARIABLE)
                      .dataType(dataType)
                      .dimensions(dimensions));
    }
    public Symbol addArrayDefaultVariable(String name, DataType dataType, List<Expression> dimensions,
                                          List<Expression> defaultValues) {
        return this.scope.peek().addLocalSymbol(
            Symbol.variable(fixArrayName(name), SymbolType.VARIABLE)
                .dataType(dataType)
                .dimensions(dimensions)
                .defaultValues(defaultValues));
    }

    public Symbol addConstant(String name, Expression value) {
        return this.scope.peek().addLocalSymbol(Symbol.constant(name, value));
    }
    public Symbol addTempVariable(DataType dataType) {
        return this.scope.peek().addTempVariable(dataType);
    }
    /** Generate labels for code. The multiple values is to allow grouping of labels (same label number) for complex structures. */
    public List<Symbol> addLabels(String... names) {
        return this.scope.peek().addLabels(names);
    }

    public void trace(String fmt, Object... args) {
        if (trace) {
            System.out.printf(fmt, args);
            System.out.println();
        }
    }

    public void uses(String libname, Predicate<Symbol> exportHandler) {
        var program = getProgram();
        if (!librariesIncluded.contains(libname)) {
            librariesIncluded.add(libname);
            trace("loading library: %s", libname);
            String name = String.format("/library/%s.bas", libname);
            try (InputStream inputStream = getClass().getResourceAsStream(name)) {
                if (inputStream == null) {
                    throw new RuntimeException("unknown library: " + libname);
                }
                // These gyrations are to ensure that anything included is at the PROGRAM level and not in some sub-scope
                // (such as FUNCTION, SUB, or MODULE).
                var oldScopeStack = this.scope;
                this.scope = new Stack<>();
                this.scope.push(program);
                ParseUtil.basicToModel(CharStreams.fromStream(inputStream), this);
                this.scope = oldScopeStack;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        // Apply the export strategy to all components of the module (note we do this every time)
        var module = program.findFirst(named(fixCase(libname)).and(in(SymbolType.MODULE))).map(Symbol::scope)
                .orElseThrow(() -> new RuntimeException("not a module: " + libname));
        var exports = module.streamAllLocalScope().filter(exportHandler).map(Symbol::name).toList();
        module.addAllExports(exports);
    }
    public static Predicate<Symbol> defaultExport() {
        return symbol -> symbol.scope() instanceof Subroutine sub && sub.is(Subroutine.Modifier.EXPORT);
    }
    public static Predicate<Symbol> nothingExported() {
        return symbol -> false;
    }
    public static Predicate<Symbol> exportSpecified(String... names) {
        final var set = Set.of(names);
        return symbol -> symbol.scope() instanceof Subroutine sub && set.contains(sub.getName());
    }

    public void callLibrarySubroutine(String name, Expression... params) {
        var descriptor = CallSubroutine.getDescriptor(name).orElseThrow();
        uses(descriptor.library(), nothingExported());
        callSubroutine(descriptor.fullName(), Arrays.asList(params));
    }

    public void callSubroutine(String name, List<Expression> params) {
        ensureModuleIncluded(name);
        var subName = fixCase(name);
        var subScope = this.scope.peek().findFirst(named(subName).and(in(SymbolType.SUBROUTINE)))
                .map(Symbol::scope).orElse(null);
        if (subScope instanceof Subroutine sub) {
            checkCallParameters(sub, params);
            CallSubroutine callSubroutine = new CallSubroutine(sub, params);
            addStatement(callSubroutine);
        } else {
            throw new RuntimeException("subroutine does not exist: " + subName);
        }
    }

    public void ensureModuleIncluded(String fullPathName) {
        var parts = fullPathName.split("\\.");
        if (parts.length == 2) {
            uses(parts[0].toLowerCase(), nothingExported());
        }
    }

    void checkCallParameters(Subroutine subOrFunc, List<Expression> params) {
        var requiredParameters = subOrFunc.findAllLocalScope(in(SymbolType.PARAMETER));
        if (params.size() != requiredParameters.size()) {
            // fixme - this should be fixed for clarity
            var msg = String.format("sub/func '%s' requires %d parameters", subOrFunc.getName(),
                    requiredParameters.size());
            throw new RuntimeException(msg);
        }
        params = params.reversed(); // call parameters are reversed fixing to make comparison easier
        for (int i = 0; i<requiredParameters.size(); i++) {
            var param = params.get(i);
            var requiredParam = requiredParameters.get(i);
            try {
                params.set(i, param.checkAndCoerce(requiredParam.dataType()));
            }
            catch (RuntimeException ex) {
                var msg = String.format("sub/func '%s' parameter %d is not of type %s: %s", subOrFunc.getName(),
                        i, requiredParam.dataType(), ex.getMessage());
                throw new RuntimeException(msg);
            }
        }

    }

    public boolean isFunction(String name) {
        if (name.contains(".")) {
            ensureModuleIncluded(name);
        }
        var id = fixCase(name);
        return FunctionExpression.isLibraryFunction(id)
            || FunctionExpression.isIntrinsicFunction(id)
            || scope.peek().findFirst(named(id).and(in(SymbolType.FUNCTION))).isPresent();
    }

    public Expression callFunction(String name, Expression... params) {
        return callFunction(name, Arrays.asList(params));
    }
    public Expression callFunction(String name, List<Expression> params) {
        ensureModuleIncluded(name);
        var id = fixCase(name);
        if (FunctionExpression.isLibraryFunction(id)) {
            FunctionExpression.Descriptor descriptor = FunctionExpression.findDescriptor(id, params).orElseThrow();
            uses(descriptor.library(), nothingExported());
            id = fixCase(descriptor.fullName());
        }

        var func = this.scope.peek().findFirst(named(id).and(in(SymbolType.FUNCTION)))
                .map(Symbol::scope).orElse(null);
        if (func instanceof a2geek.ghost.model.scope.Function fn) {
            checkCallParameters(fn, params);
            return new FunctionExpression(fn, params);
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
    public void assignStmt(UnaryExpression unary, Expression expr) {
        AssignmentStatement assignmentStatement = new AssignmentStatement(unary, expr);
        addStatement(assignmentStatement);
    }
    public void ifStmt(Expression expr, StatementBlock trueStatements, StatementBlock falseStatements) {
        IfStatement statement = new IfStatement(expr, trueStatements, falseStatements, SourceType.CODE);
        addStatement(statement);
    }

    public Scope moduleDeclBegin(String name) {
        trace("compiling module '%s'", name);
        Scope module = new Scope(scope.peek(), fixCase(name), DeclarationType.GLOBAL);
        this.scope.peek().addLocalSymbol(Symbol.scope(module));

        pushScope(module);
        pushStatementBlock(module);
        return module;
    }
    public void moduleDeclEnd() {
        popScope();
        popStatementBlock();
    }

    public Subroutine subDeclBegin(String name, List<Symbol.Builder> params) {
        trace("compiling subroutine '%s'", name);
        Subroutine sub = new Subroutine(scope.peek(), fixCase(name), params);
        this.scope.peek().addLocalSymbol(Symbol.scope(sub));

        pushScope(sub);
        pushStatementBlock(sub);
        return sub;
    }
    public void subDeclEnd() {
        // TODO does this need to be validated?
        Scope sub = popScope();
        popStatementBlock();
        setupOnErrorContext(sub);
    }
    public void setupOnErrorContext(Scope scope) {
        if (scope.contains(OnErrorStatement.class)) {
            var onerr = OnErrorContext.createCopy(scope);
            scope.setOnErrorContext(onerr);
        }
    }

    public a2geek.ghost.model.scope.Function funcDeclBegin(String name, DataType returnType, List<Symbol.Builder> params) {
        trace("compiling function '%s'", name);
        // FIXME? naming is really awkward due to naming conflicts!
        a2geek.ghost.model.scope.Function func =
            new a2geek.ghost.model.scope.Function(scope.peek(),
                Symbol.variable(name, SymbolType.RETURN_VALUE).dataType(returnType), params);
        this.scope.peek().addLocalSymbol(Symbol.scope(func));

        pushScope(func);
        pushStatementBlock(func);
        return func;
    }
    public void funcDeclEnd() {
        // TODO does this need to be validated?
        Scope func = popScope();
        popStatementBlock();
        setupOnErrorContext(func);
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
    public void dynamicGotoGosubStmt(String op, Expression target, boolean needsAddressAdjustment) {
        DynamicGotoGosubStatement dynamicGotoGosubStatement = new DynamicGotoGosubStatement(op, target, needsAddressAdjustment);
        addStatement(dynamicGotoGosubStatement);
    }

    public void returnStmt(Expression expr) {
        ReturnStatement returnStatement = new ReturnStatement(expr);
        addStatement(returnStatement);
    }

    /**
     * Allocate an array of 1 or more dimensions.
     * General layout is:
     * <pre>
     * +------------+------------+-----+--------------+------------------+
     * | dim 1 size | dim 2 size | ... | element(0,0) | element(0,1) ... |
     * +------------+------------+-----+--------------+------------------+
     * </pre>
     * A single dimension should be similar to this:
     * <pre>
     * symbol = ALLOC( (expr+2) * sizeof(datatype) )
     * POKEW symbol, expr
     * </pre>
     * Note that physical array size is the length + 1 (BASIC array is like that) + 1 (for length).
     * <pre>
     * +------+-------+-------+-----+-------+
     * | size | idx 0 | idx 1 | ... | idx N |
     * +------+-------+-------+-----+-------+
     * </pre>
     */
    public void allocateIntegerArray(Symbol symbol, List<Expression> sizes) {
        var varRef = VariableReference.with(symbol);
        var overheadBytes = new IntegerConstant(sizes.size() * DataType.INTEGER.sizeof());
        // The element count is the sum of each dimension +1 multiplied out
        var elementCount = sizes.stream().map(e -> e.plus(IntegerConstant.ONE)).reduce(Expression::times).orElseThrow();
        var bytes = overheadBytes.plus(elementCount.times(new IntegerConstant(symbol.dataType().sizeof())));
        var allocFn = callFunction(heapFunction, bytes);
        assignStmt(varRef, allocFn);
        for (int i=0; i<sizes.size(); i++) {
            pokeStmt("pokew", varRef.plus(new IntegerConstant(i * DataType.INTEGER.sizeof())), sizes.get(i));
        }
    }
    /**
     * Allocate a string array via the following code:
     * <pre>
     * symbol = ALLOC( expr + 2 )
     * POKEW symbol, expr
     * </pre>
     * Note:
     * <li>A string has room for the string + 0 terminator + max length byte.
     * <pre>
     * +------+---------------------+
     * | size | characters ... '\0' |
     * +------+---------------------+
     * </pre>
     * <li>A string defaults to 1 character if not DIMmed.
     */
    public void allocateStringArray(Symbol symbol, Expression length) {
        var varRef = VariableReference.with(symbol);
        var bytes = length.plus(IntegerConstant.TWO);
        var allocFn = callFunction(heapFunction, bytes);
        assignStmt(varRef, allocFn);
        pokeStmt("poke", varRef, length);
    }

    public void registerDimArray(Symbol symbol, Expression size) {
        arrayDims.merge(symbol, size, (oldSize, newSize) -> oldSize);
    }
    public Expression getArrayDim(Symbol symbol) {
        if (symbol.symbolType() == SymbolType.PARAMETER) {
            // Expression has only one required method to implement at this time; may need to adjust in future!
            return symbol::dataType;
        } else if (!arrayDims.containsKey(symbol)) {
            throw new RuntimeException("Array was not DIMmed: " + symbol.name());
        }
        return arrayDims.get(symbol);
    }

    public void checkArrayBounds(Symbol symbol, List<Expression> indexes, int linenum) {
        if (!boundsCheck) {
            return;
        }
        if (symbol.numDimensions() != indexes.size()) {
            var msg = String.format("[compiler bug] array bounds check; symbol='%s', indexes=%s", symbol, indexes);
            throw new RuntimeException(msg);
        }
        // IF index1 > ubound(array,1) [ OR index2 > ubound(array,2) ... ] THEN
        //    RAISE ERROR 107, "Array index out of bounds "+symbol+" at line "+linenum
        // END IF
        var errorBlock = pushStatementBlock(new StatementBlock());
        raiseError(new IntegerConstant(107),
                new StringConstant(String.format("ARRAY INDEX OUT OF BOUNDS %s AT LINE %d", symbol.name(), linenum)));
        popStatementBlock();
        var test = indexes.getFirst().gt(new ArrayLengthFunction(symbol, 1));
        for (int i=1; i<symbol.numDimensions(); i++) {
            // java indexes are 0..n while ubound indexes are 1..n+1.
            test = test.or(indexes.get(i).gt(new ArrayLengthFunction(symbol, i+1)));
        }
        IfStatement statement = new IfStatement(test, errorBlock, null, SourceType.BOUNDS_CHECK);
        addStatement(statement);
    }

    public void raiseError(Expression number, Expression message) {
        uses("err", defaultExport());
        var errNumber = findSymbol("err.number").orElseThrow();
        var errMessage = findSymbol("err.message").orElseThrow();
        assignStmt(VariableReference.with(errNumber), number);
        assignStmt(VariableReference.with(errMessage), message);
        addStatement(new RaiseErrorStatement());
    }
}
