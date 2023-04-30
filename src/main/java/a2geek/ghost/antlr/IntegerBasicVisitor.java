package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.IntegerBaseVisitor;
import a2geek.ghost.antlr.generated.IntegerParser;
import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.ForFrame;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.statement.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;

public class IntegerBasicVisitor extends IntegerBaseVisitor<Expression> {
    private ModelBuilder model;

    public IntegerBasicVisitor() {
        model = new ModelBuilder(String::toUpperCase);
    }

    public ModelBuilder getModel() {
        return model;
    }

    @Override
    public Expression visitProgram(IntegerParser.ProgramContext ctx) {
        super.visitProgram(ctx);
        if (!model.getProgram().isLastStatement(EndStatement.class)) {
            model.endStmt();
        }
        return null;
    }

    void callSubroutine(String name, ParseTree... expressions) {
        var exprs = new ArrayList<Expression>();
        for (var expression : expressions) {
            exprs.add(visit(expression));
        }
        model.callLibrarySubroutine(name, exprs.toArray(new Expression[0]));
    }

    void gotoGosub(String command, Token token) {
        var linenum = Integer.parseInt(token.getText());
        String label = String.format("L%d", linenum);
        model.gotoGosubStmt(command, label);
    }

    @Override
    public Expression visitProgramLine(IntegerParser.ProgramLineContext ctx) {
        model.labelStmt("L" + ctx.INTEGER().getText());
        visit(ctx.statements());
        return null;
    }

    @Override
    public Expression visitCallStatement(IntegerParser.CallStatementContext ctx) {
        var expr = visit(ctx.e);
        model.callAddr(expr);
        return null;
    }

    @Override
    public Expression visitClrStatement(IntegerParser.ClrStatementContext ctx) {
        System.out.println("CLR not supported; ignoring it.");
        return null;
    }

    @Override
    public Expression visitColorStatement(IntegerParser.ColorStatementContext ctx) {
        var expr = visit(ctx.e);
        model.callLibrarySubroutine("color", expr);
        return null;
    }

    @Override
    public Expression visitDelStatement(IntegerParser.DelStatementContext ctx) {
        System.out.println("DEL not supported; ignoring it.");
        return null;
    }

    @Override
    public Expression visitDimStatement(IntegerParser.DimStatementContext ctx) {
        throw new RuntimeException("DIM statement not implemented yet");
    }

    @Override
    public Expression visitDspStatement(IntegerParser.DspStatementContext ctx) {
        System.out.println("DSP not supported; ignoring it.");
        return null;
    }

    @Override
    public Expression visitEndStatement(IntegerParser.EndStatementContext ctx) {
        model.endStmt();
        return null;
    }

    Expression visitOrDefault(ParseTree tree, Expression defaultExpression) {
        return tree != null ? visit(tree) : defaultExpression;
    }

    @Override
    public Expression visitForStatement(IntegerParser.ForStatementContext ctx) {
        var ref = model.addVariable(ctx.ivar().getText(), DataType.INTEGER);
        var first = visit(ctx.first);
        var last = visit(ctx.last);
        Expression step = visitOrDefault(ctx.step, IntegerConstant.ONE);
        ForFrame frame = model.forFrame(ref);

        model.addStatement(new ForStatement(ref, first, last, step, frame));
        return null;
    }

    @Override
    public Expression visitNextStatement(IntegerParser.NextStatementContext ctx) {
        for (var ivar : ctx.ivar()) {
            var ref = model.addVariable(ivar.getText(), DataType.INTEGER);
            ForFrame frame = model.forFrame(ref);
            model.addStatement(new NextStatement(ref, frame));
        }
        return null;
    }

    @Override
    public Expression visitGosubGotoStatement(IntegerParser.GosubGotoStatementContext ctx) {
        gotoGosub(ctx.g.getText(), ctx.l);
        return null;
    }

    @Override
    public Expression visitGrStatement(IntegerParser.GrStatementContext ctx) {
        callSubroutine("gr");
        return null;
    }

    @Override
    public Expression visitHimemStatement(IntegerParser.HimemStatementContext ctx) {
        System.out.println("HIMEM not supported; ignoring it.");
        return null;
    }

    @Override
    public Expression visitHlinStatement(IntegerParser.HlinStatementContext ctx) {
        callSubroutine("hlin", ctx.x0, ctx.x1, ctx.y);
        return null;
    }

    @Override
    public Expression visitIfLineStatement(IntegerParser.IfLineStatementContext ctx) {
        var expr = visit(ctx.e);
        StatementBlock block = model.pushStatementBlock(new StatementBlock());
        gotoGosub("goto", ctx.l);
        model.popStatementBlock();
        model.ifStmt(expr, block, null);
        return null;
    }

    @Override
    public Expression visitIfStatement(IntegerParser.IfStatementContext ctx) {
        var expr = visit(ctx.e);
        StatementBlock block = model.pushStatementBlock(new StatementBlock());
        visit(ctx.s);
        model.popStatementBlock();
        model.ifStmt(expr, block, null);
        return null;
    }

    @Override
    public Expression visitInNumStatement(IntegerParser.InNumStatementContext ctx) {
        callSubroutine("innum", ctx.slot);
        return null;
    }

    @Override
    public Expression visitInputStatement(IntegerParser.InputStatementContext ctx) {
        throw new RuntimeException("input statement not implemented yet");
    }

    @Override
    public Expression visitIntegerAssignment(IntegerParser.IntegerAssignmentContext ctx) {
        var expr = visit(ctx.iexpr());
        // FIXME needs to support arrays
        var ref = model.addVariable(ctx.ivar().getText(), DataType.INTEGER);
        model.assignStmt(ref, expr);
        return null;
    }

    @Override
    public Expression visitStringAssignment(IntegerParser.StringAssignmentContext ctx) {
        throw new RuntimeException("string assignment TODO");
    }

    @Override
    public Expression visitListStatement(IntegerParser.ListStatementContext ctx) {
        System.out.println("LIST not supported; ignoring it.");
        return null;
    }

    @Override
    public Expression visitLomemStatement(IntegerParser.LomemStatementContext ctx) {
        System.out.println("LOMEM not supported; ignoring it.");
        return null;
    }

    @Override
    public Expression visitTraceStatement(IntegerParser.TraceStatementContext ctx) {
        System.out.println("TRACE not supported; ignoring it.");
        return null;
    }

    @Override
    public Expression visitPlotStatement(IntegerParser.PlotStatementContext ctx) {
        callSubroutine("plot", ctx.x, ctx.y);
        return null;
    }

    @Override
    public Expression visitPokeStatement(IntegerParser.PokeStatementContext ctx) {
        var addr = visit(ctx.addr);
        var e = visit(ctx.e);
        model.pokeStmt("poke", addr, e);
        return null;
    }

    @Override
    public Expression visitPopStatement(IntegerParser.PopStatementContext ctx) {
        // This is likely dangerous since we have no markers on the stack.
        // Uncertain how used it is. Failing for now.
        throw new RuntimeException("POP statement not supported.");
    }

    @Override
    public Expression visitPrNumStatement(IntegerParser.PrNumStatementContext ctx) {
        callSubroutine("prnum", ctx.slot);
        return null;
    }

    @Override
    public Expression visitPrintStatement(IntegerParser.PrintStatementContext ctx) {
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
                callSubroutine("comma");
            }
            else {
                Expression expr = pt.accept(this);
                switch (expr.getType()) {
                    case INTEGER -> callSubroutine("integer", pt);
                    case STRING -> callSubroutine("string", pt);
                    default -> throw new RuntimeException("Unsupported PRINT type: " + expr.getType());
                }
            }
        }
        if (!semiColonAtEnd) {
            callSubroutine("newline");
        }
        return null;
    }

    @Override
    public Expression visitRemarkStatement(IntegerParser.RemarkStatementContext ctx) {
        return null;
    }

    @Override
    public Expression visitReturnStatement(IntegerParser.ReturnStatementContext ctx) {
        model.returnStmt(null);
        return null;
    }

    @Override
    public Expression visitRunStatement(IntegerParser.RunStatementContext ctx) {
        throw new RuntimeException("RUN not supported.");
    }

    @Override
    public Expression visitTabStatement(IntegerParser.TabStatementContext ctx) {
        callSubroutine("htab", ctx.x);
        return null;
    }

    @Override
    public Expression visitNotraceStatement(IntegerParser.NotraceStatementContext ctx) {
        System.out.println("NOTRACE not supported; ignoring it.");
        return null;
    }

    @Override
    public Expression visitTextStatement(IntegerParser.TextStatementContext ctx) {
        callSubroutine("text");
        return null;
    }

    @Override
    public Expression visitVlinStatement(IntegerParser.VlinStatementContext ctx) {
        callSubroutine("vlin", ctx.y0, ctx.y1, ctx.x);
        return null;
    }

    @Override
    public Expression visitVtabStatement(IntegerParser.VtabStatementContext ctx) {
        callSubroutine("vtab", ctx.y);
        return null;
    }

    @Override
    public Expression visitBinaryIntExpr(IntegerParser.BinaryIntExprContext ctx) {
        var left = visit(ctx.left);
        var right = visit(ctx.right);
        var op = ctx.op.getText();
        return new BinaryExpression(left, right, op);
    }

    @Override
    public Expression visitUnaryIntExpr(IntegerParser.UnaryIntExprContext ctx) {
        var op = ctx.op.getText();
        var e = visit(ctx.e);
        return new UnaryExpression(op, e);
    }

    @Override
    public Expression visitFuncExpr(IntegerParser.FuncExprContext ctx) {
        return visit(ctx.func);
    }

    @Override
    public Expression visitIntVarExpr(IntegerParser.IntVarExprContext ctx) {
        return visit(ctx.ref);
    }

    @Override
    public Expression visitIntConstExpr(IntegerParser.IntConstExprContext ctx) {
        if (ctx.value.getText().startsWith("0x")) {
            return new IntegerConstant(Integer.parseInt(ctx.value.getText().substring(2), 16));
        }
        return new IntegerConstant(Integer.parseInt(ctx.value.getText()));
    }

    @Override
    public Expression visitParenExpr(IntegerParser.ParenExprContext ctx) {
        return new ParenthesisExpression(visit(ctx.e));
    }

    @Override
    public Expression visitStrConstExpr(IntegerParser.StrConstExprContext ctx) {
        return new StringConstant(ctx.value.getText().replaceAll("^\"|\"$", ""));
    }

    @Override
    public Expression visitStrVarExpr(IntegerParser.StrVarExprContext ctx) {
        // FIXME
        throw new RuntimeException("string variables not supported yet");
    }

    @Override
    public Expression visitIntArgFunc(IntegerParser.IntArgFuncContext ctx) {
        var f = ctx.f.getText();
        var e = visit(ctx.e);
        return model.callFunction(f, Arrays.asList(e));
    }

    @Override
    public Expression visitStrArgFunc(IntegerParser.StrArgFuncContext ctx) {
        var f = ctx.f.getText();
        var s = visit(ctx.s);
        return model.callFunction(f, Arrays.asList(s));
    }

    @Override
    public Expression visitScrnFunc(IntegerParser.ScrnFuncContext ctx) {
        var x = visit(ctx.x);
        var y = visit(ctx.y);
        return model.callFunction("scrn", Arrays.asList(x, y));
    }

    @Override
    public Expression visitStrRef(IntegerParser.StrRefContext ctx) {
        throw new RuntimeException("strings not supported at this time");
    }

    @Override
    public Expression visitIntAryVar(IntegerParser.IntAryVarContext ctx) {
        throw new RuntimeException("arrays not supported yet");
    }

    @Override
    public Expression visitIntVar(IntegerParser.IntVarContext ctx) {
        var ref = model.addVariable(ctx.n.getText(), DataType.INTEGER);
        return new IdentifierExpression(ref);
    }

    @Override
    public Expression visitStrVar(IntegerParser.StrVarContext ctx) {
        throw new RuntimeException("strings not supported yet");
    }
}
