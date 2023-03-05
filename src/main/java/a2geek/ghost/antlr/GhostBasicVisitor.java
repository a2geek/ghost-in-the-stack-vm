package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.BasicBaseVisitor;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.antlr.generated.BasicParser.CompExprContext;
import a2geek.ghost.antlr.generated.BasicParser.IfStatementContext;
import a2geek.ghost.model.StatementBlock;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Program;
import a2geek.ghost.model.expression.BinaryExpression;
import a2geek.ghost.model.expression.IdentifierExpression;
import a2geek.ghost.model.expression.IntegerConstant;
import a2geek.ghost.model.statement.*;

public class GhostBasicVisitor extends BasicBaseVisitor<Expression> {
    private Program program;
    private StatementBlock statementBlock;

    public Program getProgram() {
        return program;
    }

    @Override
    public Expression visitProgram(BasicParser.ProgramContext ctx) {
        this.program = new Program();
        this.statementBlock = this.program;
        return super.visitProgram(ctx);
    }

    @Override
    public Expression visitAssignment(BasicParser.AssignmentContext ctx) {
        String id = ctx.id.getText();
        Expression expr = visit(ctx.a);
        AssignmentStatement assignmentStatement = new AssignmentStatement(id, expr);
        statementBlock.addStatement(assignmentStatement);
        return null;
    }

    @Override
    public Expression visitGrStmt(BasicParser.GrStmtContext ctx) {
        statementBlock.addStatement(new GrStatement());
        return null;
    }

    @Override
    public Expression visitIfStatement(IfStatementContext ctx) {
        Expression expr = visit(ctx.a);

        StatementBlock oldStatementBlock = this.statementBlock;
        StatementBlock trueStatements = new BaseStatementBlock();
        StatementBlock falseStatements = new BaseStatementBlock();

        statementBlock = trueStatements;
        visit(ctx.t);
        statementBlock = falseStatements;
        visit(ctx.f);
        statementBlock = oldStatementBlock;

        IfStatement statement = new IfStatement(expr, trueStatements, falseStatements);
        statementBlock.addStatement(statement);
        return null;
    }

    @Override
    public Expression visitForLoop(BasicParser.ForLoopContext ctx) {
        String id = ctx.id.getText();
        Expression start = visit(ctx.a);
        Expression end = visit(ctx.b);

        StatementBlock oldStatementBlock = this.statementBlock;
        ForStatement forStatement = new ForStatement(id, start, end);
        statementBlock = forStatement;
        visit(ctx.s);
        statementBlock = oldStatementBlock;
        statementBlock.addStatement(forStatement);
        return null;
    }

    @Override
    public Expression visitColorStmt(BasicParser.ColorStmtContext ctx) {
        Expression expr = visit(ctx.a);
        ColorStatement colorStatement = new ColorStatement(expr);
        statementBlock.addStatement(colorStatement);
        return null;
    }

    @Override
    public Expression visitPlotStmt(BasicParser.PlotStmtContext ctx) {
        Expression x = visit(ctx.a);
        Expression y = visit(ctx.b);
        PlotStatement plotStatement = new PlotStatement(x, y);
        statementBlock.addStatement(plotStatement);
        return null;
    }

    @Override
    public Expression visitEndStmt(BasicParser.EndStmtContext ctx) {
        statementBlock.addStatement(new EndStatement());
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
    public Expression visitCompExpr(CompExprContext ctx) {
        Expression l = visit(ctx.a);
        Expression r = visit(ctx.b);
        String op = ctx.op.getText();
        return new BinaryExpression(l, r, op);
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
