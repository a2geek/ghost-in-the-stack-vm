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
    private Stack<StatementBlock> blocks = new Stack<>();
    private Set<String> alreadyIncluded = new TreeSet<>();
    private Map<Reference,ForFrame> forFrames = new HashMap<>();

    @Override
    public Expression visitProgram(IntegerParser.ProgramContext ctx) {
        blocks.push(new Program(String::toUpperCase));
        super.visitProgram(ctx);
        if (!blocks.peek().isLastStatement(EndStatement.class)) {
            blocks.peek().addStatement(new EndStatement());
        }
        return null;
    }

    public Program getProgram() {
        if (blocks.size() == 1 && blocks.peek() instanceof Program program) {
            return program;
        }
        throw new RuntimeException("Unexpected scope state at end of evaluation. " +
                "Should be 1 but is " + blocks.size());
    }

    public ForFrame forFrame(Reference ref) {
        return forFrames.computeIfAbsent(ref, r -> new ForFrame(r, currentScope()));
    }

    Scope currentScope() {
        for (int i=blocks.size()-1; i>=0; i--) {
            if (blocks.get(i) instanceof Scope s) {
                return s;
            }
        }
        throw new RuntimeException("scope not found");
    }

    void uses(String libraryName) {
        if (alreadyIncluded.contains(libraryName)) {
            return;
        }

        String name = String.format("/library/%s.bas", libraryName);
        try (InputStream inputStream = getClass().getResourceAsStream(name)) {
            if (inputStream == null) {
                throw new RuntimeException("unknown library: " + libraryName);
            }
            Program library = ParseUtil.basicToModel(CharStreams.fromStream(inputStream),
                    String::toUpperCase, v -> v.getModel().setIncludeLibraries(false));
            // at this time a library is simply a collection of subroutines and functions.
            boolean noStatements = library.getStatements().isEmpty();
            boolean onlyConstants = library.getLocalReferences().stream().noneMatch(ref -> ref.type() != Scope.Type.CONSTANT);
            if (!noStatements || !onlyConstants) {
                throw new RuntimeException("a library may only contain subroutines, functions, and constants");
            }
            // add subroutines and functions to our program!
            // constants are intentionally left off -- the included code has the reference and we don't want to clutter the namespace
            alreadyIncluded.add(libraryName);
            library.getScopes().forEach(s -> {
                currentScope().addScope(s);
            });
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void callSubroutine(String subName, ParseTree... expressions) {
        var descriptor = CallSubroutine.getDescriptor(subName).orElseThrow();
        uses(descriptor.library());
        var exprs = new ArrayList<Expression>();
        for (var expression : expressions) {
            exprs.add(visit(expression));
        }
        var name = descriptor.fullName().toUpperCase();
        blocks.peek().addStatement(new CallSubroutine(name, exprs));
    }

    void gotoGosub(String command, Token token) {
        var linenum = Integer.parseInt(token.getText());
        String label = String.format("L%d", linenum);
        blocks.peek().addStatement(new GotoGosubStatement(command, label));
    }

    @Override
    public Expression visitProgramLine(IntegerParser.ProgramLineContext ctx) {
        blocks.peek().addStatement(new LabelStatement("L" + ctx.INTEGER().getText()));
        visit(ctx.statements());
        return null;
    }

    @Override
    public Expression visitCallStatement(IntegerParser.CallStatementContext ctx) {
        var expr = visit(ctx.e);
        blocks.peek().addStatement(new CallStatement(expr));
        return null;
    }

    @Override
    public Expression visitClrStatement(IntegerParser.ClrStatementContext ctx) {
        System.out.println("CLR not supported; ignoring it.");
        return null;
    }

    @Override
    public Expression visitColorStatement(IntegerParser.ColorStatementContext ctx) {
        callSubroutine("color", ctx.e);
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
        blocks.peek().addStatement(new EndStatement());
        return null;
    }

    Expression visitOrDefault(ParseTree tree, Expression defaultExpression) {
        return tree != null ? visit(tree) : defaultExpression;
    }

    @Override
    public Expression visitForStatement(IntegerParser.ForStatementContext ctx) {
        var ref = currentScope().addLocalVariable(ctx.ivar().getText(), DataType.INTEGER);
        var first = visit(ctx.first);
        var last = visit(ctx.last);
        Expression step = visitOrDefault(ctx.step, IntegerConstant.ONE);
        ForFrame frame = forFrame(ref);

        blocks.peek().addStatement(new ForStatement(ref, first, last, step, frame));
        return null;
    }

    @Override
    public Expression visitNextStatement(IntegerParser.NextStatementContext ctx) {
        for (var ivar : ctx.ivar()) {
            var ref = currentScope().addLocalVariable(ivar.getText(), DataType.INTEGER);
            ForFrame frame = forFrame(ref);
            blocks.peek().addStatement(new NextStatement(ref, frame));
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
        StatementBlock block = new StatementBlock();
        blocks.push(block);
        gotoGosub("GOTO", ctx.l);
        blocks.pop();

        blocks.peek().addStatement(new IfStatement(expr, block, null));
        return null;
    }

    @Override
    public Expression visitIfStatement(IntegerParser.IfStatementContext ctx) {
        var expr = visit(ctx.e);
        StatementBlock block = new StatementBlock();
        blocks.push(block);
        visit(ctx.s);
        blocks.pop();

        blocks.peek().addStatement(new IfStatement(expr, block, null));
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
        var ref = currentScope().addLocalVariable(ctx.ivar().getText(), DataType.INTEGER);
        blocks.peek().addStatement(new AssignmentStatement(ref, expr));
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
        blocks.peek().addStatement(new PokeStatement(addr, e));
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
        blocks.peek().addStatement(new ReturnStatement());
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
        var libraryName = Optional.ofNullable(switch (f) {
            case "abs", "rnd", "sgn" -> "math";
            case "pdl" -> "misc";
            default -> null;
        });
        libraryName.ifPresent(this::uses);
        return new FunctionExpression(f, Arrays.asList(e));
    }

    @Override
    public Expression visitStrArgFunc(IntegerParser.StrArgFuncContext ctx) {
        var f = ctx.f.getText();
        var s = visit(ctx.s);
        return new FunctionExpression(f, Arrays.asList(s));
    }

    @Override
    public Expression visitScrnFunc(IntegerParser.ScrnFuncContext ctx) {
        var x = visit(ctx.x);
        var y = visit(ctx.y);
        return new FunctionExpression("scrn", Arrays.asList(x, y));
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
        var ref = currentScope().addLocalVariable(ctx.n.getText(), DataType.INTEGER);
        return new IdentifierExpression(ref);
    }

    @Override
    public Expression visitStrVar(IntegerParser.StrVarContext ctx) {
        throw new RuntimeException("strings not supported yet");
    }
}
