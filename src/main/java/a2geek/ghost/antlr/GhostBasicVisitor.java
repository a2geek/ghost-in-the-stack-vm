package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.BasicBaseVisitor;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.antlr.generated.BasicParser.CompExprContext;
import a2geek.ghost.antlr.generated.BasicParser.IfStatementContext;
import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;

public class GhostBasicVisitor extends BasicBaseVisitor<Expression> {
    private Function<String,String> caseStrategy;
    private Stack<Scope> scope = new Stack<>();
    private Stack<StatementBlock> statementBlock = new Stack<>();

    public GhostBasicVisitor(Function<String,String> caseStrategy) {
        this.caseStrategy = caseStrategy;
    }

    public Program getProgram() {
        if (scope.size() == 1 && scope.peek() instanceof Program program) {
            return program;
        }
        throw new RuntimeException("Unexpected scope state at end of evaluation. " +
                "Should be 1 but is " + scope.size());
    }

    @Override
    public Expression visitProgram(BasicParser.ProgramContext ctx) {
        Program program = new Program(caseStrategy);
        this.scope.push(program);
        this.statementBlock.push(program);
        return super.visitProgram(ctx);
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
    public void addScope(Scope scope) {
        this.scope.peek().addScope(scope);
    }
    public Scope pushScope(Scope scope) {
        return this.scope.push(scope);
    }
    public Scope popScope() {
        return this.scope.pop();
    }

    public Reference addVariable(String name) {
        return this.scope.peek().addLocalVariable(name);
    }

    @Override
    public Expression visitAssignment(BasicParser.AssignmentContext ctx) {
        Reference ref = addVariable(ctx.id.getText());
        Expression expr = visit(ctx.a);
        AssignmentStatement assignmentStatement = new AssignmentStatement(ref, expr);
        addStatement(assignmentStatement);
        return null;
    }

    @Override
    public Expression visitGrStmt(BasicParser.GrStmtContext ctx) {
        addStatement(new GrStatement());
        return null;
    }

    @Override
    public Expression visitIfShortStatement(BasicParser.IfShortStatementContext ctx) {
        Expression expr = visit(ctx.a);

        StatementBlock trueStatements = pushStatementBlock(new StatementBlock());
        visit(ctx.t);
        popStatementBlock();

        IfStatement statement = new IfStatement(expr, trueStatements, null);
        addStatement(statement);
        return null;
    }

    @Override
    public Expression visitIfStatement(IfStatementContext ctx) {
        Expression expr = visit(ctx.a);

        StatementBlock trueStatements = pushStatementBlock(new StatementBlock());
        visit(ctx.t);
        popStatementBlock();

        StatementBlock falseStatements = null;
        if (ctx.f != null) {
            falseStatements = pushStatementBlock(new StatementBlock());
            visit(ctx.f);
            popStatementBlock();
        }

        IfStatement statement = new IfStatement(expr, trueStatements, falseStatements);
        addStatement(statement);
        return null;
    }

    @Override
    public Expression visitForLoop(BasicParser.ForLoopContext ctx) {
        Reference ref = addVariable(ctx.id.getText());
        Expression start = visit(ctx.a);
        Expression end = visit(ctx.b);
        Expression step = null;

        if (ctx.c == null) {
            step = new IntegerConstant(1);
        }
        else {
            step = visit(ctx.c);
        }

        ForStatement forStatement = new ForStatement(ref, start, end, step);
        if (ctx.s != null) {
            pushStatementBlock(forStatement);
            visit(ctx.s);
            popStatementBlock();
        }
        addStatement(forStatement);
        return null;
    }

    @Override
    public Expression visitColorStmt(BasicParser.ColorStmtContext ctx) {
        Expression expr = visit(ctx.a);
        ColorStatement colorStatement = new ColorStatement(expr);
        addStatement(colorStatement);
        return null;
    }

    @Override
    public Expression visitPlotStmt(BasicParser.PlotStmtContext ctx) {
        Expression x = visit(ctx.a);
        Expression y = visit(ctx.b);
        PlotStatement plotStatement = new PlotStatement(x, y);
        addStatement(plotStatement);
        return null;
    }

    @Override
    public Expression visitHlinStmt(BasicParser.HlinStmtContext ctx) {
        var a = visit(ctx.a);
        var b = visit(ctx.b);
        var y = visit(ctx.y);
        HlinStatement hlinStatement = new HlinStatement(a, b, y);
        addStatement(hlinStatement);
        return null;
    }

    @Override
    public Expression visitVlinStmt(BasicParser.VlinStmtContext ctx) {
        var a = visit(ctx.a);
        var b = visit(ctx.b);
        var x = visit(ctx.x);
        VlinStatement vlinStatement = new VlinStatement(a, b, x);
        addStatement(vlinStatement);
        return null;
    }

    @Override
    public Expression visitEndStmt(BasicParser.EndStmtContext ctx) {
        addStatement(new EndStatement());
        return null;
    }

    @Override
    public Expression visitHomeStmt(BasicParser.HomeStmtContext ctx) {
        addStatement(new HomeStatement());
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
                switch (expr.getType()) {
                    case INTEGER -> printStatement.addIntegerAction(expr);
                    case STRING -> printStatement.addStringAction(expr);
                    default -> throw new RuntimeException("Unsupported PRINT type: " + expr.getType());
                }
            }
        }
        if (!semiColonAtEnd) {
            printStatement.addNewlineAction();
        }

        addStatement(printStatement);
        return null;
    }

    public Expression visitCallStmt(BasicParser.CallStmtContext ctx) {
        Expression expr = visit(ctx.a);
        CallStatement callStatement = new CallStatement(expr);
        addStatement(callStatement);
        return null;
    }

    @Override
    public Expression visitPokeStmt(BasicParser.PokeStmtContext ctx) {
        var a = visit(ctx.a);
        var b = visit(ctx.b);
        addStatement(new PokeStatement(a, b));
        return null;
    }

    @Override
    public Expression visitLabel(BasicParser.LabelContext ctx) {
        addStatement(new LabelStatement(ctx.id.getText()));
        return null;
    }

    @Override
    public Expression visitGotoGosubStmt(BasicParser.GotoGosubStmtContext ctx) {
        addStatement(new GotoGosubStatement(ctx.op.getText().toLowerCase(), ctx.l.getText()));
        return null;
    }

    @Override
    public Expression visitReturnStmt(BasicParser.ReturnStmtContext ctx) {
        addStatement(new ReturnStatement());
        return null;
    }

    @Override
    public Expression visitTextStmt(BasicParser.TextStmtContext ctx) {
        addStatement(new TextStatement());
        return null;
    }

    @Override
    public Expression visitHtabStmt(BasicParser.HtabStmtContext ctx) {
        var a = visit(ctx.a);
        addStatement(new HtabStatement(a));
        return null;
    }

    @Override
    public Expression visitVtabStmt(BasicParser.VtabStmtContext ctx) {
        var a = visit(ctx.a);
        addStatement(new VtabStatement(a));
        return null;
    }

    @Override
    public Expression visitSubDecl(BasicParser.SubDeclContext ctx) {
        String name = caseStrategy.apply(ctx.id.getText());
        List<String> params = new ArrayList<>();
        if (ctx.p != null) {
            ctx.p.ID().stream().map(ParseTree::getText).forEach(params::add);
        }

        Subroutine sub = new Subroutine(scope.peek(), name, params);
        addScope(sub);

        pushScope(sub);
        pushStatementBlock(sub);
        visit(ctx.s);
        popScope();
        popStatementBlock();

        return null;
    }

    @Override
    public Expression visitCallSub(BasicParser.CallSubContext ctx) {
        String name = caseStrategy.apply(ctx.id.getText());
        List<Expression> params = new ArrayList<>();
        if (ctx.p != null) {
            ctx.p.expr().stream().map(this::visit).forEach(params::add);
        }

        CallSubroutine callSubroutine = new CallSubroutine(name, params);
        addStatement(callSubroutine);

        return null;
    }

    @Override
    public Expression visitIdentifier(BasicParser.IdentifierContext ctx) {
        Reference ref = addVariable(ctx.a.getText());
        return new IdentifierExpression(ref);
    }

    @Override
    public Expression visitIntConstant(BasicParser.IntConstantContext ctx) {
        return new IntegerConstant(Integer.parseInt(ctx.a.getText()));
    }

    @Override
    public Expression visitStringConstant(BasicParser.StringConstantContext ctx) {
        return new StringConstant(ctx.s.getText().replaceAll("^\"|\"$", ""));
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
    public Expression visitLogicalExpr(BasicParser.LogicalExprContext ctx) {
        var l = visit(ctx.a);
        var r = visit(ctx.b);
        return new BinaryExpression(l, r, ctx.op.getText());
    }

    @Override
    public Expression visitPeekExpr(BasicParser.PeekExprContext ctx) {
        Expression e = visit(ctx.a);
        return new FunctionExpression("peek", e);
    }

    @Override
    public Expression visitScrnExpr(BasicParser.ScrnExprContext ctx) {
        Expression x = visit(ctx.a);
        Expression y = visit(ctx.b);
        return new FunctionExpression("scrn", x, y);
    }

    @Override
    public Expression visitAscExpr(BasicParser.AscExprContext ctx) {
        Expression e = visit(ctx.s);
        return new FunctionExpression("asc", e);
    }
}
