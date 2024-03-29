package a2geek.ghost.target.ghost;

import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.*;

import java.util.*;

import static a2geek.ghost.model.Symbol.*;

public class CodeGenerationVisitor extends DispatchVisitor {
    public static final String DEFAULT_ERROR_HANDLER = "RUNTIME.DEFAULTERRORHANDLER";
    private final Stack<Frame> frames = new Stack<>();
    private final CodeBlock code = new CodeBlock();
    private int labelNumber;
    private final Set<String> scopesUsed = new HashSet<>();
    private final Set<String> scopesProcessed = new HashSet<>();

    public List<Instruction> getInstructions() {
        return code.getInstructions();
    }

    List<String> label(final String... names) {
        List<String> labels = new ArrayList<>();
        labelNumber++;
        for (String name : names) {
            labels.add(String.format("_%s%d", name, labelNumber));
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
        setupDefaultStringValues(program);

        dispatchAll(program, program);
        // Note we don't have a GLOBAL_FREE; EXIT restores stack for us
        if (!program.isLastStatement(EndStatement.class)) {
            code.emit(Opcode.EXIT);
        }

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
            if (symbol.defaultValues() == null || symbol.numDimensions() == 0
                    || symbol.symbolType() == SymbolType.CONSTANT) {
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
                String actual = code.emitConstant(label.getFirst(), integerArray);
                code.emit(Opcode.LOADA, actual);
                emitStore(symbol);
            } else if (dataType == DataType.ADDRESS) {
                List<Symbol> labels = symbol.defaultValues().stream()
                    .filter(AddressOfOperator.class::isInstance)
                    .map(AddressOfOperator.class::cast)
                    .map(AddressOfOperator::getSymbol)
                    .toList();
                var label = label("ADDRARYCONST");
                var actual = code.emitConstantLabels(label.getFirst(), labels);
                code.emit(Opcode.LOADA, actual);
                emitStore(symbol);
            } else {
                throw new RuntimeException("unsupported symbol array with data symbolType of " + dataType);
            }
        });
    }

    public void setupDefaultStringValues(Scope scope) {
        scope.getLocalSymbols().forEach(symbol -> {
            if (symbol.dataType() != DataType.STRING || symbol.defaultValues() == null || symbol.symbolType() != SymbolType.VARIABLE) {
                return;
            }
            if (symbol.defaultValues().getFirst() instanceof StringConstant constant) {
                visit(constant);
                emitStore(symbol);
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

    public void emitLoad(Symbol symbol) {
        switch (symbol.symbolType()) {
            case VARIABLE, PARAMETER, RETURN_VALUE -> {
                switch (symbol.declarationType()) {
                    case LOCAL -> this.code.emit(Opcode.LOCAL_LOAD, localFrameOffset(symbol));
                    case GLOBAL -> this.code.emit(Opcode.GLOBAL_LOAD, globalFrameOffset(symbol));
                    case INTRINSIC -> {
                        switch (symbol.name().toLowerCase()) {
                            case Intrinsic.CPU_REGISTER_A -> this.code.emit(Opcode.GETACC);
                            case Intrinsic.CPU_REGISTER_X -> this.code.emit(Opcode.GETXREG);
                            case Intrinsic.CPU_REGISTER_Y -> this.code.emit(Opcode.GETYREG);
                            default -> throw new RuntimeException("unknown intrinsic: " + symbol.name());
                        }
                    }
                    default -> throw new RuntimeException("expecting declaration symbolType but it was: " + symbol.declarationType());
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
        switch (symbol.symbolType()) {
            case VARIABLE, PARAMETER, RETURN_VALUE -> {
                switch (symbol.declarationType()) {
                    case LOCAL -> this.code.emit(Opcode.LOCAL_STORE, localFrameOffset(symbol));
                    case GLOBAL -> this.code.emit(Opcode.GLOBAL_STORE, globalFrameOffset(symbol));
                    case INTRINSIC -> {
                        switch (symbol.name().toLowerCase()) {
                            case Intrinsic.CPU_REGISTER_A -> this.code.emit(Opcode.SETACC);
                            case Intrinsic.CPU_REGISTER_X -> this.code.emit(Opcode.SETXREG);
                            case Intrinsic.CPU_REGISTER_Y -> this.code.emit(Opcode.SETYREG);
                            default -> throw new RuntimeException("unknown intrinsic: " + symbol.name());
                        }
                    }
                    default -> throw new RuntimeException("expecting declaration symbolType but it was: " + symbol.declarationType());
                }
            }
            case CONSTANT -> {
                throw new RuntimeException("cannot store to a constant value: " + symbol.name());
            }
        }
    }

    @Override
    public void visit(AssignmentStatement statement, VisitorContext context) {
        dispatch(statement.getValue());
        if (statement.getVar() instanceof VariableReference ref) {
            // A = <expr> (simple assignment)
            emitStore(ref.getSymbol());
        }
        else if (statement.getVar() instanceof DereferenceOperator deref) {
            // *(<expr1>) = <expr2>   (dereferenced assignment)
            dispatch(deref.getExpr());
            switch (statement.getValue().getType()) {
                case BYTE -> code.emit(Opcode.ISTOREB);
                case INTEGER, ADDRESS, STRING, BOOLEAN -> code.emit(Opcode.ISTOREW);
                default -> throw new RuntimeException("unexpected type for dereferenced assignment: " + deref.getType());
            }
        }
        else {
            throw error("not valid assignment: %s", statement);
        }
    }

    @Override
    public void visit(EndStatement statement, VisitorContext context) {
        code.emit(Opcode.EXIT);
    }

    @Override
    public void visit(CallStatement statement, VisitorContext context) {
        dispatch(statement.getExpr());
        code.emit(Opcode.CALL);
    }

    @Override
    public void visit(IfStatement statement, VisitorContext context) {
        if (!statement.hasTrueStatements() && !statement.hasFalseStatements()) {
            // Do not generate code if this is somehow an empty IF statement
            return;
        }

        var labels = label("IF_ELSE", "IF_EXIT");
        var elseLabel = labels.get(0);
        var exitLabel = labels.get(1);

        // Handle short-circuit forms: IF A = 0  -or- IF A <> 0.  We don't need to test zero!
        var trueOp = Opcode.IFZ;
        var falseOp = Opcode.IFNZ;
        var expr = statement.getExpression();
        if (expr instanceof BinaryExpression bin) {
            Expression keep = null;
            if (IntegerConstant.ZERO.equals(bin.getL())) {
                keep = bin.getR();
            }
            else if (IntegerConstant.ZERO.equals(bin.getR())) {
                keep = bin.getL();
            }
            if (keep != null && "=".equals(bin.getOp())) {
                expr = keep;
                trueOp = Opcode.IFNZ;
                falseOp = Opcode.IFZ;
            }
            else if (keep != null && "<>".equals(bin.getOp())) {
                expr = keep;
            }
        }

        dispatch(expr);
        if (statement.hasTrueStatements()) {
            code.emit(trueOp, statement.hasFalseStatements() ? elseLabel : exitLabel);
            dispatchAll(context, statement.getTrueStatements());
            if (statement.hasFalseStatements()) {
                code.emit(Opcode.GOTO, exitLabel);
                code.emit(elseLabel);
                dispatchAll(context, statement.getFalseStatements());
            }
        }
        else {
            code.emit(falseOp, exitLabel);
            dispatchAll(context, statement.getFalseStatements());
        }
        code.emit(exitLabel);
    }

    @Override
    public void visit(LabelStatement statement, VisitorContext context) {
        code.emit(statement.getLabel().name());
    }

    @Override
    public void visit(GotoGosubStatement statement, VisitorContext context) {
        code.emit(Opcode.valueOf(statement.getOp().toUpperCase()), statement.getLabel().name());
    }

    @Override
    public void visit(DynamicGotoGosubStatement statement, VisitorContext context) {
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
    public void visit(OnErrorStatement statement, VisitorContext context) {
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
    public void visit(RaiseErrorStatement statement, VisitorContext context) {
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
    public void visit(PopStatement statement, VisitorContext context) {
        // This is an address; expect that 2 is always correct!
        code.emit(Opcode.POPN, 2);
    }

    @Override
    public void visit(ReturnStatement statement, VisitorContext context) {
        boolean hasReturnValue = statement.getExpr() != null;
        if (this.frames.peek().scope() instanceof Function f) {
            var refs = this.frames.peek().scope().findAllLocalScope(in(SymbolType.RETURN_VALUE));
            if (refs.size() == 1 && hasReturnValue) {
                dispatch(statement.getExpr());
                emitStore(refs.getFirst());
            } else if (!refs.isEmpty() || hasReturnValue) {
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
    public void visit(CallSubroutine statement, VisitorContext context) {
        // Using the "program" frame
        var subFullName = statement.getSubroutine().getFullPathName();
        if ("dealloc".equalsIgnoreCase(subFullName)) {
            // We assume we have 1 parameter and try to optimize operation based on being a constant or not...
            var param = statement.getParameters().getFirst();
            if (param.asInteger().isPresent()) {
                code.emit(Opcode.POPN, param.asInteger().get());
            }
            else {
                dispatch(param);
                code.emit(Opcode.POP);
            }
            return;
        }
        var scope = frames.getFirst().scope()
                .findFirstLocalScope(named(subFullName).and(in(SymbolType.SUBROUTINE)))
                .map(Symbol::scope)
                .orElseThrow(() -> new RuntimeException(subFullName + " not found"));
        if (scope instanceof Subroutine sub) {
            scopesUsed.add(sub.getFullPathName());
            for (Expression expr : statement.getParameters()) {
                dispatch(expr);
            }
            code.emit(Opcode.GOSUB, sub.getFullPathName());
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
        var exitLabel = label("SUBEXIT").getFirst();
        subroutine.setExitLabel(exitLabel);
        var hasLocalScope = !subroutine.findAllLocalScope(is(DeclarationType.LOCAL).and(in(SymbolType.VARIABLE, SymbolType.PARAMETER))).isEmpty();
        var frame = frames.push(Frame.create(subroutine));
        code.emit(subroutine.getFullPathName());
        if (hasLocalScope) setupLocalFrame(frame);
        saveOnErrContext(subroutine);
        setupDefaultArrayValues(subroutine);
        setupDefaultStringValues(subroutine);
        if (subroutine.getStatements() != null) {
            dispatchAll(subroutine, subroutine);
        }
        code.emit(exitLabel);
        if (hasLocalScope) tearDownLocalFrame(frame);
        restoreOnErrContext(subroutine);
        code.emit(Opcode.RETURNN, frame.parameterSize());
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
        setupDefaultStringValues(function);
        if (function.getStatements() != null) {
            dispatchAll(function, function);
        }
        code.emit(exitLabel);
        tearDownLocalFrame(frame);
        restoreOnErrContext(function);
        code.emit(Opcode.RETURNN, frame.parameterSize());
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

    @Override
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

    @Override
    public Expression visit(VariableReference expression) {
        emitLoad(expression.getSymbol());
        return null;
    }

    @Override
    public Expression visit(IntegerConstant expression) {
        code.emit(Opcode.LOADC, expression.getValue());
        return null;
    }

    @Override
    public Expression visit(ByteConstant expression) {
        // TODO ... we just treat a BYTE as a WORD value
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
                code.emit(Opcode.LOADC, func.getDataType().sizeof());
                code.emit(Opcode.PUSHZ);
                emitParameters.run();
                code.emit(Opcode.GOSUB, func.getFullPathName());
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
    public Expression visit(DereferenceOperator expression) {
        // *(<expr>)   (dereferenced load)
        dispatch(expression.getExpr());
        switch (expression.getType()) {
            case BYTE -> code.emit(Opcode.ILOADB);
            case INTEGER, ADDRESS, STRING, BOOLEAN -> code.emit(Opcode.ILOADW);
            default -> throw new RuntimeException("unexpected type for dereferenced load: " + expression.getType());
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
    public Expression visit(AddressOfOperator expression) {
        var symbol = expression.getSymbol();
        switch (symbol.symbolType()) {
            case VARIABLE, PARAMETER -> {
                switch (symbol.declarationType()) {
                    case GLOBAL -> {
                        code.emit(Opcode.LOADGP);
                        code.emit(Opcode.INCR);
                        code.emit(Opcode.LOADC, globalFrameOffset(symbol));
                        code.emit(Opcode.ADD);
                    }
                    case LOCAL -> {
                        code.emit(Opcode.LOADLP);
                        code.emit(Opcode.INCR);
                        code.emit(Opcode.LOADC, localFrameOffset(symbol));
                        code.emit(Opcode.ADD);
                    }
                    case INTRINSIC -> throw error("cannot take address of an intrinsic value: '%s'", expression);
                };
            }
            case LABEL -> code.emit(Opcode.LOADA, symbol.name());
            default -> throw error("unexpected addressof expression: '%s'", expression);
        }
        return null;
    }

    @Override
    public Expression visit(TypeConversionOperator expression) {
        // TODO we don't really have "real" type conversions yet
        dispatch(expression.getExpr());
        return null;
    }

    @Override
    public Expression visit(PlaceholderExpression expression) {
        return null;
    }
}
