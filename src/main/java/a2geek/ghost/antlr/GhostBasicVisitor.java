package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.BasicBaseVisitor;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.antlr.generated.BasicParser.IfStatementContext;
import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.javatuples.Pair;

import java.util.*;
import java.util.function.Function;

public class GhostBasicVisitor extends BasicBaseVisitor<Expression> {
    private ModelBuilder model;

    public GhostBasicVisitor(Function<String,String> caseStrategy) {
        this.model = new ModelBuilder(caseStrategy);
    }

    public ModelBuilder getModel() {
        return model;
    }

    public Optional<Expression> optVisit(ParseTree pt) {
        return Optional.ofNullable(pt).map(this::visit);
    }

    @Override
    public Expression visitUseDirective(BasicParser.UseDirectiveContext ctx) {
        for (var str : ctx.STR()) {
            String libname = str.getText().replaceAll("^\"|\"$", "");
            model.uses(libname);
        }
        return null;
    }

    @Override
    public Expression visitAssignment(BasicParser.AssignmentContext ctx) {
        String id = ctx.id.getText();
        Expression expr = visit(ctx.a);
        Symbol symbol = switch (id.toLowerCase()) {
            case Intrinsic.CPU_REGISTER_A,
                 Intrinsic.CPU_REGISTER_X,
                 Intrinsic.CPU_REGISTER_Y -> model.addVariable(id, Scope.Type.INTRINSIC, expr.getType());
            default -> {
                if (id.contains(".")) {
                    throw new RuntimeException("invalid identifier: " + id);
                }
                yield model.addVariable(id, expr.getType());
            }
        };

        model.assignStmt(symbol, expr);
        return null;
    }

    @Override
    public Expression visitGrStmt(BasicParser.GrStmtContext ctx) {
        model.callLibrarySubroutine("gr");
        return null;
    }

    @Override
    public Expression visitIfShortStatement(BasicParser.IfShortStatementContext ctx) {
        Expression expr = visit(ctx.a);

        StatementBlock trueStatements = model.pushStatementBlock(new StatementBlock());
        visit(ctx.t);
        model.popStatementBlock();

        model.ifStmt(expr, trueStatements, null);
        return null;
    }

    @Override
    public Expression visitIfStatement(IfStatementContext ctx) {
        Expression expr = visit(ctx.a);

        StatementBlock trueStatements = model.pushStatementBlock(new StatementBlock());
        visit(ctx.t);
        model.popStatementBlock();

        StatementBlock falseStatements = null;
        if (ctx.f != null) {
            falseStatements = model.pushStatementBlock(new StatementBlock());
            visit(ctx.f);
            model.popStatementBlock();
        }

        model.ifStmt(expr, trueStatements, falseStatements);
        return null;
    }

    @Override
    public Expression visitForLoop(BasicParser.ForLoopContext ctx) {
        Symbol symbol = model.addVariable(ctx.id.getText(), DataType.INTEGER);
        Expression start = visit(ctx.a);
        Expression end = visit(ctx.b);
        Expression step = optVisit(ctx.c).orElse(new IntegerConstant(1));

        model.forBegin(symbol, start, end, step);
        optVisit(ctx.s);
        model.forEnd();
        return null;
    }

    @Override
    public Expression visitColorStmt(BasicParser.ColorStmtContext ctx) {
        Expression expr = visit(ctx.a);
        model.callLibrarySubroutine("color", expr);
        return null;
    }

    @Override
    public Expression visitPlotStmt(BasicParser.PlotStmtContext ctx) {
        Expression x = visit(ctx.a);
        Expression y = visit(ctx.b);
        model.callLibrarySubroutine("plot", x, y);
        return null;
    }

    @Override
    public Expression visitHlinStmt(BasicParser.HlinStmtContext ctx) {
        var a = visit(ctx.a);
        var b = visit(ctx.b);
        var y = visit(ctx.y);
        model.callLibrarySubroutine("hlin", a, b, y);
        return null;
    }

    @Override
    public Expression visitVlinStmt(BasicParser.VlinStmtContext ctx) {
        var a = visit(ctx.a);
        var b = visit(ctx.b);
        var x = visit(ctx.x);
        model.callLibrarySubroutine("vlin", a, b, x);
        return null;
    }

    @Override
    public Expression visitEndStmt(BasicParser.EndStmtContext ctx) {
        model.endStmt();
        return null;
    }

    @Override
    public Expression visitHomeStmt(BasicParser.HomeStmtContext ctx) {
        model.callLibrarySubroutine("home");
        return null;
    }

    @Override
    public Expression visitPrintStmt(BasicParser.PrintStmtContext ctx) {
        // Print is a little bit different in that we need to pay attention to syntax,
        // so this tortured code handles that.
        // Note that PRINT devolves into a bunch of subroutine calls as well.
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
                model.callLibrarySubroutine("comma");
            }
            else {
                Expression expr = pt.accept(this);
                switch (expr.getType()) {
                    case INTEGER -> model.callLibrarySubroutine("integer", expr);
                    case BOOLEAN -> model.callLibrarySubroutine("boolean", expr);
                    case STRING -> model.callLibrarySubroutine("string", expr);
                    default -> throw new RuntimeException("Unsupported PRINT type: " + expr.getType());
                }
            }
        }
        if (!semiColonAtEnd) {
            model.callLibrarySubroutine("newline");
        }
        return null;
    }

    public Expression visitCallStmt(BasicParser.CallStmtContext ctx) {
        Expression expr = visit(ctx.a);
        model.callAddr(expr);
        return null;
    }

    @Override
    public Expression visitPokeStmt(BasicParser.PokeStmtContext ctx) {
        var a = visit(ctx.a);
        var b = visit(ctx.b);
        model.pokeStmt(ctx.op.getText(), a, b);
        return null;
    }

    @Override
    public Expression visitLabel(BasicParser.LabelContext ctx) {
        model.labelStmt(ctx.id.getText());
        return null;
    }

    @Override
    public Expression visitGotoGosubStmt(BasicParser.GotoGosubStmtContext ctx) {
        model.gotoGosubStmt(ctx.op.getText(), ctx.l.getText());
        return null;
    }

    @Override
    public Expression visitReturnStmt(BasicParser.ReturnStmtContext ctx) {
        if (ctx.e != null) {
            var expr = visit(ctx.e);
            model.returnStmt(expr);
        }
        else {
            model.returnStmt(null);
        }
        return null;
    }

    @Override
    public Expression visitTextStmt(BasicParser.TextStmtContext ctx) {
        model.callLibrarySubroutine("text");
        return null;
    }

    @Override
    public Expression visitHtabStmt(BasicParser.HtabStmtContext ctx) {
        var a = visit(ctx.a);
        model.callLibrarySubroutine("htab", a);
        return null;
    }

    @Override
    public Expression visitVtabStmt(BasicParser.VtabStmtContext ctx) {
        var a = visit(ctx.a);
        model.callLibrarySubroutine("vtab", a);
        return null;
    }

    List<Pair<String,DataType>> buildDeclarationList(List<BasicParser.IdDeclContext> params) {
        var refs = new ArrayList<Pair<String,DataType>>();
        var names = new HashSet<>();
        if (params != null) {
            params.forEach(idDecl -> {
                String id = model.fixCase(idDecl.ID().getText());
                DataType dt = buildDataType(idDecl.datatype());
                if (names.contains(id)) {
                    throw new RuntimeException("parameter already defined: " + id);
                }
                names.add(id);
                refs.add(Pair.with(id,dt));
            });
        }
        return refs;
    }
    DataType buildDataType(BasicParser.DatatypeContext ctx) {
        DataType dt = DataType.INTEGER;
        if (ctx != null) {
            dt = DataType.valueOf(ctx.getText().toUpperCase());
        }
        return dt;
    }

    @Override
    public Expression visitSubDecl(BasicParser.SubDeclContext ctx) {
        List<Pair<String,DataType>> params = Collections.emptyList();
        if (ctx.paramDecl() != null) {
            params = buildDeclarationList(ctx.paramDecl().idDecl());
        }

        model.subDeclBegin(ctx.id.getText(), params);
        visit(ctx.s);
        model.subDeclEnd();
        return null;
    }

    @Override
    public Expression visitFuncDecl(BasicParser.FuncDeclContext ctx) {
        List<Pair<String,DataType>> params = Collections.emptyList();
        if (ctx.paramDecl() != null) {
            params = buildDeclarationList(ctx.paramDecl().idDecl());
        }
        DataType dt = buildDataType(ctx.datatype());

        model.funcDeclBegin(ctx.id.getText(), dt, params);
        visit(ctx.s);
        model.funcDeclEnd();
        return null;
    }

    @Override
    public Expression visitCallSub(BasicParser.CallSubContext ctx) {
        List<Expression> params = new ArrayList<>();
        if (ctx.p != null) {
            ctx.parameters().anyExpr().stream().map(this::visit).forEach(params::add);
        }
        model.callSubroutine(ctx.id.getText(), params);
        return null;
    }

    @Override
    public Expression visitDimStmt(BasicParser.DimStmtContext ctx) {
        buildDeclarationList(ctx.idDecl()).forEach(d -> {
           model.addVariable(d.getValue0(), d.getValue1());
        });
        return null;
    }

    @Override
    public Expression visitConstant(BasicParser.ConstantContext ctx) {
        for (var decl : ctx.constantDecl()) {
            var e = visit(decl.e);
            if (e.isConstant()) {
                model.addConstant(decl.id.getText(), e);
            }
            else {
                String msg = String.format("'%s' is not a constant: %s", decl.id.getText(), e);
                throw new RuntimeException(msg);
            }
        }
        return null;
    }

    @Override
    public Expression visitIdentifier(BasicParser.IdentifierContext ctx) {
        // The ID could possibly be a function call with zero arguments.
        var id = ctx.id.getText();
        Optional<Scope> scope = model.findScope(id);
        if (scope.isPresent()) {
            if (scope.get() instanceof a2geek.ghost.model.scope.Function fn) {
                return new FunctionExpression(fn, Collections.emptyList());
            }
            else {
                throw new RuntimeException("Expecting a function named " + id);
            }
        }

        // Look for a likely symbol - and it might already exist
        Symbol symbol = switch (id.toLowerCase()) {
            case Intrinsic.CPU_REGISTER_A,
                    Intrinsic.CPU_REGISTER_X,
                    Intrinsic.CPU_REGISTER_Y -> model.addVariable(id, Scope.Type.INTRINSIC, DataType.INTEGER);
            default -> {
                if (id.contains(".")) {
                    throw new RuntimeException("invalid identifier: " + id);
                }
                // Look through local and parent scopes; otherwise assume it's a new integer
                var existing = model.findSymbol(id);
                yield existing.orElseGet(() -> model.addVariable(id, DataType.INTEGER));
            }
        };
        return new IdentifierExpression(symbol);
    }

    @Override
    public Expression visitFuncExpr(BasicParser.FuncExprContext ctx) {
        String id = ctx.id.getText();
        List<Expression> params = new ArrayList<>();
        if (ctx.p != null) {
            ctx.parameters().anyExpr().stream().map(this::visit).forEach(params::add);
        }

        if (Intrinsic.CPU_REGISTERS.contains(id.toLowerCase())) {
            if (params.size() > 0) {
                throw new RuntimeException("Intrinsic reference takes no arguments: " + id);
            }
            Symbol symbol = model.addVariable(id, Scope.Type.INTRINSIC, DataType.INTEGER);
            return new IdentifierExpression(symbol);
        }
        return model.callFunction(id, params);
    }

    @Override
    public Expression visitIntConstant(BasicParser.IntConstantContext ctx) {
        var a = ctx.a.getText();
        if (a.startsWith("0x")) {
            return new IntegerConstant(Integer.parseInt(a.substring(2), 16));
        }
        else if (a.startsWith("0b")) {
            return new IntegerConstant(Integer.parseInt(a.substring(2), 2));
        }
        return new IntegerConstant(Integer.parseInt(ctx.a.getText()));
    }

    @Override
    public Expression visitStringConstant(BasicParser.StringConstantContext ctx) {
        return new StringConstant(ctx.s.getText().replaceAll("^\"|\"$", ""));
    }

    @Override
    public Expression visitBinaryExpr(BasicParser.BinaryExprContext ctx) {
        Expression l = visit(ctx.a);
        Expression r = visit(ctx.b);
        String op = ctx.op.getText();
        return new BinaryExpression(l, r, op);
    }

    @Override
    public Expression visitBoolConstant(BasicParser.BoolConstantContext ctx) {
        return new BooleanConstant(Boolean.parseBoolean(ctx.b.getText()));
    }

    @Override
    public Expression visitParenExpr(BasicParser.ParenExprContext ctx) {
        Expression e = visit(ctx.a);
        return new ParenthesisExpression(e);
    }

    @Override
    public Expression visitUnaryExpr(BasicParser.UnaryExprContext ctx) {
        Expression e = visit(ctx.a);
        String op = ctx.op.getText();
        return new UnaryExpression(op, e);
    }
}
