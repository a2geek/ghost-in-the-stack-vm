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

    public int frameOffset(Reference ref) {
        if (this.frames.peek().offsets().containsKey(ref)) {
            return this.frames.peek().offsets().get(ref);
        }
       throw new RuntimeException("reference not in frame: " + ref);
    }

    public void emitLoad(Reference ref) {
        switch (ref.type()) {
            case LOCAL, PARAMETER, RETURN_VALUE -> {
                this.code.emit(Opcode.LOCAL_LOAD, frameOffset(ref));
            }
            case GLOBAL -> {
                this.code.emit(Opcode.GLOBAL_LOAD, frameOffset(ref));
            }
            case INTRINSIC -> {
                switch (ref.name().toLowerCase()) {
                    case Intrinsic.CPU_REGISTER_A -> this.code.emit(Opcode.GETACC);
                    case Intrinsic.CPU_REGISTER_X -> this.code.emit(Opcode.GETXREG);
                    case Intrinsic.CPU_REGISTER_Y -> this.code.emit(Opcode.GETYREG);
                    default -> throw new RuntimeException("unknown intrinsic: " + ref.name());
                }
            }
            case CONSTANT -> {
                if (ref.expr() instanceof IntegerConstant c) {
                    visit(c);
                }
                else if (ref.expr() instanceof StringConstant s) {
                    visit(s);
                }
                else {
                    throw new RuntimeException("unable to generate code for constant expression: " + ref);
                }
            }
        }
    }
    public void emitStore(Reference ref) {
        switch (ref.type()) {
            case LOCAL, PARAMETER, RETURN_VALUE -> {
                this.code.emit(Opcode.LOCAL_STORE, frameOffset(ref));
            }
            case GLOBAL -> {
                this.code.emit(Opcode.GLOBAL_STORE, frameOffset(ref));
            }
            case INTRINSIC -> {
                switch (ref.name().toLowerCase()) {
                    case Intrinsic.CPU_REGISTER_A -> this.code.emit(Opcode.SETACC);
                    case Intrinsic.CPU_REGISTER_X -> this.code.emit(Opcode.SETXREG);
                    case Intrinsic.CPU_REGISTER_Y -> this.code.emit(Opcode.SETYREG);
                    default -> throw new RuntimeException("unknown intrinsic: " + ref.name());
                }
            }
            case CONSTANT -> {
                throw new RuntimeException("cannot store to a constant value");
            }
        }
    }


    @Override
    public void visit(AssignmentStatement statement) {
        dispatch(statement.getExpr());
        emitStore(statement.getRef());
    }

    public void visit(ColorStatement statement) {
        dispatch(statement.getExpr());
        code.emit(Opcode.SETACC);
        code.emit(Opcode.LOADC, 0xf864);
        code.emit(Opcode.CALL);
    }

    public void visit(EndStatement statement) {
        code.emit(Opcode.EXIT);
    }

    @Override
    public void visit(HomeStatement statement) {
        code.emit(Opcode.LOADC, 0xfc58);
        code.emit(Opcode.CALL);
    }

    @Override
    public void visit(PrintStatement statement) {
        for (PrintStatement.Action action : statement.getActions()) {
            if (action instanceof PrintStatement.PrintCommaAction a) {
                // FIXME. Just spacing between.
                code.emit(Opcode.LOADC, 0xa0);
                code.emit(Opcode.SETACC);
                code.emit(Opcode.LOADC, 0xfded);
                code.emit(Opcode.CALL);
            }
            else if (action instanceof PrintStatement.PrintIntegerAction a) {
                // Defer to Applesoft for printing
                dispatch(a.getExpr());
                code.emit(Opcode.DUP);
                code.emit(Opcode.SETYREG);
                code.emit(Opcode.LOADC, 8);
                code.emit(Opcode.SHIFTR);
                code.emit(Opcode.SETACC);
                code.emit(Opcode.LOADC, 0xe2f2);    // GIVAYF
                code.emit(Opcode.CALL);
                code.emit(Opcode.LOADC, 0xed34);    // FOUT
                code.emit(Opcode.CALL);
                // Note that we are "trusting" the interpreter
                // to preserve Y,A from FOUT for STROUT...
                code.emit(Opcode.LOADC, 0xdb3a);    // STROUT
                code.emit(Opcode.CALL);
            }
            else if (action instanceof PrintStatement.PrintBooleanAction a) {
                var label = label("STRTRUE", "STRFALSE", "PRINTTRUE", "PRINTFALSE");
                String lblTrue = code.emitConstant(label.get(0), "True");
                String lblFalse = code.emitConstant(label.get(1), "False");

                dispatch(a.getExpr());
                code.emit(Opcode.IFTRUE, label.get(2));
                code.emit(Opcode.LOADA, lblFalse);
                code.emit(Opcode.GOTO, label.get(3));
                code.emit(label.get(2));
                code.emit(Opcode.LOADA, lblTrue);
                code.emit(label.get(3));
                // duplicating print string code here
                code.emit(Opcode.DUP);
                code.emit(Opcode.SETACC);
                code.emit(Opcode.LOADC, 8);
                code.emit(Opcode.SHIFTR);
                code.emit(Opcode.SETYREG);
                code.emit(Opcode.LOADC, 0xdb3a);    // STROUT
                code.emit(Opcode.CALL);
            }
            else if (action instanceof PrintStatement.PrintStringAction a) {
                dispatch(a.getExpr());
                code.emit(Opcode.DUP);
                code.emit(Opcode.SETACC);
                code.emit(Opcode.LOADC, 8);
                code.emit(Opcode.SHIFTR);
                code.emit(Opcode.SETYREG);
                code.emit(Opcode.LOADC, 0xdb3a);    // STROUT
                code.emit(Opcode.CALL);
            }
            else if (action instanceof PrintStatement.PrintNewlineAction a) {
                code.emit(Opcode.LOADC, 0xfd8e);
                code.emit(Opcode.CALL);
            }
            else {
                throw new RuntimeException("Unexpected action class: " + action.getClass().getName());
            }
        }
    }

    @Override
    public void visit(CallStatement statement) {
        dispatch(statement.getExpr());
        code.emit(Opcode.CALL);
    }

    @Override
    public void visit(IfStatement statement) {
        var labels = label("IFF", "IFX");
        dispatch(statement.getExpression());
        code.emit(Opcode.IFFALSE, statement.hasFalseStatements() ? labels.get(0) : labels.get(1));
        statement.getTrueStatements().getStatements().forEach(this::dispatch);
        if (statement.hasFalseStatements()) {
            code.emit(Opcode.GOTO, labels.get(1));
            code.emit(labels.get(0));
            statement.getFalseStatements().getStatements().forEach(this::dispatch);
        }
        code.emit(labels.get(1));
    }

    public void visit(ForStatement statement) {
        var labels = label("FOR", "FORX");
        visit(new AssignmentStatement(statement.getRef(), statement.getStart()));
        code.emit(labels.get(0));

        // Note: We don't have a GE at this time.
        // FIXME: If STEP is a variable, code generated will be incorrect.
        boolean stepIsNegative = statement.getStep() instanceof IntegerConstant e && e.getValue() < 0;
        if (stepIsNegative) {
            dispatch(statement.getEnd());
            emitLoad(statement.getRef());
        }
        else {
            emitLoad(statement.getRef());
            dispatch(statement.getEnd());
        }
        code.emit(Opcode.LE);

        code.emit(Opcode.IFFALSE, labels.get(1));
        statement.getStatements().forEach(this::dispatch);

        // Lean on the binary expression processor to setup an optimized STEP increment.
        BinaryExpression stepIncrementExpr = new BinaryExpression(
            new IdentifierExpression(statement.getRef()),
            statement.getStep(), "+");
        visit(stepIncrementExpr);

        emitStore(statement.getRef());
        code.emit(Opcode.GOTO, labels.get(0));
        code.emit(labels.get(1));
    }

    public void visit(GrStatement statement) {
        code.emit(Opcode.LOADC, 0xfb40);
        code.emit(Opcode.CALL);
    }

    public void visit(PlotStatement statement) {
        if (statement.getY().equals(statement.getX())) {
            // Minor optimization if X=Y (aka PLOT 10,10)
            dispatch(statement.getY());
            code.emit(Opcode.DUP);
        } else {
            dispatch(statement.getY());
            dispatch(statement.getX());
        }
        code.emit(Opcode.SETYREG);  // X coordinate
        code.emit(Opcode.SETACC);   // Y coordinate
        code.emit(Opcode.LOADC, 0xf800);
        code.emit(Opcode.CALL);
    }

    @Override
    public void visit(HlinStatement statement) {
        dispatch(statement.getA());
        code.emit(Opcode.SETYREG);
        dispatch(statement.getB());
        code.emit(Opcode.LOADC, 0x2c);
        code.emit(Opcode.ISTORE);
        dispatch(statement.getY());
        code.emit(Opcode.SETACC);
        code.emit(Opcode.LOADC, 0xf819);
        code.emit(Opcode.CALL);
    }

    @Override
    public void visit(VlinStatement statement) {
        dispatch(statement.getA());
        code.emit(Opcode.SETACC);
        dispatch(statement.getB());
        code.emit(Opcode.LOADC, 0x2d);
        code.emit(Opcode.ISTORE);
        dispatch(statement.getX());
        code.emit(Opcode.SETYREG);
        code.emit(Opcode.LOADC, 0xf828);
        code.emit(Opcode.CALL);
    }

    @Override
    public void visit(PokeStatement statement) {
        dispatch(statement.getB());
        dispatch(statement.getA());
        code.emit(Opcode.ISTORE);
    }

    public void visit(LabelStatement statement) {
        code.emit(statement.getId());
    }

    @Override
    public void visit(GotoGosubStatement statement) {
        code.emit(Opcode.valueOf(statement.getOp().toUpperCase()), statement.getId());
    }

    @Override
    public void visit(ReturnStatement statement) {
        boolean hasReturnValue = statement.getExpr() != null;
        if (this.frames.peek().scope() instanceof Function f){
            var refs = this.frames.peek().scope().findByType(Scope.Type.RETURN_VALUE);
            if (refs.size() == 1 && hasReturnValue) {
                visit(new AssignmentStatement(refs.get(0), statement.getExpr()));
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

    public void visit(TextStatement statement) {
        code.emit(Opcode.LOADC, 0xfb2f);
        code.emit(Opcode.CALL);
    }

    @Override
    public void visit(HtabStatement statement) {
        dispatch(statement.getExpr());
        code.emit(Opcode.DECR);
        code.emit(Opcode.LOADC, 0x24);
        code.emit(Opcode.ISTORE);
    }

    @Override
    public void visit(VtabStatement statement) {
        dispatch(statement.getExpr());
        code.emit(Opcode.SETACC);
        code.emit(Opcode.LOADC, 0xfb5b);
        code.emit(Opcode.CALL);
    }

    @Override
    public void visit(CallSubroutine statement) {
        for (Expression expr : statement.getParameters()) {
            dispatch(expr);
        }
        code.emit(Opcode.GOSUB, statement.getName());
        // FIXME when we have more datatypes this will be incorrect
        code.emit(Opcode.POPN, statement.getParameters().size() * 2);
    }

    @Override
    public void visit(Subroutine subroutine) {
        var frame = frames.push(Frame.create(subroutine));
        code.emit(subroutine.getName());
        code.emit(Opcode.LOCAL_RESERVE, frame.localSize());
        if (subroutine.getStatements() != null) {
            subroutine.getStatements().forEach(this::dispatch);
        }
        code.emit(Opcode.LOCAL_FREE, frame.localSize());
        code.emit(Opcode.RETURN);
        frames.pop();
    }

    @Override
    public void visit(Function function) {
        var frame = frames.push(Frame.create(function));
        var labels = label("FUNCXIT");
        var exitLabel = labels.get(0);
        function.setExitLabel(exitLabel);
        code.emit(function.getName());
        code.emit(Opcode.LOCAL_RESERVE, frame.localSize());
        if (function.getStatements() != null) {
            function.getStatements().forEach(this::dispatch);
        }
        code.emit(exitLabel);
        code.emit(Opcode.LOCAL_FREE, frame.localSize());
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
            case "/" -> code.emit(Opcode.DIV);
            case "mod" -> code.emit(Opcode.MOD);
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

    public Expression visit(IdentifierExpression expression) {
        emitLoad(expression.getRef());
        return null;
    }

    public Expression visit(IntegerConstant expression) {
        code.emit(Opcode.LOADC, expression.getValue());
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
                code.emit(Opcode.ILOAD);
            }
            case "scrn" -> {
                emitParameters.run();
                code.emit(Opcode.SETACC);
                code.emit(Opcode.SETYREG);
                code.emit(Opcode.LOADC, 0xf871);
                code.emit(Opcode.CALL);
                code.emit(Opcode.GETACC);
            }
            default -> {
                if (function.getFunction() == null) {
                    throw new RuntimeException("unimplemented standard function: " + function.getName());
                }
                // FIXME need to reserve by return type when types are in place!
                code.emit(Opcode.LOADC, 0);
                emitParameters.run();
                code.emit(Opcode.GOSUB, function.getName());
                // FIXME when we have more datatypes this will be incorrect
                code.emit(Opcode.POPN, function.getParameters().size() * 2);
            }
        }
        return null;
    }
    @Override
    public Expression visit(UnaryExpression expression) {
        if ("-".equals(expression.getOp())) {
            code.emit(Opcode.LOADC, 0);
            dispatch(expression.getExpr());
            code.emit(Opcode.SUB);
        }
        else {
            throw new RuntimeException("unknown unary operator: " + expression.getOp());
        }
        return null;
    }
}
