package a2geek.ghost.target.ghost;

import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.*;

import java.util.*;

public class CodeGenerationVisitor extends Visitor {
    private Stack<Frame> frames = new Stack<>();
    private CodeBlock code = new CodeBlock();
    private int labelNumber;

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

    @Override
    public void visit(Program program) {
        var frame = this.frames.push(Frame.create(program));
        code.emit(Opcode.GLOBAL_RESERVE, frame.localSize());
        super.visit(program);
        // Note we don't have a GLOBAL_FREE; EXIT restores stack for us
        code.emit(Opcode.EXIT);
        this.frames.pop();
    }

    public int frameOffset(Symbol symbol) {
        if (this.frames.peek().offsets().containsKey(symbol)) {
            return this.frames.peek().offsets().get(symbol);
        }
       throw new RuntimeException("symbol not in frame: " + symbol);
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
        switch (symbol.type()) {
            case LOCAL, PARAMETER, RETURN_VALUE -> {
                this.code.emit(Opcode.LOCAL_LOAD, frameOffset(symbol));
            }
            case GLOBAL -> {
                this.code.emit(Opcode.GLOBAL_LOAD, frameOffset(symbol));
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
                if (symbol.expr() instanceof IntegerConstant c) {
                    visit(c);
                }
                else if (symbol.expr() instanceof StringConstant s) {
                    visit(s);
                }
                else {
                    throw new RuntimeException("unable to generate code for constant expression: " + symbol);
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
        }        var symbol = var.getSymbol();
        switch (symbol.type()) {
            case LOCAL, PARAMETER, RETURN_VALUE -> {
                this.code.emit(Opcode.LOCAL_STORE, frameOffset(symbol));
            }
            case GLOBAL -> {
                this.code.emit(Opcode.GLOBAL_STORE, frameOffset(symbol));
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
                throw new RuntimeException("cannot store to a constant value");
            }
        }
    }

    @Override
    public void visit(DimStatement statement, StatementContext context) {
        // TODO assuming element size of 2
        // TODO assuming array size of 1
        // Reserve N bytes on stack = (Size of Array+1) * (Element Size) + (Size of dimension) * Dimensions
        //  for now N = (array size+1) * 2 + 2 * 1 => (array size + 2) * 2 => (array size + 2) << 1
        dispatch(statement.getExpr());
        code.emit(Opcode.INCR); // INCR, INCR = TOS+2
        code.emit(Opcode.INCR);
        code.emit(Opcode.LOADC, 1);
        code.emit(Opcode.SHIFTL);
        code.emit(Opcode.PUSHZ);
        // TODO ^^ ARRAY ALLOCATED TO STACK FOR NOW! This should be configurable?
        code.emit(Opcode.LOADSP);
        emitStore(statement.getSymbol());
        // Need to set array size for dimension 0
        dispatch(statement.getExpr());
        emitLoad(statement.getSymbol());
        code.emit(Opcode.ISTOREW);
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
        var labels = label("IFF", "IFX");
        dispatch(statement.getExpression());
        code.emit(Opcode.IFFALSE, statement.hasFalseStatements() ? labels.get(0) : labels.get(1));
        dispatchAll(statement.getTrueStatements());
        if (statement.hasFalseStatements()) {
            code.emit(Opcode.GOTO, labels.get(1));
            code.emit(labels.get(0));
            dispatchAll(statement.getFalseStatements());
        }
        code.emit(labels.get(1));
    }

    public void visit(ForNextStatement statement, StatementContext context) {
        var labels = label("FOR", "FORX");
        assignment(new VariableReference(statement.getSymbol()), statement.getStart());
        code.emit(labels.get(0));

        // Note: We don't have a GE at this time.
        // FIXME: If STEP is a variable, code generated will be incorrect.
        boolean stepIsNegative = statement.getStep() instanceof IntegerConstant e && e.getValue() < 0;
        if (stepIsNegative) {
            dispatch(statement.getEnd());
            emitLoad(statement.getSymbol());
        }
        else {
            emitLoad(statement.getSymbol());
            dispatch(statement.getEnd());
        }
        code.emit(Opcode.LE);

        code.emit(Opcode.IFFALSE, labels.get(1));
        dispatchAll(statement);

        // Lean on the binary expression processor to setup an optimized STEP increment.
        BinaryExpression stepIncrementExpr = new BinaryExpression(
            new VariableReference(statement.getSymbol()),
            statement.getStep(), "+");
        visit(stepIncrementExpr);

        emitStore(statement.getSymbol());
        code.emit(Opcode.GOTO, labels.get(0));
        code.emit(labels.get(1));
    }

    @Override
    public void visit(ForStatement statement, StatementContext context) {
        var labels = label(
            String.format("do_%s_for", statement.getSymbol().name()),
            String.format("do_%s_next", statement.getSymbol().name()));
        // initialize loop variables
        // - start value
        dispatch(statement.getStart());
        emitStore(statement.getSymbol());
        // - end value
        dispatch(statement.getEnd());
        emitStore(statement.getFrame().getEndRef());
        // - step value
        dispatch(statement.getStep());
        emitStore(statement.getFrame().getStepRef());
        // - next address-1 (can be multiple FOR loops with this variable)
        code.emit(Opcode.LOADA, labels.get(1));
        code.emit(Opcode.DECR);
        emitStore(statement.getFrame().getNextRef());
        // skip around the next logic
        code.emit(Opcode.GOTO, labels.get(0));

        // FOR NEXT logic here
        // - add step value
        code.emit(labels.get(1));
        emitLoad(statement.getSymbol());
        emitLoad(statement.getFrame().getStepRef());
        code.emit(Opcode.ADD);
        emitStore(statement.getSymbol());
        // - test end value (same bugs as FOR above!)  FIXME
        //   step is:
        //     neg: IF X < END THEN EXIT LOOP
        //     pos: IF X > END THEN EXIT LOOP
        boolean stepIsNegative = statement.getStep() instanceof IntegerConstant e && e.getValue() < 0;
        if (stepIsNegative) {
            emitLoad(statement.getSymbol());
            emitLoad(statement.getFrame().getEndRef());
        }
        else {
            emitLoad(statement.getFrame().getEndRef());
            emitLoad(statement.getSymbol());
        }
        code.emit(Opcode.LT);
        code.emit(Opcode.IFFALSE, labels.get(0));
        emitLoad(statement.getFrame().getExitRef());
        code.emit(Opcode.RETURN);

        // do the for statements!
        code.emit(labels.get(0));
    }

    @Override
    public void visit(NextStatement statement, StatementContext context) {
        // save our exit address-1
        code.emit(Opcode.LOADA, statement.getExitLabel());
        code.emit(Opcode.DECR);
        emitStore(statement.getFrame().getExitRef());
        // dynamic GOTO
        emitLoad(statement.getFrame().getNextRef());
        code.emit(Opcode.RETURN);
        code.emit(statement.getExitLabel());
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
        code.emit(statement.getId());
    }

    @Override
    public void visit(GotoGosubStatement statement, StatementContext context) {
        code.emit(Opcode.valueOf(statement.getOp().toUpperCase()), statement.getId());
    }

    @Override
    public void visit(PopStatement statement, StatementContext context) {
        // This is an address; expect that 2 is always correct!
        code.emit(Opcode.POPN, 2);
    }

    @Override
    public void visit(ReturnStatement statement, StatementContext context) {
        boolean hasReturnValue = statement.getExpr() != null;
        if (this.frames.peek().scope() instanceof Function f){
            var refs = this.frames.peek().scope().findByType(Scope.Type.RETURN_VALUE);
            if (refs.size() == 1 && hasReturnValue) {
                assignment(new VariableReference(refs.get(0)), statement.getExpr());
            } else if (refs.size() != 0 || hasReturnValue) {
                throw new RuntimeException("function return mismatch");
            }
            code.emit(Opcode.GOTO, f.getExitLabel());
        }
        else if (hasReturnValue) {
            throw new RuntimeException("cannot return value from a subroutine");
        }
        else {
            code.emit(Opcode.RETURN);
        }
    }

    @Override
    public void visit(CallSubroutine statement, StatementContext context) {
        boolean hasParameters = statement.getParameters().size() != 0;
        for (Expression expr : statement.getParameters()) {
            dispatch(expr);
        }
        code.emit(Opcode.GOSUB, statement.getName());
        if (hasParameters) {
            // FIXME when we have more datatypes this will be incorrect
            code.emit(Opcode.POPN, statement.getParameters().size() * 2);
        }
    }

    @Override
    public void visit(Subroutine subroutine) {
        var hasLocalScope = subroutine.findByType(Scope.Type.PARAMETER, Scope.Type.LOCAL).size() != 0;
        var frame = frames.push(Frame.create(subroutine));
        code.emit(subroutine.getName());
        if (hasLocalScope) code.emit(Opcode.LOCAL_RESERVE, frame.localSize());
        if (subroutine.getStatements() != null) {
            dispatchAll(subroutine);
        }
        if (hasLocalScope) code.emit(Opcode.LOCAL_FREE, frame.localSize());
        code.emit(Opcode.RETURN);
        frames.pop();
    }

    @Override
    public void visit(Function function) {
        var hasLocalScope = function.findByType(Scope.Type.PARAMETER, Scope.Type.LOCAL).size() != 0;
        var frame = frames.push(Frame.create(function));
        var labels = label("FUNCXIT");
        var exitLabel = labels.get(0);
        function.setExitLabel(exitLabel);
        code.emit(function.getName());
        if (hasLocalScope) code.emit(Opcode.LOCAL_RESERVE, frame.localSize());
        if (function.getStatements() != null) {
            dispatchAll(function);
        }
        code.emit(exitLabel);
        if (hasLocalScope) code.emit(Opcode.LOCAL_FREE, frame.localSize());
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
        emitLoad(expression);
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
        String actual = code.emitConstant(label.get(0), expression.getValue());
        code.emit(Opcode.LOADA, actual);
        return null;
    }

    @Override
    public Expression visit(ParenthesisExpression expression) {
        dispatch(expression.getExpr());
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
            default -> {
                if (function.getFunction() == null) {
                    throw new RuntimeException("unimplemented standard function: " + function.getName());
                }
                // FIXME need to reserve by return type when types are in place!
                code.emit(Opcode.LOADC, 0);
                emitParameters.run();
                code.emit(Opcode.GOSUB, function.getName());
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
}
