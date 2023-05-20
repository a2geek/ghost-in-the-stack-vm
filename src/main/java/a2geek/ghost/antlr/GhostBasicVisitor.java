package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.BasicBaseVisitor;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.antlr.generated.BasicParser.IfStatementContext;
import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.statement.DoLoopStatement;
import a2geek.ghost.model.statement.ForNextStatement;
import org.antlr.v4.runtime.tree.ParseTree;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.util.*;
import java.util.stream.Collectors;

public class GhostBasicVisitor extends BasicBaseVisitor<Expression> {
    private ModelBuilder model;

    public GhostBasicVisitor(ModelBuilder model) {
        this.model = model;
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
        if (visit(ctx.id) instanceof VariableReference varRef) {
            Expression expr = visit(ctx.a);
            model.assignStmt(varRef, expr);
            return null;
        }
        else {
            throw new RuntimeException("expecting a variable type for assignment: " + ctx.id.getText());
        }
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
    public Expression visitDoLoop1(BasicParser.DoLoop1Context ctx) {
        Expression test = visit(ctx.a);
        DoLoopStatement.Operation op = switch (ctx.op.getText()) {
            case "while" -> DoLoopStatement.Operation.DO_WHILE;
            case "until" -> DoLoopStatement.Operation.DO_UNTIL;
            default -> throw new RuntimeException("unexpected do loop type: " + ctx.op.getText());
        };

        model.loopBegin(op, test);
        optVisit(ctx.s);
        model.loopEnd();
        return null;
    }

    @Override
    public Expression visitDoLoop2(BasicParser.DoLoop2Context ctx) {
        Expression test = visit(ctx.a);
        DoLoopStatement.Operation op = switch (ctx.op.getText()) {
            case "while" -> DoLoopStatement.Operation.LOOP_WHILE;
            case "until" -> DoLoopStatement.Operation.LOOP_UNTIL;
            default -> throw new RuntimeException("unexpected do loop type: " + ctx.op.getText());
        };

        model.loopBegin(op, test);
        optVisit(ctx.s);
        model.loopEnd();
        return null;
    }

    @Override
    public Expression visitWhileLoop(BasicParser.WhileLoopContext ctx) {
        Expression test = visit(ctx.a);

        model.loopBegin(DoLoopStatement.Operation.WHILE, test);
        optVisit(ctx.s);
        model.loopEnd();
        return null;
    }

    @Override
    public Expression visitRepeatLoop(BasicParser.RepeatLoopContext ctx) {
        Expression test = visit(ctx.a);

        model.loopBegin(DoLoopStatement.Operation.REPEAT, test);
        optVisit(ctx.s);
        model.loopEnd();
        return null;
    }

    @Override
    public Expression visitExitStmt(BasicParser.ExitStmtContext ctx) {
        var op = ctx.n.getText().toLowerCase();
        switch (op) {
            case "for" -> {
                if (model.findBlock(ForNextStatement.class).isPresent()) {
                    model.exitStmt(op);
                    return null;
                }
                throw new RuntimeException("'exit for' must be in a for statement");
            }
            case "while", "loop", "repeat" -> {
                if (model.findBlock(DoLoopStatement.class).isPresent()) {
                    model.exitStmt(op);
                    return null;
                }
                throw new RuntimeException(String.format("'exit %s' must be in a %s statement", op, op));
            }
        }
        throw new RuntimeException(String.format("unknown exit type: " + op));
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

    List<Triplet<String,DataType,List<Expression>>> buildDeclarationList(List<BasicParser.IdDeclContext> params) {
        var decls = new ArrayList<Triplet<String,DataType,List<Expression>>>();
        var names = new HashSet<>();
        if (params != null) {
            params.forEach(idDecl -> {
                String id = model.fixCase(idDecl.ID().getText());
                DataType dt = buildDataType(idDecl.datatype());
                List<Expression> exprs = new ArrayList<>();
                for (var expr : idDecl.expr()) {
                    exprs.add(visit(expr));
                }
                if (exprs.size() > 1) {
                    throw new RuntimeException("only support 1 dimensional arrays at this time: " + id);
                }
                if (names.contains(id)) {
                    throw new RuntimeException("parameter already defined: " + id);
                }
                names.add(id);
                decls.add(Triplet.with(id,dt,exprs));
            });
        }
        return decls;
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
            params = buildDeclarationList(ctx.paramDecl().idDecl()).stream()
                .map(Triplet::removeFrom2)
                .collect(Collectors.toList());
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
            params = buildDeclarationList(ctx.paramDecl().idDecl()).stream()
                .map(Triplet::removeFrom2)
                .collect(Collectors.toList());
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
        buildDeclarationList(ctx.idDecl()).forEach(decl -> {
            if (decl.getValue2() != null && decl.getValue2().size() > 0) {
                var symbol = model.addArrayVariable(decl.getValue0(), decl.getValue1(), decl.getValue2().size());
                // FIXME assuming 1 dimension
                model.addDimArray(symbol, decl.getValue2().get(0));
            }
            else {
                model.addVariable(decl.getValue0(), decl.getValue1());
            }
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
    public Expression visitVariableOrFunctionRef(BasicParser.VariableOrFunctionRefContext ctx) {
        // The ID could possibly be a function call with zero arguments.
        var id = ctx.getText();
        if (model.isFunction(id)) {
            return model.callFunction(id, Collections.emptyList());
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
        return new VariableReference(symbol);
    }

    @Override
    public Expression visitArrayOrFunctionRef(BasicParser.ArrayOrFunctionRefContext ctx) {
        var id = ctx.ID().getText();
        List<Expression> params = new ArrayList<>();
        if (ctx.anyExpr() != null) {
            ctx.anyExpr().stream().map(this::visit).forEach(params::add);
        }

        if (Intrinsic.CPU_REGISTERS.contains(id.toLowerCase())) {
            if (params.size() > 0) {
                throw new RuntimeException("Intrinsic reference takes no arguments: " + id);
            }
            Symbol symbol = model.addVariable(id, Scope.Type.INTRINSIC, DataType.INTEGER);
            return new VariableReference(symbol);
        }

        if (model.isFunction(id)) {
            return model.callFunction(id, params);
        }

        var existing = model.findSymbol(id);
        if (existing.isEmpty()) {
            throw new RuntimeException("variable does not exist: " + id);
        }
        if (params.size() != existing.get().numDimensions()) {
            if (existing.get().numDimensions() == 0) {
                throw new RuntimeException("variable is not declared as an array: " + id);
            }
            else {
                var msg = String.format("variable %s should have %d dimensions: %s", id, existing.get().numDimensions(), ctx.getText());
                throw new RuntimeException(msg);
            }
        }

        return new VariableReference(existing.get(), params);
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
        String value = ctx.s.getText().replaceAll("^\"|\"$", "");
        return new StringConstant(model.fixControlChars(value));
    }

    @Override
    public Expression visitBinaryExpr(BasicParser.BinaryExprContext ctx) {
        Expression l = visit(ctx.a);
        Expression r = visit(ctx.b);
        String op = ctx.op.getText();

        if ("^".equals(op)) {
            return model.callFunction("ipow", Arrays.asList(l,r));
        }
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
