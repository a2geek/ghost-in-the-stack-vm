package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Program;
import a2geek.ghost.model.Visitor;
import a2geek.ghost.model.code.CodeBlock;
import a2geek.ghost.model.code.Instruction;
import a2geek.ghost.model.code.Opcode;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.statement.*;

import java.util.*;

public class CodeGenerationVisitor extends Visitor {
    private List<String> variables = new ArrayList<>();
    private CodeBlock code = new CodeBlock();
    private int labelNumber;

    public CodeGenerationVisitor(Collection<String> variables) {
        this.variables.addAll(variables);
    }

    public List<Instruction> getInstructions() {
        return code.getInstructions();
    }

    int varOffset(String var) {
        // TODO: this will need to be fixed when more than integer are supported!
        var num = variables.indexOf(var);
        if (num == -1) {
            throw new RuntimeException("Variable is unknown: " + var);
        }
        return num * 2;
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
        if (!variables.isEmpty()) {
            // TODO: this will need to be fixed when more than integer are supported!
            code.emit(Opcode.RESERVE, variables.size() * 2);
        }
        super.visit(program);
    }

    @Override
    public void visit(AssignmentStatement statement) {
        dispatch(statement.getExpr());
        code.emit(Opcode.STORE, varOffset(statement.getId()));
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
                code.emit(Opcode.LOADC, 0x100);
                code.emit(Opcode.DIV);
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
            else if (action instanceof PrintStatement.PrintStringAction a) {
                dispatch(a.getExpr());
                code.emit(Opcode.DUP);
                code.emit(Opcode.SETACC);
                code.emit(Opcode.LOADC, 0x100);
                code.emit(Opcode.DIV);
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
        visit(new AssignmentStatement(statement.getId(), statement.getStart()));
        code.emit(labels.get(0));

        // Note: We don't have a GE at this time.
        // FIXME: If STEP is a variable, code generated will be incorrect.
        boolean stepIsNegative = statement.getStep() instanceof IntegerConstant e && e.getValue() < 0;
        if (stepIsNegative) {
            dispatch(statement.getEnd());
            code.emit(Opcode.LOAD, varOffset(statement.getId()));
        }
        else {
            code.emit(Opcode.LOAD, varOffset(statement.getId()));
            dispatch(statement.getEnd());
        }
        code.emit(Opcode.LE);

        code.emit(Opcode.IFFALSE, labels.get(1));
        statement.getStatements().forEach(this::dispatch);

        // Lean on the binary expression processor to setup an optimized STEP increment.
        BinaryExpression stepIncrementExpr = new BinaryExpression(
            new IdentifierExpression(statement.getId()),
            statement.getStep(), "+");
        visit(stepIncrementExpr);

        code.emit(Opcode.STORE, varOffset(statement.getId()));
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
        code.emit(Opcode.RETURN);
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
            default -> throw new RuntimeException("Operation not supported: " + expression.getOp());
        }
        return null;
    }

    public Expression visit(IdentifierExpression expression) {
        code.emit(Opcode.LOAD, varOffset(expression.getId()));
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
    public Expression visit(FunctionExpression expression) {
        for (Expression expr : expression.getExpr()) {
            dispatch(expr);
        }
        switch (expression.getName()) {
            case "peek" -> code.emit(Opcode.ILOAD);
            case "scrn" -> {
                code.emit(Opcode.SETACC);
                code.emit(Opcode.SETYREG);
                code.emit(Opcode.LOADC, 0xf871);
                code.emit(Opcode.CALL);
                code.emit(Opcode.GETACC);
            }
            default -> throw new RuntimeException("Function unknown: " + expression.getName());
        }
        return null;
    }
    @Override
    public Expression visit(NegateExpression expression) {
        throw new RuntimeException("Negate is not implemented yet.");
    }
}
