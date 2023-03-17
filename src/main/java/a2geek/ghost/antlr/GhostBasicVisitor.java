package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.BasicBaseVisitor;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.antlr.generated.BasicParser.CompExprContext;
import a2geek.ghost.antlr.generated.BasicParser.IfStatementContext;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Program;
import a2geek.ghost.model.StatementBlock;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.statement.*;
import org.antlr.v4.runtime.tree.ParseTree;

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
    public Expression visitIfShortStatement(BasicParser.IfShortStatementContext ctx) {
        Expression expr = visit(ctx.a);

        StatementBlock oldStatementBlock = this.statementBlock;
        StatementBlock trueStatements = new BaseStatementBlock();

        statementBlock = trueStatements;
        visit(ctx.t);
        statementBlock = oldStatementBlock;

        IfStatement statement = new IfStatement(expr, trueStatements, null);
        statementBlock.addStatement(statement);
        return null;
    }

    @Override
    public Expression visitIfStatement(IfStatementContext ctx) {
        Expression expr = visit(ctx.a);

        StatementBlock oldStatementBlock = this.statementBlock;
        StatementBlock trueStatements = new BaseStatementBlock();
        StatementBlock falseStatements = null;

        statementBlock = trueStatements;
        visit(ctx.t);
        if (ctx.f != null) {
            falseStatements = new BaseStatementBlock();
            statementBlock = falseStatements;
            visit(ctx.f);
        }
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
        Expression step = null;

        if (ctx.c == null) {
            step = new IntegerConstant(1);
        }
        else {
            step = visit(ctx.c);
        }

        StatementBlock oldStatementBlock = this.statementBlock;
        ForStatement forStatement = new ForStatement(id, start, end, step);
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
    public Expression visitHlinStmt(BasicParser.HlinStmtContext ctx) {
        var a = visit(ctx.a);
        var b = visit(ctx.b);
        var y = visit(ctx.y);
        HlinStatement hlinStatement = new HlinStatement(a, b, y);
        statementBlock.addStatement(hlinStatement);
        return null;
    }

    @Override
    public Expression visitVlinStmt(BasicParser.VlinStmtContext ctx) {
        var a = visit(ctx.a);
        var b = visit(ctx.b);
        var x = visit(ctx.x);
        VlinStatement vlinStatement = new VlinStatement(a, b, x);
        statementBlock.addStatement(vlinStatement);
        return null;
    }

    @Override
    public Expression visitEndStmt(BasicParser.EndStmtContext ctx) {
        statementBlock.addStatement(new EndStatement());
        return null;
    }

    @Override
    public Expression visitHomeStmt(BasicParser.HomeStmtContext ctx) {
        statementBlock.addStatement(new HomeStatement());
        return null;
    }

    @Override
    public Expression visitPrintStmt(BasicParser.PrintStmtContext ctx) {
        PrintStatement printStatement = new PrintStatement();
        // Print is a little bit different in that we need to pay attention to syntax,
        // so this tortured code handles that.
        boolean semiColonAtEnd = false;
        for (ParseTree pt : ctx.children) {
            semiColonAtEnd = false;
            if ("print".equalsIgnoreCase(pt.getText())) {
                // this is the zeroth element
            }
            else if (";".equals(pt.getText())) {
                semiColonAtEnd = true;
            }
            else if (",".equals(pt.getText())) {
                printStatement.addCommaAction();
            }
            else {
                Expression expr = pt.accept(this);
                printStatement.addIntegerAction(expr);
            }
        }
        if (!semiColonAtEnd) {
            printStatement.addNewlineAction();
        }

        statementBlock.addStatement(printStatement);
        return null;
    }

    public Expression visitCallStmt(BasicParser.CallStmtContext ctx) {
        Expression expr = visit(ctx.a);
        CallStatement callStatement = new CallStatement(expr);
        statementBlock.addStatement(callStatement);
        return null;
    }

    @Override
    public Expression visitPokeStmt(BasicParser.PokeStmtContext ctx) {
        var a = visit(ctx.a);
        var b = visit(ctx.b);
        statementBlock.addStatement(new PokeStatement(a, b));
        return null;
    }

    @Override
    public Expression visitLabel(BasicParser.LabelContext ctx) {
        statementBlock.addStatement(new LabelStatement(ctx.id.getText()));
        return null;
    }

    @Override
    public Expression visitGotoGosubStmt(BasicParser.GotoGosubStmtContext ctx) {
        statementBlock.addStatement(new GotoGosubStatement(ctx.op.getText().toLowerCase(), ctx.l.getText()));
        return null;
    }

    @Override
    public Expression visitReturnStmt(BasicParser.ReturnStmtContext ctx) {
        statementBlock.addStatement(new ReturnStatement());
        return null;
    }

    @Override
    public Expression visitTextStmt(BasicParser.TextStmtContext ctx) {
        statementBlock.addStatement(new TextStatement());
        return null;
    }

    @Override
    public Expression visitHtabStmt(BasicParser.HtabStmtContext ctx) {
        var a = visit(ctx.a);
        statementBlock.addStatement(new HtabStatement(a));
        return null;
    }

    @Override
    public Expression visitVtabStmt(BasicParser.VtabStmtContext ctx) {
        var a = visit(ctx.a);
        statementBlock.addStatement(new VtabStatement(a));
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
        String op = ctx.op.getText().toLowerCase();
        return new BinaryExpression(l, r, op);
    }

    @Override
    public Expression visitParenExpr(BasicParser.ParenExprContext ctx) {
        Expression e = visit(ctx.a);
        return new ParenthesisExpression(e);
    }

    @Override
    public Expression visitNegateExpr(BasicParser.NegateExprContext ctx) {
        Expression e = visit(ctx.a);
        return new NegateExpression(e);
    }

    @Override
    public Expression visitPeekExpr(BasicParser.PeekExprContext ctx) {
        Expression e = visit(ctx.a);
        return new FunctionExpression("peek", e);
    }
}
