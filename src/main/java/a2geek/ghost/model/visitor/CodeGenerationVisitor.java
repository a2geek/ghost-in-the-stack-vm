package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Program;
import a2geek.ghost.model.Visitor;
import a2geek.ghost.model.code.CodeBlock;
import a2geek.ghost.model.code.Instruction;
import a2geek.ghost.model.code.Opcode;
import a2geek.ghost.model.expression.BinaryExpression;
import a2geek.ghost.model.expression.IdentifierExpression;
import a2geek.ghost.model.expression.IntegerConstant;
import a2geek.ghost.model.statement.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
            code.emit(Opcode.RESERVE, variables.size());
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
    public void visit(IfStatement statement) {
        var labels = label("IFT", "IFX");
        dispatch(statement.getExpression());
        code.emit(Opcode.IFTRUE, labels.get(0));
        statement.getFalseStatements().getStatements().forEach(this::dispatch);
        code.emit(Opcode.GOTO, labels.get(1));
        code.emit(labels.get(0));
        statement.getTrueStatements().getStatements().forEach(this::dispatch);
        code.emit(labels.get(1));
    }

    public void visit(ForStatement statement) {
        var labels = label("FOR", "FORX");
        visit(new AssignmentStatement(statement.getId(), statement.getStart()));
        code.emit(labels.get(0));
        code.emit(Opcode.LOAD, varOffset(statement.getId()));
        dispatch(statement.getEnd());
        code.emit(Opcode.LT);
        code.emit(Opcode.IFFALSE, labels.get(1));
        statement.getStatements().forEach(this::dispatch);
        code.emit(Opcode.LOAD, varOffset(statement.getId()));
        code.emit(Opcode.LOADC, 1);
        code.emit(Opcode.ADD);
        code.emit(Opcode.STORE, varOffset(statement.getId()));
        code.emit(Opcode.GOTO, labels.get(0));
        code.emit(labels.get(1));
    }

    public void visit(GrStatement statement) {
        code.emit(Opcode.LOADC, 0xfb40);
        code.emit(Opcode.CALL);
    }

    public void visit(PlotStatement statement) {
        dispatch(statement.getX());
        code.emit(Opcode.SETYREG);
        dispatch(statement.getY());
        code.emit(Opcode.SETACC);
        code.emit(Opcode.LOADC, 0xf800);
        code.emit(Opcode.CALL);
    }

    public Expression visit(BinaryExpression expression) {
        // FIXME: Special case ">" isn't implemented, so swap arguments and use LT.
        if (">".equals(expression.getOp())) {
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
            case "<" -> code.emit(Opcode.LT);
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

}
