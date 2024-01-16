package a2geek.ghost.target.ghost;

import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.*;

import java.util.*;

import static a2geek.ghost.model.Symbol.*;

public class CodeGenerationVisitor extends Visitor {
    public static final String DEFAULT_ERROR_HANDLER = "RUNTIME.DEFAULTERRORHANDLER";
    private Stack<Frame> frames = new Stack<>();
    private CodeBlock code = new CodeBlock();
    private int labelNumber;
    private Stack<Map<Symbol,Expression>> inlineVariables = new Stack<>();
    private Set<String> scopesUsed = new HashSet<>();
    private Set<String> scopesProcessed = new HashSet<>();

    public List<Instruction> getInstructions() {
        return code.getInstructions();
    }

    List<String> label(final String... names) {
        List<String> labels = new ArrayList<>();
        labelNumber++;
        for (int i=0; i<names.length; i++) {
            labels.add(String.format("_%s%d", names[i], labelNumber));
        }
        return labels;
    }

    private RuntimeException error(String fmt, Object... args) {
        return new RuntimeException(String.format(fmt, args));
    }

    @Override
    public void visit(Program program) {
        var frame = this.frames.push(Frame.create(program));
        code.emit(Opcode.LOADC, frame.localSize());
        code.emit(Opcode.PUSHZ);
        code.emit(Opcode.LOADSP);
        code.emit(Opcode.DECR);
        code.emit(Opcode.DUP);
        code.emit(Opcode.STOREGP);
        code.emit(Opcode.STORELP);
        setupOnErrContext(program.findFirst(named(DEFAULT_ERROR_HANDLER))
                .orElseThrow(() -> error("unknown label: '%s'", DEFAULT_ERROR_HANDLER)));
        setupDefaultArrayValues(program);

        program.streamAllLocalScope().filter(in(SymbolType.MODULE)).forEach(symbol -> {
            // statements in a module are initialization for the program
            dispatchToList(symbol.scope().getStatements());
        });

        dispatchAll(program);
        // Note we don't have a GLOBAL_FREE; EXIT restores stack for us
        code.emit(Opcode.EXIT);

        // Only write library methods that are actually used
        boolean wroteCode;
        do {
            wroteCode = false;
            scopesUsed.add(DEFAULT_ERROR_HANDLER);
            var detachedScopesUsed = new HashSet<>(scopesUsed);
            for (String scopeName : detachedScopesUsed) {
                if (!scopesProcessed.contains(scopeName)) {
                    program.findFirstLocalScope(named(scopeName).and(in(SymbolType.FUNCTION, SymbolType.SUBROUTINE)))
                            .map(Symbol::scope).ifPresentOrElse(this::dispatch, () -> {
                                throw new RuntimeException("unable to generate code for: " + scopeName);
                            });
                    scopesProcessed.add(scopeName);
                    wroteCode = true;
                }
            }
        } while (wroteCode);

        this.frames.pop();
    }

    public void setupDefaultArrayValues(Scope scope) {
        scope.getLocalSymbols().forEach(symbol -> {
            if (symbol.defaultValues() == null || symbol.symbolType() == SymbolType.CONSTANT) {
                return;
            }
            var dataType = symbol.dataType();
            if (dataType == DataType.INTEGER) {
                List<Integer> integerArray = symbol.defaultValues().stream()
                    .map(Expression::asInteger)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
                var label = label("INTARYCONST");
                String actual = code.emitConstant(label.get(0), integerArray);
                code.emit(Opcode.LOADA, actual);
                emitStore(symbol);
            } else if (dataType == DataType.ADDRESS) {
                List<Symbol> labels = symbol.defaultValues().stream()
                    .filter(AddressOfFunction.class::isInstance)
                    .map(AddressOfFunction.class::cast)
                    .map(AddressOfFunction::getSymbol)
                    .toList();
                var label = label("ADDRARYCONST");
                var actual = code.emitConstantLabels(label.get(0), labels);
                code.emit(Opcode.LOADA, actual);
                emitStore(symbol);
            } else {
                throw new RuntimeException("unsupported symbol array with data symbolType of " + dataType);
            }
        });
    }

    public int localFrameOffset(Symbol symbol) {
        if (this.frames.getLast().offsets().containsKey(symbol)) {
            return this.frames.getLast().offsets().get(symbol);
        }
       throw new RuntimeException("symbol not in local frame: " + symbol);
    }
    public int globalFrameOffset(Symbol symbol) {
        if (this.frames.getFirst().offsets().containsKey(symbol)) {
            return this.frames.getFirst().offsets().get(symbol);
        }
        throw new RuntimeException("symbol not in global frame: " + symbol);
    }

    public void emitArrayAddressCalc(VariableReference var) {
        // TODO assuming element size is 2 - hand optimized as well!
        // TODO assuming array size is 1
        // TODO this probably should be in the model to take advantage of optimizations!
        //      A(0) => LOAD addr, LOADC 0, INCR, SHIFTL 1, ADD  .. should be LOAD addr, LOADC 2, ADD.
        // FIXME not validating array size
        // Base Array (symbol) + Expression * sizeof(element) + 2 (array size)
        emitLoad(var.getSymbol());
        dispatch(var.getIndexes().get(0));
        code.emit(Opcode.INCR);
        code.emit(Opcode.LOADC, 1);
        code.emit(Opcode.SHIFTL);
        code.emit(Opcode.ADD);
    }

    public void emitLoad(Symbol symbol) {
        emitLoad(new VariableReference(symbol));
    }
    public void emitLoad(VariableReference var) {
        if (var.isArray()) {
            emitArrayAddressCalc(var);
            code.emit(Opcode.ILOADW);
            return;
        }
        var symbol = var.getSymbol();
        switch (symbol.symbolType()) {
            case VARIABLE, PARAMETER, RETURN_VALUE -> {
                switch (symbol.declarationType()) {
                    case LOCAL -> this.code.emit(Opcode.LOCAL_LOAD, localFrameOffset(symbol));
                    case GLOBAL -> this.code.emit(Opcode.GLOBAL_LOAD, globalFrameOffset(symbol));
                    default -> throw new RuntimeException("expecting declaration symbolType but it was: " + symbol.declarationType());
                }
            }
            case INTRINSIC -> {
                switch (symbol.name().toLowerCase()) {
                    case Intrinsic.CPU_REGISTER_A -> this.code.emit(Opcode.GETACC);
                    case Intrinsic.CPU_REGISTER_X -> this.code.emit(Opcode.GETXREG);
                    case Intrinsic.CPU_REGISTER_Y -> this.code.emit(Opcode.GETYREG);
                    default -> throw new RuntimeException("unknown intrinsic: " + symbol.name());
                }
            }
            case CONSTANT -> {
                switch (symbol.defaultValues().getFirst()) {
                    case IntegerConstant c -> visit(c);
                    case StringConstant s -> visit(s);
                    default -> throw new RuntimeException("unable to generate code for constant expression: " + symbol);
                }
            }
        }
    }
    public void emitStore(Symbol symbol) {
        emitStore(new VariableReference(symbol));
    }
    public void emitStore(VariableReference var) {
        if (var.isArray()) {
            emitArrayAddressCalc(var);
            code.emit(Opcode.ISTOREW);
            return;
        }
        var symbol = var.getSymbol();
        switch (symbol.symbolType()) {
            case VARIABLE, PARAMETER, RETURN_VALUE -> {
                switch (symbol.declarationType()) {
                    case LOCAL -> this.code.emit(Opcode.LOCAL_STORE, localFrameOffset(symbol));
                    case GLOBAL -> this.code.emit(Opcode.GLOBAL_STORE, globalFrameOffset(symbol));
                    default -> throw new RuntimeException("expecting declaration symbolType but it was: " + symbol.declarationType());
                }
            }
            case INTRINSIC -> {
                switch (symbol.name().toLowerCase()) {
                    case Intrinsic.CPU_REGISTER_A -> this.code.emit(Opcode.SETACC);
                    case Intrinsic.CPU_REGISTER_X -> this.code.emit(Opcode.SETXREG);
                    case Intrinsic.CPU_REGISTER_Y -> this.code.emit(Opcode.SETYREG);
                    default -> throw new RuntimeException("unknown intrinsic: " + symbol.name());
                }
            }
            case CONSTANT -> {
                throw new RuntimeException("cannot store to a constant value: " + symbol.name());
            }
        }
    }

    public void assignment(VariableReference var, Expression expr) {
        dispatch(expr);
        emitStore(var);
    }

    @Override
    public void visit(AssignmentStatement statement, StatementContext context) {
        assignment(statement.getVar(), statement.getExpr());
    }

    public void visit(EndStatement statement, StatementContext context) {
        code.emit(Opcode.EXIT);
    }

    @Override
    public void visit(CallStatement statement, StatementContext context) {
        dispatch(statement.getExpr());
        code.emit(Opcode.CALL);
    }

    @Override
    public void visit(IfStatement statement, StatementContext context) {
        if (!statement.hasTrueStatements() && !statement.hasFalseStatements()) {
            // Do not generate code if this is somehow an empty IF statement
            return;
        }

        var labels = label("IF_ELSE", "IF_EXIT");
        var elseLabel = labels.get(0);
        var exitLabel = labels.get(1);

        dispatch(statement.getExpression());
        if (statement.hasTrueStatements()) {
            code.emit(Opcode.IFFALSE, statement.hasFalseStatements() ? elseLabel : exitLabel);
            dispatchAll(statement.getTrueStatements());
            if (statement.hasFalseStatements()) {
                code.emit(Opcode.GOTO, exitLabel);
                code.emit(elseLabel);
                dispatchAll(statement.getFalseStatements());
            }
        }
        else {
            code.emit(Opcode.IFTRUE, exitLabel);
            dispatchAll(statement.getFalseStatements());
        }
        code.emit(exitLabel);
    }

    @Override
    public void visit(PokeStatement statement, StatementContext context) {
        dispatch(statement.getB());
        dispatch(statement.getA());
        switch (statement.getOp().toLowerCase()) {
            case "poke" -> code.emit(Opcode.ISTOREB);
            case "pokew" -> code.emit(Opcode.ISTOREW);
            default -> throw new RuntimeException("unknown poke op: " + statement.getOp());
        }
    }

    public void visit(LabelStatement statement, StatementContext context) {
        code.emit(statement.getLabel().name());
    }

    @Override
    public void visit(GotoGosubStatement statement, StatementContext context) {
        code.emit(Opcode.valueOf(statement.getOp().toUpperCase()), statement.getLabel().name());
    }

    @Override
    public void visit(DynamicGotoGosubStatement statement, StatementContext context) {
        var labels = label("RETURN");
        var returnLabel = labels.getFirst();
        if ("gosub".equalsIgnoreCase(statement.getOp())) {
            // GOSUB needs a return address on the stack
            code.emit(Opcode.LOADA, returnLabel);
            code.emit(Opcode.DECR);
        }
        dispatch(statement.getTarget());
        if (statement.needsAddressAdjustment()) {
            code.emit(Opcode.FIXA);
        }
        code.emit(Opcode.RETURN);
        code.emit(returnLabel);
    }

    @Override
    public void visit(OnErrorStatement statement, StatementContext context) {
        switch (statement.getOp()) {
            case GOTO -> setupOnErrContext(statement.getLabel());
            case DISABLE -> setupOnErrContext(this.frames.getFirst().scope().findFirst(named(DEFAULT_ERROR_HANDLER)).orElseThrow());
            // TODO
            case RESUME_NEXT -> {}  // do nothing
        }
    }

    public void setupOnErrContext(Symbol label) {
        var onerr = this.frames.getFirst().scope().getOnErrorContext();
        code.emit(Opcode.LOADSP);
        emitStore(onerr.stackPointer());
        code.emit(Opcode.LOADLP);
        emitStore(onerr.framePointer());
        code.emit(Opcode.LOADA, label.name());
        code.emit(Opcode.DECR);     // need to setup for a RETURN
        emitStore(onerr.gotoAddress());

    }

    @Override
    public void visit(RaiseErrorStatement statement, StatementContext context) {
        var onerr = this.frames.getFirst().scope().getOnErrorContext();
        emitLoad(onerr.stackPointer());
        code.emit(Opcode.STORESP);
        emitLoad(onerr.framePointer());
        code.emit(Opcode.STORELP);
        // push address and return to it
        emitLoad(onerr.gotoAddress());
        code.emit(Opcode.RETURN);
    }

    @Override
    public void visit(PopStatement statement, StatementContext context) {
        // This is an address; expect that 2 is always correct!
        code.emit(Opcode.POPN, 2);
    }

    @Override
    public void visit(ReturnStatement statement, StatementContext context) {
        boolean hasReturnValue = statement.getExpr() != null;
        if (this.frames.peek().scope() instanceof Function f) {
            var refs = this.frames.peek().scope().findAllLocalScope(in(SymbolType.RETURN_VALUE));
            if (refs.size() == 1 && hasReturnValue) {
                assignment(new VariableReference(refs.getFirst()), statement.getExpr());
            } else if (refs.size() != 0 || hasReturnValue) {
                throw new RuntimeException("function return mismatch");
            }
            code.emit(Opcode.GOTO, f.getExitLabel());
        }
        else if (hasReturnValue) {
            throw new RuntimeException("cannot return value from a subroutine");
        }
        else if (this.frames.peek().scope() instanceof Subroutine s) {
            code.emit(Opcode.GOTO, s.getExitLabel());
        }
        else {
            code.emit(Opcode.RETURN);
        }
    }

    @Override
    public void visit(CallSubroutine statement, StatementContext context) {
        // Using the "program" frame
        var subFullName = statement.getSubroutine().getFullPathName();
        var scope = frames.getFirst().scope()
                .findFirstLocalScope(named(subFullName).and(in(SymbolType.SUBROUTINE)))
                .map(Symbol::scope)
                .orElseThrow(() -> new RuntimeException(subFullName + " not found"));
        if (scope instanceof Subroutine sub) {
            Map<Symbol,Expression> map = new HashMap<>();
            inlineVariables.push(map);
            if (sub.is(Subroutine.Modifier.INLINE)) {
                var parameters = sub.findAllLocalScope(in(SymbolType.PARAMETER));
                if (parameters.size() != statement.getParameters().size()) {
                    throw new RuntimeException(String.format("parameter size mismatch for call to '%s'", subFullName));
                }
                // Subroutine parameters are REVERSED (for stack placement), so taking that into account:
                parameters = parameters.reversed();
                for (int i=0; i<parameters.size(); i++) {
                    var param = parameters.get(i);
                    var value = statement.getParameters().get(i);
                    if (param.numDimensions() > 0) {
                        throw new RuntimeException("parameter inlining not available for arrays: " + param.name());
                    }
                    map.put(param, value);
                }
                dispatchAll(sub);
            }
            else {
                scopesUsed.add(sub.getFullPathName());
                boolean hasParameters = statement.getParameters().size() != 0;
                for (Expression expr : statement.getParameters()) {
                    dispatch(expr);
                }
                code.emit(Opcode.GOSUB, sub.getFullPathName());
                if (hasParameters) {
                    // FIXME when we have more datatypes this will be incorrect
                    code.emit(Opcode.POPN, statement.getParameters().size() * 2);
                }
            }
            inlineVariables.pop();
            return;
        }
        throw new RuntimeException(String.format("calling a subroutine but '%s' is not a subroutine?", subFullName));
    }

    public void saveOnErrContext(Scope scope) {
        var local = scope.getOnErrorContext();
        if (local == null) {
            return;
        }
        var global = this.frames.getFirst().scope().getOnErrorContext();
        emitLoad(global.stackPointer());
        emitStore(local.stackPointer());
        emitLoad(global.framePointer());
        emitStore(local.framePointer());
        emitLoad(global.gotoAddress());
        emitStore(local.gotoAddress());
    }

    public void restoreOnErrContext(Scope scope) {
        var local = scope.getOnErrorContext();
        if (local == null) {
            return;
        }
        var global = this.frames.getFirst().scope().getOnErrorContext();
        emitLoad(local.stackPointer());
        emitStore(global.stackPointer());
        emitLoad(local.framePointer());
        emitStore(global.framePointer());
        emitLoad(local.gotoAddress());
        emitStore(global.gotoAddress());
    }

    @Override
    public void visit(Subroutine subroutine) {
        if (subroutine.is(Subroutine.Modifier.INLINE)) {
            // We are already inlining this, so do not generate code.
            return;
        }
        var exitLabel = label("SUBEXIT").getFirst();
        subroutine.setExitLabel(exitLabel);
        var hasLocalScope = subroutine.findAllLocalScope(is(DeclarationType.LOCAL).and(in(SymbolType.VARIABLE, SymbolType.PARAMETER))).size() != 0;
        var frame = frames.push(Frame.create(subroutine));
        code.emit(subroutine.getFullPathName());
        if (hasLocalScope) setupLocalFrame(frame);
        saveOnErrContext(subroutine);
        setupDefaultArrayValues(subroutine);
        if (subroutine.getStatements() != null) {
            dispatchAll(subroutine);
        }
        code.emit(exitLabel);
        if (hasLocalScope) tearDownLocalFrame(frame);
        restoreOnErrContext(subroutine);
        code.emit(Opcode.RETURN);
        frames.pop();
    }

    public void setupLocalFrame(Frame frame) {
        code.emit(Opcode.LOADLP);
        code.emit(Opcode.LOADC, frame.localSize());
        code.emit(Opcode.PUSHZ);
        code.emit(Opcode.LOADSP);
        code.emit(Opcode.DECR);
        code.emit(Opcode.STORELP);
    }
    public void tearDownLocalFrame(Frame frame) {
        code.emit(Opcode.POPN, frame.localSize());
        code.emit(Opcode.STORELP);
    }

    @Override
    public void visit(Function function) {
        // NOTE/WARNING: Function cannot have the optimization to drop LOCAL_RESERVE/LOCAL_FREE
        //               because the RETURN_VALUE is always present and relative to function entry.
        var frame = frames.push(Frame.create(function));
        var labels = label("FUNCXIT");
        var exitLabel = labels.getFirst();
        function.setExitLabel(exitLabel);
        code.emit(function.getFullPathName());
        setupLocalFrame(frame);
        saveOnErrContext(function);
        setupDefaultArrayValues(function);
        if (function.getStatements() != null) {
            dispatchAll(function);
        }
        code.emit(exitLabel);
        tearDownLocalFrame(frame);
        restoreOnErrContext(function);
        code.emit(Opcode.RETURN);
        frames.pop();
    }

    public boolean emitBinaryAddOptimizations(Expression left, Expression right) {
        // left + 1 => left++
        if (Objects.equals(left, IntegerConstant.ONE)) {
            dispatch(right);
            code.emit(Opcode.INCR);
            return true;
        }
        // 1 + right => right++
        else if (Objects.equals(right, IntegerConstant.ONE)) {
            dispatch(left);
            code.emit(Opcode.INCR);
            return true;
        }
        // left + -1 => left--
        else if (Objects.equals(left, IntegerConstant.NEGATIVE_ONE)) {
            dispatch(right);
            code.emit(Opcode.DECR);
            return true;
        }
        // -1 + right => right--
        else if (Objects.equals(right, IntegerConstant.NEGATIVE_ONE)) {
            dispatch(left);
            code.emit(Opcode.DECR);
            return true;
        }
        return false;
    }

    public boolean emitBinarySubOptimizations(Expression left, Expression right) {
        // left - -1 => left++
        if (Objects.equals(right, IntegerConstant.NEGATIVE_ONE)) {
            dispatch(left);
            code.emit(Opcode.INCR);
            return true;
        }
        // left - 1 => left--
        else if (Objects.equals(right, IntegerConstant.ONE)) {
            dispatch(left);
            code.emit(Opcode.DECR);
            return true;
        }
        return false;
    }

    public Expression visit(BinaryExpression expression) {
        boolean optimized = switch (expression.getOp()) {
            case "+" -> emitBinaryAddOptimizations(expression.getL(), expression.getR());
            case "-" -> emitBinarySubOptimizations(expression.getL(), expression.getR());
            default -> false;
        };
        if (optimized) return null;

        final Set<String> needsSwap = Set.of(">", ">=");
        if (expression.getL().equals(expression.getR())) {
            // Minor optimization when both args are identical
            dispatch(expression.getL());
            code.emit(Opcode.DUP);
        }
        else if (needsSwap.contains(expression.getOp())) {
            dispatch(expression.getR());
            dispatch(expression.getL());
        } 
        else {
            dispatch(expression.getL());
            dispatch(expression.getR());
        }

        switch (expression.getOp()) {
            case "+" -> code.emit(Opcode.ADD);
            case "-" -> code.emit(Opcode.SUB);
            case "*" -> code.emit(Opcode.MUL);
            case "/" -> code.emit(Opcode.DIVS);
            case "mod" -> code.emit(Opcode.MODS);
            case "<", ">" -> code.emit(Opcode.LT);      // We swapped arguments for ">" to reuse "LT"
            case "<=", ">=" -> code.emit(Opcode.LE);    // We swapped arguments for ">=" to reuse "LE"
            case "=" -> code.emit(Opcode.EQ);
            case "<>" -> code.emit(Opcode.NE);
            case "or" -> code.emit(Opcode.OR);
            case "and" -> code.emit(Opcode.AND);
            case "xor" -> code.emit(Opcode.XOR);
            case "<<" -> code.emit(Opcode.SHIFTL);
            case ">>" -> code.emit(Opcode.SHIFTR);
            default -> throw new RuntimeException("Operation not supported: " + expression.getOp());
        }
        return null;
    }

    public Expression visit(VariableReference expression) {
        if (!inlineVariables.isEmpty() && inlineVariables.peek().containsKey(expression.getSymbol())) {
            Expression expr = inlineVariables.peek().get(expression.getSymbol());
            inlineVariables.push(Collections.emptyMap());   // We are outside the mapped context for this evaluation
            dispatch(expr);
            inlineVariables.pop();
        }
        else {
            emitLoad(expression);
        }
        return null;
    }

    public Expression visit(IntegerConstant expression) {
        code.emit(Opcode.LOADC, expression.getValue());
        return null;
    }

    @Override
    public Expression visit(BooleanConstant expression) {
        code.emit(Opcode.LOADC, expression.getValue() ? 1 : 0);
        return null;
    }

    @Override
    public Expression visit(StringConstant expression) {
        var label = label("STRCONST");
        String actual = code.emitConstant(label.getFirst(), expression.getValue());
        code.emit(Opcode.LOADA, actual);
        return null;
    }

    @Override
    public Expression visit(FunctionExpression function) {
        Runnable emitParameters = () -> {
            for (Expression expr : function.getParameters()) {
                dispatch(expr);
            }
        };
        switch (function.getName().toLowerCase()) {
            case "peek" -> {
                emitParameters.run();
                code.emit(Opcode.ILOADB);
            }
            case "peekw" -> {
                emitParameters.run();
                code.emit(Opcode.ILOADW);
            }
            case "alloc" -> {
                emitParameters.run();
                code.emit(Opcode.PUSHZ);
                code.emit(Opcode.LOADSP);
            }
            default -> {
                if (function.getFunction() == null) {
                    throw new RuntimeException("unimplemented standard function: " + function.getName());
                }
                var func = function.getFunction();
                scopesUsed.add(func.getFullPathName());
                // FIXME need to reserve by return type when types are in place!
                code.emit(Opcode.LOADC, 0);
                emitParameters.run();
                code.emit(Opcode.GOSUB, func.getFullPathName());
                var hasParameters = function.getParameters().size() != 0;
                if (hasParameters) {
                    // FIXME when we have more datatypes this will be incorrect
                    code.emit(Opcode.POPN, function.getParameters().size() * 2);
                }
            }
        }
        return null;
    }
    @Override
    public Expression visit(UnaryExpression expression) {
        if ("-".equals(expression.getOp())) {
            dispatch(expression.getExpr());
            code.emit(Opcode.NEG);
        }
        else if ("not".equalsIgnoreCase(expression.getOp())) {
            dispatch(expression.getExpr());
            switch (expression.getType()) {
                case INTEGER -> code.emit(Opcode.LOADC, 0xffff);
                case BOOLEAN -> code.emit(Opcode.LOADC, 1);
                default -> throw new RuntimeException("cannot only perform a NOT on a Boolean or Integer: " + expression);
            }
            code.emit(Opcode.XOR);
        }
        else {
            throw new RuntimeException("unknown unary operator: " + expression.getOp());
        }
        return null;
    }

    @Override
    public Expression visit(ArrayLengthFunction expression) {
        emitLoad(expression.getSymbol());
        code.emit(Opcode.ILOADW);
        return null;
    }

    @Override
    public Expression visit(AddressOfFunction expression) {
        code.emit(Opcode.LOADA, expression.getSymbol().name());
        return null;
    }
}
