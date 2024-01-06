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

import static a2geek.ghost.model.Symbol.in;
import static a2geek.ghost.model.Symbol.named;

/**
 * A shared component to help building the BASIC model between language variants.
 */
public class ModelBuilder {
    public static final String LORES_LIBRARY = "lores";
    public static final String MEMORY_LIBRARY = "memory";
    public static final String MISC_LIBRARY = "misc";
    public static final String MATH_LIBRARY = "math";
    public static final String RUNTIME_LIBRARY = "runtime";
    public static final String STRINGS_LIBRARY = "strings";
    public static final String TEXT_LIBRARY = "text";

    private Function<String,String> caseStrategy;
    private Function<String,String> arrayNameStrategy = s -> s;
    private Function<String,String> controlCharsFn = s -> s;
    private Stack<Scope> scope = new Stack<>();
    private Stack<StatementBlock> statementBlock = new Stack<>();
    private Set<String> librariesIncluded = new HashSet<>();
    private boolean trace = false;
    private boolean boundsCheck = true;
    private boolean includeLibraries = true;
    private String heapFunction;
    /** Track array dimensions */
    private Map<Symbol, Expression> arrayDims = new HashMap<>();
    /** Tracking a distinct label number globally (regardless of scope) to prevent name collisions. */
    private static int labelNumber;

    public ModelBuilder(Function<String,String> caseStrategy) {
        commonInit(caseStrategy);

        // --- PRE-LOAD ALL LIBRARIES ---
        // No dependencies
        uses(MATH_LIBRARY);
        uses(RUNTIME_LIBRARY);  // has an implicit dependency on math
        uses(LORES_LIBRARY);
        uses(MEMORY_LIBRARY);
        uses(MISC_LIBRARY);
        uses(STRINGS_LIBRARY);
        uses(TEXT_LIBRARY);
    }

    private void commonInit(Function<String,String> caseStrategy) {
        this.caseStrategy = caseStrategy;
        useStackForHeap();  // default
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

    public void useStackForHeap() {
        this.heapFunction = "alloc";
    }
    public void useMemoryForHeap(int startAddress) {
        this.heapFunction = "heapalloc";
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
    public Symbol addArrayVariable(String name, DataType dataType, int numDimensions) {
        return this.scope.peek().addLocalSymbol(
                Symbol.variable(fixArrayName(name), SymbolType.VARIABLE)
                      .dataType(dataType)
                      .dimensions(numDimensions));
    }
    public Symbol addArrayDefaultVariable(String name, DataType dataType, int numDimensions,
                                          List<Expression> defaultValues) {
        return this.scope.peek().addLocalSymbol(
            Symbol.variable(fixArrayName(name), SymbolType.VARIABLE)
                .dataType(dataType)
                .dimensions(numDimensions)
                .defaultValues(defaultValues));
    }

    public Symbol addConstant(String name, Expression value) {
        return this.scope.peek().addLocalSymbol(Symbol.constant(name, value));
    }
    public Symbol addTempVariable(DataType dataType) {
        labelNumber+= 1;    // just reusing the counter
        var name = String.format("_temp%d", labelNumber);
        return this.scope.peek().addLocalSymbol(
                Symbol.variable(name, SymbolType.VARIABLE)
                      .dataType(dataType));
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

    public void trace(String fmt, Object... args) {
        if (trace) {
            System.out.printf(fmt, args);
            System.out.println();
        }
    }

    public void uses(String libname) {
        if (librariesIncluded.contains(libname)) {
            return;
        }
        trace("loading library: %s", libname);
        librariesIncluded.add(libname);
        String name = String.format("/library/%s.bas", libname);
        try (InputStream inputStream = getClass().getResourceAsStream(name)) {
            if (inputStream == null) {
                throw new RuntimeException("unknown library: " + libname);
            }
            ParseUtil.basicToModel(CharStreams.fromStream(inputStream), this);
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
        var id = fixCase(name);
        return FunctionExpression.isLibraryFunction(id)
            || FunctionExpression.isIntrinsicFunction(id)
            || scope.peek().findFirst(named(id).and(in(SymbolType.FUNCTION))).isPresent();
    }

    public FunctionExpression callFunction(String name, List<Expression> params) {
        var id = fixCase(name);
        if (FunctionExpression.isLibraryFunction(id)) {
            FunctionExpression.Descriptor descriptor = FunctionExpression.findDescriptor(id, params).orElseThrow();
            uses(descriptor.library());
            id = fixCase(descriptor.fullName());
        }

        Optional<Scope> scope = this.scope.peek().findFirst(named(id).and(in(SymbolType.FUNCTION)))
                .map(Symbol::scope);
        if (scope.isPresent()) {
            if (scope.get() instanceof a2geek.ghost.model.scope.Function fn) {
                checkCallParameters(fn, params);
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

    public Scope moduleDeclBegin(String name) {
        Scope module = new Scope(scope.peek(), fixCase(name));
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
        Subroutine sub = new Subroutine(scope.peek(), fixCase(name), params);
        this.scope.peek().addLocalSymbol(Symbol.scope(sub));

        pushScope(sub);
        pushStatementBlock(sub);
        return sub;
    }
    public void subDeclEnd() {
        // TODO does this need to be validated?
        popScope();
        popStatementBlock();
    }

    public a2geek.ghost.model.scope.Function funcDeclBegin(String name, DataType returnType, List<Symbol.Builder> params) {
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
    public void dynamicGotoGosubStmt(String op, Expression target, boolean needsAddressAdjustment) {
        DynamicGotoGosubStatement dynamicGotoGosubStatement = new DynamicGotoGosubStatement(op, target, needsAddressAdjustment);
        addStatement(dynamicGotoGosubStatement);
    }

    public void returnStmt(Expression expr) {
        ReturnStatement returnStatement = new ReturnStatement(expr);
        addStatement(returnStatement);
    }

    /**
     * Allocate an integer array via the following code:
     * <pre>
     * symbol = ALLOC( (expr+2) * 2 )
     * POKEW symbol, expr
     * </pre>
     * Note that physical array size is the length + 1 (BASIC array is like that) + 1 (for length).
     * <pre>
     * +------+-------+-------+-----+-------+
     * | size | idx 0 | idx 1 | ... | idx N |
     * +------+-------+-------+-----+-------+
     * </pre>
     */
    public void allocateIntegerArray(Symbol symbol, Expression size) {
        var varRef = VariableReference.with(symbol);
        var bytes = new BinaryExpression(
                new BinaryExpression(size, IntegerConstant.TWO, "+"),
                IntegerConstant.TWO, "*");
        var allocFn = callFunction(heapFunction, Arrays.asList(bytes));
        assignStmt(varRef, allocFn);
        pokeStmt("pokew", varRef, size);
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
        var bytes = new BinaryExpression(length, IntegerConstant.TWO, "+");
        var allocFn = callFunction(heapFunction, Arrays.asList(bytes));
        assignStmt(varRef, allocFn);
        pokeStmt("poke", varRef, length);
    }

    public void registerDimArray(Symbol symbol, Expression size) {
        arrayDims.merge(symbol, size, (oldSize, newSize) -> oldSize == null ? newSize : oldSize);
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
