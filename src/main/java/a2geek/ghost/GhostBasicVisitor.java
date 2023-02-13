package a2geek.ghost;

import a2geek.ghost.antlr.BasicBaseVisitor;
import a2geek.ghost.antlr.BasicParser;
import a2geek.ghost.model.*;

public class GhostBasicVisitor extends BasicBaseVisitor<Expression> {
    private Program program;
    private CodeBlock codeBlock;

    public Program getProgram() {
        return program;
    }

    @Override
    public Expression visitProgram(BasicParser.ProgramContext ctx) {
        this.program = new Program();
        this.codeBlock = this.program;
        return super.visitProgram(ctx);
    }

    @Override
    public Expression visitAssignment(BasicParser.AssignmentContext ctx) {
        String id = ctx.id.getText();
        Expression expr = visit(ctx.a);
        AssignmentStatement assignmentStatement = new AssignmentStatement(id, expr);
        codeBlock.addStatement(assignmentStatement);
        return null;
    }

    @Override
    public Expression visitGrStmt(BasicParser.GrStmtContext ctx) {
        codeBlock.addStatement(new GrStatement());
        return null;
    }

    @Override
    public Expression visitForLoop(BasicParser.ForLoopContext ctx) {
        String id = ctx.id.getText();
        Expression start = visit(ctx.a);
        Expression end = visit(ctx.b);

        CodeBlock oldCodeBlock = this.codeBlock;
        ForStatement forStatement = new ForStatement(id, start, end);
        codeBlock = forStatement;
        visit(ctx.s);
        codeBlock = oldCodeBlock;
        codeBlock.addStatement(forStatement);
        return null;
    }

    @Override
    public Expression visitColorStmt(BasicParser.ColorStmtContext ctx) {
        Expression expr = visit(ctx.a);
        ColorStatement colorStatement = new ColorStatement(expr);
        codeBlock.addStatement(colorStatement);
        return null;
    }

    @Override
    public Expression visitPlotStmt(BasicParser.PlotStmtContext ctx) {
        Expression x = visit(ctx.a);
        Expression y = visit(ctx.b);
        PlotStatement plotStatement = new PlotStatement(x, y);
        codeBlock.addStatement(plotStatement);
        return null;
    }

    @Override
    public Expression visitEndStmt(BasicParser.EndStmtContext ctx) {
        codeBlock.addStatement(new EndStatement());
        return null;
    }

    @Override
    public Expression visitIdentifier(BasicParser.IdentifierContext ctx) {
        return new IdentifierExpression(ctx.a.getText());
    }

    @Override
    public Expression visitIntConstant(BasicParser.IntConstantContext ctx) {
        return new IntegerConstant(Integer.parseInt(ctx.a.getText()));
    }

    @Override
    public Expression visitAddSubExpr(BasicParser.AddSubExprContext ctx) {
        Expression l = visit(ctx.a);
        Expression r = visit(ctx.b);
        String op = ctx.op.getText();
        return new BinaryExpression(l, r, op);
    }

    @Override
    public Expression visitMulDivModExpr(BasicParser.MulDivModExprContext ctx) {
        Expression l = visit(ctx.a);
        Expression r = visit(ctx.b);
        String op = ctx.op.getText();
        return new BinaryExpression(l, r, op);
    }
}
