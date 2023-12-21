package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.BasicBaseVisitor;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.antlr.generated.BasicParser.IfStatementContext;
import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.statement.GotoGosubStatement;
import a2geek.ghost.model.statement.IfStatement;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.stream.Collectors;

public class GhostBasicVisitor extends BasicBaseVisitor<Expression> {
    private ModelBuilder model;
    private Map<String,Symbol> gotoGosubLabels = new HashMap<>();
    private Stack<Symbol> forExitLabels = new Stack<>();
    private Stack<Symbol> doExitLabels = new Stack<>();
    private Stack<Symbol> repeatExitLabels = new Stack<>();
    private Stack<Symbol> whileExitLabels = new Stack<>();

    public GhostBasicVisitor(ModelBuilder model) {
        this.model = model;
        Intrinsic.CPU_REGISTERS.forEach(name -> {
            model.addVariable(name, SymbolType.INTRINSIC, DataType.INTEGER);
        });
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
        StatementBlock falseStatements = null;
        if (ctx.f != null) {
            falseStatements = model.pushStatementBlock(new StatementBlock());
            visit(ctx.f);
            model.popStatementBlock();
        }

        int i = ctx.ifFragment().size() - 1;
        while (i > 0) {
            var fragment = ctx.ifFragment(i);
            model.pushStatementBlock(new StatementBlock());
            visit(fragment.statements());
            var sb = model.popStatementBlock();

            model.pushStatementBlock(new StatementBlock());
            var expr = visit(fragment.expr());
            model.ifStmt(expr, sb, falseStatements);
            falseStatements = model.popStatementBlock();

            i-= 1;
        }

        Expression expr = visit(ctx.ifFragment(0).expr());
        model.pushStatementBlock(new StatementBlock());
        visit(ctx.ifFragment(0).statements());
        StatementBlock sb = model.popStatementBlock();

        model.ifStmt(expr, sb, falseStatements);
        return null;
    }

    /**
     * Build a FOR ... NEXT loop. Note that the initial code is complicated by the possibility
     * of the step being negative (meaning different tests for the loop portion).  If the step is
     * a constant (or default of 1), this collapses into the simple condition during dead code
     * optimization.
     * <p>
     * Sample FOR loop:
     * <pre>
     * FOR X = 1 TO 10 [ STEP 1 ]
     *     ' code
     *     [EXIT FOR]
     *     ' code
     * NEXT X
     * </pre>
     * <p>
     * Target intermediate pseudocode; "(name)" is a label:
     * <pre>
     * X = START
     * (LOOP)
     * IF SGN(STEP) >= 0 THEN  ' positive increment/zero
     *     IF X <= END THEN
     *         ...STATEMENTS...
     *         ...EXIT FOR == GOTO (EXIT)
     *         GOTO (LOOP)
     *     END IF
     * ELSE                   ' decrement
     *     IF X >= END THEN
     *         ...STATEMENTS...
     *         ...EXIT FOR == GOTO (EXIT)
     *         X = X + STEP
     *         GOTO (LOOP)
     *     END IF
     * END IF
     * (EXIT)
     * </pre>
     */
    @Override
    public Expression visitForLoop(BasicParser.ForLoopContext ctx) {
        Symbol symbol = model.addVariable(ctx.id.getText(), DataType.INTEGER);
        Expression start = visit(ctx.a);
        Expression end = visit(ctx.b);
        Expression step = optVisit(ctx.c).orElse(new IntegerConstant(1));

        var ref = new VariableReference(symbol);
        var labels = model.addLabels("FOR_LOOP", "FOR_EXIT");
        var loopLabel = labels.get(0);
        var exitLabel = labels.get(1);

        // building contents of "IF" statement
        model.pushStatementBlock(new StatementBlock());
        forExitLabels.push(exitLabel);
        optVisit(ctx.s);
        forExitLabels.pop();
        model.assignStmt(ref, new BinaryExpression(ref, step, "+"));
        model.gotoGosubStmt("goto", loopLabel);
        var sb = model.popStatementBlock();
        var positive = new IfStatement(new BinaryExpression(ref, end, "<="), sb, null);
        var negative = new IfStatement(new BinaryExpression(ref, end, ">="), sb, null);

        // generating code
        model.assignStmt(ref, start);
        model.labelStmt(loopLabel);
        model.ifStmt(new BinaryExpression(model.callFunction("SGN", Arrays.asList(step)), IntegerConstant.ZERO, ">="),
                StatementBlock.with(positive), StatementBlock.with(negative));
        model.labelStmt(exitLabel);
        return null;
    }

    /**
     * Build a DO [ WHILE | UNTIL ] ... LOOP statement, which tests at beginning of statement.
     * <p>
     * Sample code:
     * <pre>
     * DO [ WHILE | UNTIL ] (condition)
     *     ' statements
     *     [ EXIT DO ]
     *     ' more statements
     * LOOP
     * </pre>
     * <p>
     * Generated code for DO WHILE ... LOOP will be of the form:
     * <pre>
     * (LOOP)
     * IF condition THEN
     *     ' DO NOTHING
     * ELSE
     *     GOTO (EXIT)
     * END IF
     * ' statements
     * [ EXIT DO => GOTO (EXIT) ]
     * ' more statements
     * GOTO (LOOP)
     * (EXIT)
     * </pre>
     * <p>
     * With DO UNTIL ... LOOP, the first IF will be:
     * <pre>
     * IF condition THEN
     *     GOTO (EXIT)
     * END IF
     * </pre>
     */
    @Override
    public Expression visitDoLoop1(BasicParser.DoLoop1Context ctx) {
        Expression test = visit(ctx.a);

        var labels = model.addLabels("DO_LOOP", "DO_EXIT");
        var loopLabel = labels.get(0);
        var exitLabel = labels.get(1);
        var gotoExitStatement = StatementBlock.with(new GotoGosubStatement("goto", exitLabel));

        model.labelStmt(loopLabel);
        switch (ctx.op.getText()) {
            case "while" -> model.ifStmt(test, StatementBlock.EMPTY, gotoExitStatement);
            case "until" -> model.ifStmt(test, gotoExitStatement, null);
            default -> throw new RuntimeException("unexpected do loop type: " + ctx.op.getText());
        };
        doExitLabels.push(exitLabel);
        optVisit(ctx.s);
        doExitLabels.pop();
        model.gotoGosubStmt("goto", loopLabel);
        model.labelStmt(exitLabel);

        return null;
    }

    /**
     * Build a DO ... LOOP [ WHILE | UNTIL ] statement, which tests at end of statement.
     * <p>
     * Sample code:
     * <pre>
     * DO
     *     ' statements
     *     [ EXIT DO ]
     *     ' more statements
     * LOOP [ WHILE | UNTIL ] (condition)
     * </pre>
     * <p>
     * Generated code for DO ... LOOP WHILE will be of the form:
     * <pre>
     * (LOOP)
     * ' statements
     * [ EXIT DO => GOTO (EXIT) ]
     * ' more statements
     * IF condition THEN
     *     GOTO (LOOP)
     * END IF
     * (EXIT)
     * </pre>
     * <p>
     * With DO ... LOOP UNTIL, the final IF will be:
     * <pre>
     * IF condition THEN
     *     ' DO NOTHING
     * ELSE
     *     GOTO (LOOP)
     * END IF
     * </pre>
     */
    @Override
    public Expression visitDoLoop2(BasicParser.DoLoop2Context ctx) {
        Expression test = visit(ctx.a);

        var labels = model.addLabels("DO_LOOP", "DO_EXIT");
        var loopLabel = labels.get(0);
        var exitLabel = labels.get(1);
        var gotoStatement = StatementBlock.with(new GotoGosubStatement("goto", loopLabel));

        model.labelStmt(loopLabel);
        doExitLabels.push(exitLabel);
        optVisit(ctx.s);
        doExitLabels.pop();
        switch (ctx.op.getText()) {
            case "while" -> model.ifStmt(test, gotoStatement, null);
            case "until" -> model.ifStmt(test, StatementBlock.EMPTY, gotoStatement);
            default -> throw new RuntimeException("unexpected do loop type: " + ctx.op.getText());
        };
        model.labelStmt(exitLabel);

        return null;
    }

    /**
     * Build a WHILE ... LOOP statement.
     * <p>
     * Sample code:
     * <pre>
     * WHILE (condition)
     *     ' statements
     *     [ EXIT WHILE ]
     *     ' more statements
     * LOOP
     * </pre>
     * <p>
     * Generated code will be of the form:
     * <pre>
     * (LOOP)
     * IF condition THEN
     *     ' statements
     *     [ EXIT WHILE => GOTO (EXIT) ]
     *     ' more statements
     *     GOTO (LOOP)
     * END IF
     * (EXIT)
     * </pre>
     */
    @Override
    public Expression visitWhileLoop(BasicParser.WhileLoopContext ctx) {
        Expression test = visit(ctx.a);

        var labels = model.addLabels("WHILE_LOOP", "WHILE_EXIT");
        var loopLabel = labels.get(0);
        var exitLabel = labels.get(1);

        model.pushStatementBlock(new StatementBlock());
        whileExitLabels.push(exitLabel);
        optVisit(ctx.s);
        whileExitLabels.pop();
        model.gotoGosubStmt("goto", loopLabel);
        var loopStatements = model.popStatementBlock();

        model.labelStmt(loopLabel);
        model.ifStmt(test, loopStatements, null);
        model.labelStmt(exitLabel);

        return null;
    }

    /**
     * Build a REPEAT ... UNTIL statement.
     * <p>
     * Sample code:
     * <pre>
     * REPEAT
     *     ' statements
     *     [ EXIT REPEAT ]
     *     ' more statements
     * UNTIL (condition)
     * </pre>
     * <p>
     * Generated code will be of the form:
     * <pre>
     * (LOOP)
     * IF condition THEN
     *     ' DO NOTHING; EXIT LOOP
     * ELSE
     *     ' statements
     *     [ EXIT REPEAT => GOTO (EXIT) ]
     *     ' more statements
     *     GOTO (LOOP)
     * END IF
     * (EXIT)
     * </pre>
     */
    @Override
    public Expression visitRepeatLoop(BasicParser.RepeatLoopContext ctx) {
        Expression test = visit(ctx.a);

        var labels = model.addLabels("REPEAT_LOOP", "REPEAT_EXIT");
        var loopLabel = labels.get(0);
        var exitLabel = labels.get(1);

        model.labelStmt(loopLabel);
        repeatExitLabels.push(exitLabel);
        optVisit(ctx.s);
        repeatExitLabels.pop();
        model.ifStmt(test, StatementBlock.EMPTY, StatementBlock.with(new GotoGosubStatement("goto", loopLabel)));
        model.labelStmt(exitLabel);

        return null;
    }

    @Override
    public Expression visitExitStmt(BasicParser.ExitStmtContext ctx) {
        var op = ctx.n.getText().toLowerCase();
        var labels = switch (op) {
            case "for" -> forExitLabels;
            case "while" -> whileExitLabels;
            case "repeat" -> repeatExitLabels;
            case "do" -> doExitLabels;
            default -> throw new RuntimeException(String.format("unknown exit type: " + op));
        };

        if (labels.isEmpty()) {
            var msg = String.format("'exit %s' must be in a %s statement", op, op);
            throw new RuntimeException(msg);
        }

        model.gotoGosubStmt("goto", labels.peek());
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
                    case ADDRESS -> model.callLibrarySubroutine("address", expr);
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

    public Symbol findGotoGosubLabel(Token id) {
        return gotoGosubLabels.computeIfAbsent(
                model.fixCase(id.getText()),
                x -> model.addLabels(x).get(0));
    }

    @Override
    public Expression visitLabel(BasicParser.LabelContext ctx) {
        model.labelStmt(findGotoGosubLabel(ctx.id));
        return null;
    }

    @Override
    public Expression visitGotoGosubStmt(BasicParser.GotoGosubStmtContext ctx) {
        model.gotoGosubStmt(ctx.op.getText(), findGotoGosubLabel(ctx.l));
        return null;
    }

    /**
     * Build an ON ... ( GOTO | GOSUB ) statement. Note that we simply generate an array of the
     * labels and then index into them.
     * <p>
     * Pseudocode. NOTE: the upper range check needs to extend array length by one
     * sinc the array has a zero index but ON ... GOTO/GOSUB starts at index 1.
     * <pre>
     * IF N > 0 AND N <= UBOUND(ADDRS)+1 THEN
     *     ( GOTO | GOSUB ) *ADDRS[N-1]
     * END IF
     * </pre>
     */
    @Override
    public Expression visitOnGotoGosubStmt(BasicParser.OnGotoGosubStmtContext ctx) {
        var op = ctx.op.getText();
        var expr = visit(ctx.a);
        var addrof = ctx.ID().stream()
                .map(TerminalNode::getSymbol)
                .map(this::findGotoGosubLabel)
                .map(AddressOfFunction::new)
                .map(Expression.class::cast)
                .toList();

        var name = String.format("ON_%s", op).toUpperCase();
        var addrs = model.addArrayDefaultVariable(name, DataType.ADDRESS, 1, addrof);
        // TODO optimize "expr" reference due to multiple references
        var test = new BinaryExpression(new BinaryExpression(expr, IntegerConstant.ZERO, ">"),
                new BinaryExpression(expr, new ArrayLengthFunction(model, addrs), "<="), "and");
        model.pushStatementBlock(new StatementBlock());
        model.dynamicGotoGosubStmt(op, VariableReference.with(addrs, new BinaryExpression(expr, IntegerConstant.ONE, "-")), true);
        var sb = model.popStatementBlock();
        model.ifStmt(test, sb, null);
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

    List<IdDeclaration> buildDeclarationList(List<BasicParser.IdDeclContext> params) {
        var decls = new ArrayList<IdDeclaration>();
        var names = new HashSet<>();
        if (params != null) {
            params.forEach(idDecl -> {
                Set<IdModifier> modifiers = new HashSet<>();
                if (idDecl.idModifier() != null) {
                    modifiers.add(IdModifier.valueOf(idDecl.idModifier().getText().toUpperCase()));
                }
                String id = model.fixCase(idDecl.ID().getText());
                if (names.contains(id)) {
                    throw new RuntimeException("parameter already defined: " + id);
                }
                DataType dt = buildDataType(idDecl.datatype());
                List<Expression> dimensions = new ArrayList<>();
                for (var expr : idDecl.expr()) {
                    dimensions.add(visit(expr));
                }
                if (dimensions.size() > 1) {
                    throw new RuntimeException("only support 1 dimensional arrays at this time: " + id);
                }
                if (modifiers.contains(IdModifier.STATIC) && dimensions.size() > 0) {
                    // FIXME?
                    System.out.println("WARNING: static array size set by assignment");
                }
                if (idDecl.getText().contains("()")) {
                    // We should be able to assume no dimensions declared
                    dimensions.add(new IntegerConstant(1));
                }
                boolean isArray = !dimensions.isEmpty();
                List<Expression> defaultValues = new ArrayList<>();
                if (idDecl.idDeclDefault() != null && idDecl.idDeclDefault().anyExpr() != null) {
                    for (var anyExpr : idDecl.idDeclDefault().anyExpr()) {
                        var expr = visit(anyExpr);
                        if (isArray && !expr.isConstant()) {
                            throw new RuntimeException("array default values currently must be constant: "
                                + anyExpr.getText());
                        }
                        defaultValues.add(expr);
                    }
                }
                if (isArray && modifiers.contains(IdModifier.STATIC)) {
                    dimensions.clear();;
                    dimensions.add(new IntegerConstant(defaultValues.size()));
                }
                names.add(id);
                decls.add(new IdDeclaration(modifiers, id, dt, dimensions, defaultValues));
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
        List<Symbol.Builder> params = Collections.emptyList();
        if (ctx.paramDecl() != null) {
            params = buildDeclarationList(ctx.paramDecl().idDecl()).stream()
                .map(IdDeclaration::toParameter)
                .collect(Collectors.toList());
        }
        boolean inline = ctx.f != null && "inline".equalsIgnoreCase(ctx.f.getText());

        var sub = model.subDeclBegin(ctx.id.getText(), params);
        sub.setInline(inline);
        visit(ctx.s);
        model.subDeclEnd();
        return null;
    }

    @Override
    public Expression visitFuncDecl(BasicParser.FuncDeclContext ctx) {
        List<Symbol.Builder> params = Collections.emptyList();
        if (ctx.paramDecl() != null) {
            params = buildDeclarationList(ctx.paramDecl().idDecl()).stream()
                .map(IdDeclaration::toParameter)
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
            if (decl.isArray()) {
                // FIXME assuming 1 dimension
                if (decl.hasDefaultValues()) {
                    decl.defaultValues().forEach(expr -> {
                        if (!expr.isConstant()) {
                            throw new RuntimeException("default array values must be constant");
                        }
                    });
                    model.addArrayDefaultVariable(decl.name(), decl.dataType(),
                        decl.dimensions().size(), decl.defaultValues());
                }
                else {
                    var symbol = model.addArrayVariable(decl.name(), decl.dataType(), decl.dimensions().size());
                    model.allocateIntegerArray(symbol, decl.dimensions().get(0));
                    model.registerDimArray(symbol, decl.dimensions().get(0));
                }
            }
            else {
                var symbol = model.addVariable(decl.name(), decl.dataType());
                if (decl.hasDefaultValues()) {
                    if (decl.defaultValues().size() != 1) {
                        throw new RuntimeException("can only assign one default value: " + decl.name());
                    }
                    model.assignStmt(new VariableReference(symbol), decl.defaultValues().get(0));
                }
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
        var symbol = model.findSymbol(id).orElseGet(() -> {
            if (id.contains(".")) {
                throw new RuntimeException("invalid identifier: " + id);
            }
            return model.addVariable(id, DataType.INTEGER);
        });
        return new VariableReference(symbol);
    }

    @Override
    public Expression visitArrayOrFunctionRef(BasicParser.ArrayOrFunctionRefContext ctx) {
        var id = ctx.ID().getText();
        List<Expression> params = new ArrayList<>();
        if (ctx.anyExpr() != null) {
            ctx.anyExpr().stream().map(this::visit).forEach(params::add);
        }

        if ("ubound".equalsIgnoreCase(id)) {
            if (params.size() == 1 && params.get(0) instanceof VariableReference varRef) {
                return new ArrayLengthFunction(model, varRef.getSymbol());
            }
            throw new RuntimeException("ubound expects a variable name as its argument: " + ctx.getText());
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

        // FIXME
        if (params.size() > 1) {
            throw new RuntimeException("only single dimension arrays supported at this time: " + ctx.getText());
        }
        model.checkArrayBounds(existing.get(), params.get(0), ctx.getStart().getLine());
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
        return visit(ctx.a);
    }

    @Override
    public Expression visitUnaryExpr(BasicParser.UnaryExprContext ctx) {
        Expression e = visit(ctx.a);
        String op = ctx.op.getText();
        return new UnaryExpression(op, e);
    }

    enum IdModifier {
        STATIC
    }
    record IdDeclaration(Set<IdModifier> modifiers,
                         String name,
                         DataType dataType,
                         List<Expression> dimensions,
                         List<Expression> defaultValues) {
        /** Validate this is appropriate for a parameter and transform it. */
        public Symbol.Builder toParameter() {
            if (!defaultValues().isEmpty()) {
                throw new RuntimeException("parameters cannot have default values");
            }
            return Symbol.variable(name, SymbolType.PARAMETER)
                    .dataType(dataType)
                    .dimensions(dimensions.size());
        }
        public boolean isArray() {
            return dimensions != null && dimensions.size() > 0;
        }
        public boolean hasDefaultValues() {
            return defaultValues != null && defaultValues.size() > 0;
        }
    }
}
