package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.BasicBaseVisitor;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.antlr.generated.BasicParser.IfStatementContext;
import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.tree.ParseTree;
import org.javatuples.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Function;

public class GhostBasicVisitor extends BasicBaseVisitor<Expression> {
    private final String LORES_LIBRARY = "lores";
    private final String PRINT_LIBRARY = "print";
    private final String TEXT_LIBRARY = "text";

    private Function<String,String> caseStrategy;
    private Stack<Scope> scope = new Stack<>();
    private Stack<StatementBlock> statementBlock = new Stack<>();
    private Set<String> librariesIncluded = new HashSet<>();
    private boolean includeLibraries = true;

    public GhostBasicVisitor(Function<String,String> caseStrategy) {
        this.caseStrategy = caseStrategy;
    }

    public void setIncludeLibraries(boolean includeLibraries) {
        this.includeLibraries = includeLibraries;
    }

    public Program getProgram() {
        if (scope.size() == 1 && scope.peek() instanceof Program program) {
            return program;
        }
        throw new RuntimeException("Unexpected scope state at end of evaluation. " +
                "Should be 1 but is " + scope.size());
    }
    Program findProgram() {
        for (Scope s : scope) {
            if (s instanceof Program program) {
                return program;
            }
        }
        throw new RuntimeException("Did not find Program!");
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

    public Reference addVariable(String name, DataType dataType) {
        name = caseStrategy.apply(name);
        return this.scope.peek().addLocalVariable(name, dataType);
    }
    public Reference addVariable(String name, Scope.Type type, DataType dataType) {
        name = caseStrategy.apply(name);
        return this.scope.peek().addLocalVariable(name, type, dataType);
    }

    public void uses(String libname) {
        if (!includeLibraries || librariesIncluded.contains(libname)) {
            return;
        }
        librariesIncluded.add(libname);
        String name = String.format("/library/%s.bas", libname);
        try (InputStream inputStream = getClass().getResourceAsStream(name)) {
            if (inputStream == null) {
                throw new RuntimeException("unknown library: " + libname);
            }
            Program library = GhostBasicUtil.toModel(CharStreams.fromStream(inputStream), caseStrategy, v -> v.setIncludeLibraries(false));
            // at this time a library is simply a collection of subroutines and functions.
            boolean noStatements = library.getStatements().isEmpty();
            boolean onlyConstants = library.getLocalVariables().stream().noneMatch(ref -> ref.type() != Scope.Type.CONSTANT);
            if (!noStatements || !onlyConstants) {
                throw new RuntimeException("a library may only contain subroutines, functions, and constants");
            }
            // add subroutines and functions to our program!
            // constants are intentionally left off -- the included code has the reference and we don't want to clutter the namespace
            Program program = findProgram();
            library.getScopes().forEach(program::addScope);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Expression visitUseDirective(BasicParser.UseDirectiveContext ctx) {
        for (var str : ctx.STR()) {
            String libname = str.getText().replaceAll("^\"|\"$", "");
            uses(libname);
        }
        return null;
    }

    @Override
    public Expression visitAssignment(BasicParser.AssignmentContext ctx) {
        String id = caseStrategy.apply(ctx.id.getText());
        Expression expr = visit(ctx.a);
        Reference ref = switch (id.toLowerCase()) {
            case Intrinsic.CPU_REGISTER_A,
                 Intrinsic.CPU_REGISTER_X,
                 Intrinsic.CPU_REGISTER_Y -> addVariable(id, Scope.Type.INTRINSIC, expr.getType());
            default -> {
                if (id.contains(".")) {
                    throw new RuntimeException("invalid identifier: " + id);
                }
                yield addVariable(id, expr.getType());
            }
        };

        AssignmentStatement assignmentStatement = new AssignmentStatement(ref, expr);
        addStatement(assignmentStatement);
        return null;
    }

    public void callLibrarySubroutine(String library, String name, Expression... params) {
        uses(library);
        var subName = caseStrategy.apply(String.format("%s_%s", library, name));
        CallSubroutine callSubroutine = new CallSubroutine(subName, Arrays.asList(params));
        addStatement(callSubroutine);
    }

    @Override
    public Expression visitGrStmt(BasicParser.GrStmtContext ctx) {
        callLibrarySubroutine(LORES_LIBRARY, "gr");
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
        String id = caseStrategy.apply(ctx.id.getText());
        Reference ref = addVariable(id, DataType.INTEGER);
        Expression start = visit(ctx.a);
        Expression end = visit(ctx.b);
        Expression step = null;

        if (ctx.c == null) {
            step = new IntegerConstant(1);
        }
        else {
            step = visit(ctx.c);
        }

        ForNextStatement forStatement = new ForNextStatement(ref, start, end, step);
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
        callLibrarySubroutine(LORES_LIBRARY, "color", expr);
        return null;
    }

    @Override
    public Expression visitPlotStmt(BasicParser.PlotStmtContext ctx) {
        Expression x = visit(ctx.a);
        Expression y = visit(ctx.b);
        callLibrarySubroutine(LORES_LIBRARY, "plot", x, y);
        return null;
    }

    @Override
    public Expression visitHlinStmt(BasicParser.HlinStmtContext ctx) {
        var a = visit(ctx.a);
        var b = visit(ctx.b);
        var y = visit(ctx.y);
        callLibrarySubroutine(LORES_LIBRARY, "hlin", a, b, y);
        return null;
    }

    @Override
    public Expression visitVlinStmt(BasicParser.VlinStmtContext ctx) {
        var a = visit(ctx.a);
        var b = visit(ctx.b);
        var x = visit(ctx.x);
        callLibrarySubroutine(LORES_LIBRARY, "vlin", a, b, x);
        return null;
    }

    @Override
    public Expression visitEndStmt(BasicParser.EndStmtContext ctx) {
        addStatement(new EndStatement());
        return null;
    }

    @Override
    public Expression visitHomeStmt(BasicParser.HomeStmtContext ctx) {
        callLibrarySubroutine(TEXT_LIBRARY, "home");
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
                callLibrarySubroutine(PRINT_LIBRARY, "comma");
            }
            else {
                Expression expr = pt.accept(this);
                switch (expr.getType()) {
                    case INTEGER -> callLibrarySubroutine(PRINT_LIBRARY, "integer", expr);
                    case BOOLEAN -> callLibrarySubroutine(PRINT_LIBRARY, "boolean", expr);
                    case STRING -> callLibrarySubroutine(PRINT_LIBRARY, "string", expr);
                    default -> throw new RuntimeException("Unsupported PRINT type: " + expr.getType());
                }
            }
        }
        if (!semiColonAtEnd) {
            callLibrarySubroutine(PRINT_LIBRARY, "newline");
        }
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
        String id = caseStrategy.apply(ctx.id.getText());
        addStatement(new LabelStatement(id));
        return null;
    }

    @Override
    public Expression visitGotoGosubStmt(BasicParser.GotoGosubStmtContext ctx) {
        String label = caseStrategy.apply(ctx.l.getText());
        addStatement(new GotoGosubStatement(ctx.op.getText().toLowerCase(), label));
        return null;
    }

    @Override
    public Expression visitReturnStmt(BasicParser.ReturnStmtContext ctx) {
        if (ctx.e != null) {
            var expr = visit(ctx.e);
            addStatement(new ReturnStatement(expr));
        }
        else {
            addStatement(new ReturnStatement());
        }
        return null;
    }

    @Override
    public Expression visitTextStmt(BasicParser.TextStmtContext ctx) {
        callLibrarySubroutine(TEXT_LIBRARY, "text");
        return null;
    }

    @Override
    public Expression visitHtabStmt(BasicParser.HtabStmtContext ctx) {
        var a = visit(ctx.a);
        callLibrarySubroutine(TEXT_LIBRARY, "htab", a);
        return null;
    }

    @Override
    public Expression visitVtabStmt(BasicParser.VtabStmtContext ctx) {
        var a = visit(ctx.a);
        callLibrarySubroutine(TEXT_LIBRARY, "vtab", a);
        return null;
    }

    List<Pair<String,DataType>> buildDeclarationList(List<BasicParser.IdDeclContext> params) {
        var refs = new ArrayList<Pair<String,DataType>>();
        var names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (params != null) {
            params.forEach(idDecl -> {
                String id = caseStrategy.apply(idDecl.ID().getText());
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
        String name = caseStrategy.apply(ctx.id.getText());
        List<Pair<String,DataType>> params = Collections.emptyList();
        if (ctx.paramDecl() != null) {
            params = buildDeclarationList(ctx.paramDecl().idDecl());
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
    public Expression visitFuncDecl(BasicParser.FuncDeclContext ctx) {
        String name = caseStrategy.apply(ctx.id.getText());
        List<Pair<String,DataType>> params = Collections.emptyList();
        if (ctx.paramDecl() != null) {
            params = buildDeclarationList(ctx.paramDecl().idDecl());
        }
        DataType dt = buildDataType(ctx.datatype());

        // FIXME? naming is really awkward due to naming conflicts!
        a2geek.ghost.model.scope.Function func =
                new a2geek.ghost.model.scope.Function(scope.peek(), Pair.with(name,dt), params);
        addScope(func);

        pushScope(func);
        pushStatementBlock(func);
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
            ctx.parameters().anyExpr().stream().map(this::visit).forEach(params::add);
        }

        CallSubroutine callSubroutine = new CallSubroutine(name, params);
        addStatement(callSubroutine);

        return null;
    }

    @Override
    public Expression visitDimStmt(BasicParser.DimStmtContext ctx) {
        buildDeclarationList(ctx.idDecl()).forEach(d -> {
           addVariable(d.getValue0(), d.getValue1());
        });
        return null;
    }

    @Override
    public Expression visitConstant(BasicParser.ConstantContext ctx) {
        if (ctx.constantDecl() != null) {
            for (var decl : ctx.constantDecl()) {
                var id = caseStrategy.apply(decl.id.getText());
                var e = visit(decl.e);
                if (e.isConstant()) {
                    this.scope.peek().addLocalConstant(id, e);
                }
                else {
                    String msg = String.format("'%s' is not a constant: %s", id, e);
                    throw new RuntimeException(msg);
                }
            }
        }
        return null;
    }

    @Override
    public Expression visitIdentifier(BasicParser.IdentifierContext ctx) {
        // The ID could possibly be a function call with zero arguments.
        String id = caseStrategy.apply(ctx.id.getText());
        Optional<Scope> scope = findProgram().findScope(id);
        if (scope.isPresent()) {
            if (scope.get() instanceof a2geek.ghost.model.scope.Function fn) {
                return new FunctionExpression(fn, Collections.emptyList());
            }
            else {
                throw new RuntimeException("Expecting a function named " + id);
            }
        }

        // Look for a likely reference - and it might already exist
        Reference ref = switch (id.toLowerCase()) {
            case Intrinsic.CPU_REGISTER_A,
                    Intrinsic.CPU_REGISTER_X,
                    Intrinsic.CPU_REGISTER_Y -> addVariable(id, Scope.Type.INTRINSIC, DataType.INTEGER);
            default -> {
                if (id.contains(".")) {
                    throw new RuntimeException("invalid identifier: " + id);
                }
                // Look through local and parent scopes; otherwise assume it's a new integer
                var existing = this.scope.peek().findVariable(id);
                yield existing.orElseGet(() -> addVariable(id, DataType.INTEGER));
            }
        };
        return new IdentifierExpression(ref);
    }

    @Override
    public Expression visitFuncExpr(BasicParser.FuncExprContext ctx) {
        String id = caseStrategy.apply(ctx.id.getText());
        List<Expression> params = new ArrayList<>();
        if (ctx.p != null) {
            ctx.parameters().anyExpr().stream().map(this::visit).forEach(params::add);
        }

        if (Intrinsic.CPU_REGISTERS.contains(id.toLowerCase())) {
            if (params.size() > 0) {
                throw new RuntimeException("Intrinsic reference takes no arguments: " + id);
            }
            Reference ref = addVariable(id, Scope.Type.INTRINSIC, DataType.INTEGER);
            return new IdentifierExpression(ref);
        }

        if (FunctionExpression.isLibraryFunction(id)) {
            FunctionExpression.Descriptor descriptor = FunctionExpression.getDescriptor(id).orElseThrow();
            uses(descriptor.library());
            id = caseStrategy.apply(String.format("%s_%s", descriptor.library(), id));
        }

        Optional<Scope> scope = findProgram().findScope(id);
        if (scope.isPresent()) {
            if (scope.get() instanceof a2geek.ghost.model.scope.Function fn) {
                return new FunctionExpression(fn, params);
            }
        }
        else if (FunctionExpression.isIntrinsicFunction(id)) {
            return new FunctionExpression(id, params);
        }

        throw new RuntimeException("Expecting a function named " + id);
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
