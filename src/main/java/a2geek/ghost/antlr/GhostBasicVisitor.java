package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.BasicBaseVisitor;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.antlr.generated.BasicParser.IfStatementContext;
import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.GotoGosubStatement;
import a2geek.ghost.model.statement.IfStatement;
import a2geek.ghost.model.statement.OnErrorStatement;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static a2geek.ghost.model.CommonExpressions.*;
import static a2geek.ghost.model.ModelBuilder.*;
import static a2geek.ghost.model.visitor.ExpressionVisitors.hasAnyArraySymbol;

public class GhostBasicVisitor extends BasicBaseVisitor<Expression> {
    private final ModelBuilder model;
    private final CompilerConfiguration config;
    private final Map<String,Symbol> gotoGosubLabels = new HashMap<>();
    private final Stack<LoopFrame> forFrames = new Stack<>();
    private final Stack<LoopFrame> doFrames = new Stack<>();
    private final Stack<LoopFrame> repeatFrames = new Stack<>();
    private final Stack<LoopFrame> whileFrames = new Stack<>();
    private Predicate<String> variableTest = (s) -> true;

    public GhostBasicVisitor(ModelBuilder model) {
        this.model = model;
        this.config = model.getConfig();
        Intrinsic.CPU_REGISTERS.forEach(name -> model.addIntrinsicVariable(name, DataType.INTEGER));
        //
        var sub = new Subroutine(model.getProgram(), "dealloc", List.of(Symbol.variable("bytes", SymbolType.PARAMETER).dataType(DataType.INTEGER)));
        model.peekScope().addLocalSymbol(Symbol.scope(sub).declarationType(DeclarationType.INTRINSIC));
    }

    public ModelBuilder getModel() {
        return model;
    }

    public Optional<Expression> optVisit(ParseTree pt) {
        return Optional.ofNullable(pt).map(this::visit);
    }

    @Override
    public Expression visitProgram(BasicParser.ProgramContext ctx) {
        super.visitProgram(ctx);
        model.uses(RUNTIME_LIBRARY, nothingExported()); // must be last to ensure exports are handled correctly!
        return null;
    }

    @Override
    public Expression visitModule(BasicParser.ModuleContext ctx) {
        model.moduleDeclBegin(ctx.identifier().getText());
        super.visitModule(ctx);
        model.moduleDeclEnd();
        return null;
    }

    @Override
    public Expression visitUseDirective(BasicParser.UseDirectiveContext ctx) {
        for (var str : ctx.STR()) {
            String libname = str.getText().replaceAll("^\"|\"$", "");
            model.uses(libname, defaultExport());
        }
        return null;
    }

    @Override
    public Expression visitOptionDirective(BasicParser.OptionDirectiveContext ctx) {
        switch (ctx.optionTypes().op.getText()) {
            case "heap" -> {
                int lomem = 0x8000;
                if (ctx.optionTypes().lomem != null) {
                    lomem = parseInteger(ctx.optionTypes().lomem.getText());
                }
                model.useMemoryForHeap(lomem);
            }
            case "strict" -> {
                this.variableTest = this::ensureVariableExists;
            }
            default -> throw new RuntimeException("unknown option type: " + ctx.getText());
        }
        return null;
    }

    public boolean ensureVariableExists(String varName) {
        if (model.findSymbol(varName).isEmpty()) {
            var msg = String.format("variable '%s' does not exist; in option strict mode, it must be DIMmed", varName);
            throw new RuntimeException(msg);
        }
        return true;
    }

    @Override
    public Expression visitAssignment(BasicParser.AssignmentContext ctx) {
        var ref = visit(ctx.id);
        var expr = visit(ctx.a);
        switch (ctx.op.getText()) {
            case "=" -> {} // do nothing
            case "+=" -> expr = ref.plus(expr);
            case "-=" -> expr = ref.minus(expr);
            case "/=" -> expr = ref.dividedBy(expr);
            case "*=" -> expr = ref.times(expr);
            case "^=" -> expr = model.callFunction("math.ipow", ref, expr);
            case ">>=" -> expr = ref.rshift(expr);
            case "<<=" -> expr = ref.lshift(expr);
            default -> throw new RuntimeException("unexpected assignment operator: " + ctx.getText());
        }
        if (expr.isType(DataType.STRING)) {
            expr = handleStringConcatenation(expr);
        }
        if (ref.isType(DataType.STRING)) {
            model.getProgram().getMemoryManagementStrategy().decrementReferenceCount(ref);
        }
        if (ref instanceof VariableReference varRef) {
            model.assignStmt(varRef, expr);
            if (ref.isType(DataType.STRING)) {
                model.getProgram().getMemoryManagementStrategy().incrementReferenceCount(ref);
            }
            return null;
        }
        else if (ref instanceof DereferenceOperator deref) {
            model.assignStmt(deref, expr);
            if (ref.isType(DataType.STRING)) {
                model.getProgram().getMemoryManagementStrategy().incrementReferenceCount(ref);
            }
            return null;
        }
        else {
            throw new RuntimeException("expecting a variable type for assignment: " + ctx.id.getText());
        }
    }

    /**
     * The tree structure for string concatenation creates an unnecessarily complex chain of events.
     * Instead of concatenating just two items at a time, we concatenate all of them at once.
     * Note that this strategy does mean we need to identify every place a string expression might
     * exist and set that up.
     * <p/>
     * From (pseudocode for <pre>a$=a$+b$+c$</pre> with extra details dropped):
     * <pre>
     *     T1=alloc(len(b$)+len(c$))
     *     strcat(T1,b$)
     *     strcat(T1,c$)
     *     T2=alloc(len(a$)+len(T1))
     *     strcat(T2,a$)
     *     strcat(T2,T1)
     *     return T2
     * </pre>
     * ... to ...
     * <pre>
     *     T1=alloc(len(a$)+len(b$)+len(c$))
     *     strcat(T1,a$)
     *     strcat(T1,b$)
     *     strcat(T1,c$)
     *     return T1
     * </pre>
     */
    Expression handleStringConcatenation(Expression expr) {
        if (!expr.isType(DataType.STRING)) {
            throw new RuntimeException("[compiler bug] expecting string expression: " + expr);
        }
        // only for binary concatenation expressions
        if (!(expr instanceof BinaryExpression bin && "+".equals(bin.getOp()))) {
            return expr;
        }
        List<Expression> exprs = new ArrayList<>();
        collectStringExpressions(expr, exprs);
        var strlen = exprs.stream()
                .map(str -> model.callFunction("strings.len",str))
                .reduce(Expression::plus)
                .orElseThrow();
        var symbol = model.addTempVariable(DataType.STRING);
        var temp = VariableReference.with(symbol);
        model.allocateStringArray(symbol, strlen);
        exprs.forEach(str -> {
            model.callSubroutine("strings.strcat", temp, str);
        });
        return temp;
    }
    public void collectStringExpressions(Expression source, List<Expression> exprs) {
        if (source instanceof BinaryExpression bin) {
            collectStringExpressions(bin.getL(), exprs);
            collectStringExpressions(bin.getR(), exprs);
        }
        else {
            exprs.add(source);
        }
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

    @Override
    public Expression visitSelectStmt(BasicParser.SelectStmtContext ctx) {
        StatementBlock currentStatementBlock = null;
        if (ctx.selectElseFragment() != null) {
            currentStatementBlock = model.pushStatementBlock(new StatementBlock());
            visit(ctx.selectElseFragment().statements());
            model.popStatementBlock();
        }

        var expr = model.simplify(visit(ctx.a));
        for (var fragment : ctx.selectCaseFragment().reversed()) {
            var trueStatements = model.pushStatementBlock(new StatementBlock());
            visit(fragment.statements());
            model.popStatementBlock();

            Expression test = null;
            for (var caseExpr : fragment.selectCaseExpr()) {
                // Generate the specific CASE expression
                BinaryExpression binex;
                if (caseExpr.op != null) {
                    // 'is'? op=( '<' | '<=' | '>' | '>=' | '=' | '<>' ) a=expr :: expr OP a
                    binex = new BinaryExpression(expr, visit(caseExpr.a), caseExpr.op.getText());
                }
                else if (caseExpr.b != null) {
                    // a=expr 'to' b=expr :: expr >= a AND expr <= b
                    binex = expr.ge(visit(caseExpr.a)).and(expr.le(visit(caseExpr.b)));
                }
                else {
                    // a=expr :: expr = a
                    binex = expr.eq(visit(caseExpr.a));
                }

                // Concatenate CASE expressions together
                if (test == null) {
                    test = binex;
                }
                else {
                    test = test.or(binex);
                }
            }

            model.pushStatementBlock(new StatementBlock());
            model.ifStmt(test, trueStatements, currentStatementBlock);
            currentStatementBlock = model.popStatementBlock();
        }
        model.addStatements(currentStatementBlock);
        return null;
    }

    @Override
    public Expression visitOnErrorStmt(BasicParser.OnErrorStmtContext ctx) {
        var stmt = switch (ctx.op.getText().toLowerCase()) {
            case "goto" -> new OnErrorStatement(findGotoGosubLabel(ctx.l));
            case "disable" -> new OnErrorStatement(OnErrorStatement.Operation.DISABLE);
            case "resume" -> new OnErrorStatement(OnErrorStatement.Operation.RESUME_NEXT);
            default -> throw new RuntimeException("unexpected ON ERROR construct: " + ctx.getText());
        };
        model.addStatement(stmt);
        return null;
    }

    @Override
    public Expression visitRaiseErrorStmt(BasicParser.RaiseErrorStmtContext ctx) {
        Expression expr = visit(ctx.a);
        Expression msg = StringConstant.EMPTY;
        if (ctx.m != null) {
            msg = handleStringConcatenation(visit(ctx.m));
        }
        Expression context = StringConstant.EMPTY;
        if (ctx.c != null) {
            context = handleStringConcatenation(visit(ctx.c));
        }
        var linenum = new IntegerConstant(ctx.getStart().getLine());
        var source = new StringConstant(ctx.getStart().getTokenSource().getSourceName());
        model.raiseError(expr, msg, linenum, source, context);
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
        variableTest.test(ctx.id.getText());
        Symbol symbol = model.addVariable(ctx.id.getText(), DataType.INTEGER);
        Expression start = visit(ctx.a);
        Expression end = visit(ctx.b);
        Expression step = optVisit(ctx.c).orElse(new IntegerConstant(1));

        var ref = VariableReference.with(symbol);
        var labels = model.addLabels("FOR_LOOP", "FOR_EXIT", "FOR_CONTINUE");
        var loopLabel = labels.get(0);
        var exitLabel = labels.get(1);
        var continueLabel = labels.get(2);

        // building contents of "IF" statement
        model.pushStatementBlock(new StatementBlock());
        forFrames.push(new LoopFrame(continueLabel, exitLabel));
        optVisit(ctx.s);
        forFrames.pop();
        model.labelStmt(continueLabel);
        model.assignStmt(ref, ref.plus(step));
        model.gotoGosubStmt("goto", loopLabel);
        var sb = model.popStatementBlock();
        var positive = new IfStatement(ref.le(end), sb, null);
        var negative = new IfStatement(ref.ge(end), sb, null);

        // generating code
        model.assignStmt(ref, start);
        model.labelStmt(loopLabel);
        // IF (SGN(step) >= 0)
        model.ifStmt(model.callFunction("SGN", step).ge(IntegerConstant.ZERO),
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
        }
        doFrames.push(new LoopFrame(loopLabel, exitLabel));
        optVisit(ctx.s);
        doFrames.pop();
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
        doFrames.push(new LoopFrame(loopLabel, exitLabel));
        optVisit(ctx.s);
        doFrames.pop();
        switch (ctx.op.getText()) {
            case "while" -> model.ifStmt(test, gotoStatement, null);
            case "until" -> model.ifStmt(test, StatementBlock.EMPTY, gotoStatement);
            default -> throw new RuntimeException("unexpected do loop type: " + ctx.op.getText());
        }
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
        whileFrames.push(new LoopFrame(loopLabel, exitLabel));
        optVisit(ctx.s);
        whileFrames.pop();
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

        var labels = model.addLabels("REPEAT_LOOP", "REPEAT_EXIT", "REPEAT_CONTINUE");
        var loopLabel = labels.get(0);
        var exitLabel = labels.get(1);
        var continueLabel = labels.get(2);

        model.labelStmt(loopLabel);
        repeatFrames.push(new LoopFrame(continueLabel, exitLabel));
        optVisit(ctx.s);
        repeatFrames.pop();
        model.ifStmt(test, StatementBlock.EMPTY, StatementBlock.with(new GotoGosubStatement("goto", loopLabel)));
        model.labelStmt(exitLabel);

        return null;
    }

    @Override
    public Expression visitContinueExitStmt(BasicParser.ContinueExitStmtContext ctx) {
        var op = ctx.op.getText().toLowerCase();
        var stmtType = ctx.n.getText().toLowerCase();
        var frames = switch (stmtType) {
            case "for" -> forFrames;
            case "while" -> whileFrames;
            case "repeat" -> repeatFrames;
            case "do" -> doFrames;
            default -> throw new RuntimeException("unknown exit type: " + ctx.getText());
        };

        if (frames.isEmpty()) {
            var msg = String.format("'%s %s' must be in a %s statement", op, stmtType, stmtType);
            throw new RuntimeException(msg);
        }

        var label = switch (op) {
            case "continue" -> frames.peek().continueLabel;
            case "exit" -> frames.peek().exitLabel;
            default -> throw new RuntimeException("unknown exit type: " + ctx.getText());
        };
        model.gotoGosubStmt("goto", label);
        return null;
    }

    @Override
    public Expression visitEndStmt(BasicParser.EndStmtContext ctx) {
        model.endStmt();
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
                model.callLibrarySubroutine("print_comma");
            }
            else {
                Expression expr = pt.accept(this);
                switch (expr.getType()) {
                    case INTEGER -> model.callLibrarySubroutine("print_integer", expr);
                    case BOOLEAN -> model.callLibrarySubroutine("print_boolean", expr);
                    case STRING -> model.callLibrarySubroutine("print_string", handleStringConcatenation(expr));
                    case ADDRESS -> model.callLibrarySubroutine("print_address", expr);
                    case BYTE -> model.callLibrarySubroutine("print_byte", expr);
                    default -> throw new RuntimeException("Unsupported PRINT type: " + expr.getType());
                }
            }
        }
        if (!semiColonAtEnd) {
            model.callLibrarySubroutine("print_newline");
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
        switch (ctx.op.getText().toLowerCase()) {
            case "poke" -> model.assignStmt(derefByte(a), b);
            case "pokew" -> model.assignStmt(derefWord(a), b);
            default -> throw new RuntimeException("[compiler bug] unknown poke statement: " + ctx.getText());
        }
        return null;
    }

    public Symbol findGotoGosubLabel(BasicParser.IdentifierContext ctx) {
        return gotoGosubLabels.computeIfAbsent(
                config.applyCaseStrategy(ctx.getText()),
                x -> model.addLabels(x).getFirst());
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
     * IF N > 0 AND N <= UBOUND(ADDRS,1) THEN
     *     ( GOTO | GOSUB ) *(ADDRS+((N-1)*2))
     * END IF
     * </pre>
     */
    @Override
    public Expression visitOnGotoGosubStmt(BasicParser.OnGotoGosubStmtContext ctx) {
        var op = ctx.op.getText();
        var expr = model.simplify(visit(ctx.a));
        var addrof = ctx.identifier().stream()
                .map(this::findGotoGosubLabel)
                .map(AddressOfOperator::new)
                .map(Expression.class::cast)
                .toList();

        var name = String.format("ON_%s", op).toUpperCase();
        var addrs = model.addArrayDefaultVariable(name, DataType.ADDRESS, List.of(new IntegerConstant(addrof.size())), addrof);
        var test = expr.gt(IntegerConstant.ZERO).and(expr.le(new ArrayLengthFunction(addrs, 1)));
        model.pushStatementBlock(new StatementBlock());
        model.dynamicGotoGosubStmt(op, arrayReference(addrs, List.of(expr.minus1())), true);
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

    List<IdDeclaration> buildDeclarationList(List<BasicParser.IdDeclContext> params) {
        var decls = new ArrayList<IdDeclaration>();
        var names = new HashSet<>();
        if (params != null) {
            params.forEach(idDecl -> {
                Set<IdModifier> modifiers = new HashSet<>();
                if (idDecl.idModifier() != null) {
                    modifiers.add(IdModifier.valueOf(idDecl.idModifier().getText().toUpperCase()));
                }
                String id = config.applyCaseStrategy(idDecl.identifier().getText());
                if (names.contains(id)) {
                    throw new RuntimeException("variable already defined: " + id);
                }
                List<Expression> dimensions = new ArrayList<>();
                for (var expr : idDecl.expr()) {
                    dimensions.add(visit(expr));
                }
                if (modifiers.contains(IdModifier.STATIC) && !dimensions.isEmpty()) {
                    config.trace("WARNING: static array size set by assignment");
                }
                if (idDecl.getText().contains("()")) {
                    // We should be able to assume no dimensions declared
                    dimensions.add(new IntegerConstant(1));
                }
                boolean isArray = !dimensions.isEmpty();
                DataType dt = buildDataType(id, idDecl.datatype(), dimensions.isEmpty());
                List<Expression> defaultValues = new ArrayList<>();
                if (idDecl.idDeclDefault() != null && idDecl.idDeclDefault().expr() != null) {
                    for (var defaultExpr : idDecl.idDeclDefault().expr()) {
                        var expr = visit(defaultExpr);
                        if (isArray && !expr.isConstant()) {
                            throw new RuntimeException("array default values currently must be constant: "
                                    + defaultExpr.getText());
                        }
                        defaultValues.add(expr);
                    }
                }
                if (isArray && !defaultValues.isEmpty()) {
                    dimensions.clear();
                    dimensions.add(new IntegerConstant(defaultValues.size()));
                }
                names.add(id);
                decls.add(new IdDeclaration(modifiers, id, PassingMode.BYVAL, dt, dimensions, defaultValues));
            });
        }
        return decls;
    }
    DataType buildDataType(String id, BasicParser.DatatypeContext ctx, boolean isNotArray) {
        DataType nameType = determineDataType(id, null);
        DataType dt = null;
        if (ctx != null) {
            dt = DataType.valueOf(ctx.getText().toUpperCase());
        }
        if (isNotArray && dt == DataType.BYTE) {
            dt = DataType.INTEGER;
        }
        if (nameType == null && dt == null) {
            return DataType.INTEGER;
        }
        else if (nameType == null) {
            return dt;
        }
        else if (dt == null) {
            return nameType;
        }
        else if (nameType == dt) {
            return dt;
        }
        var msg = String.format("'%s' can't be type %s", id, dt);
        throw new RuntimeException(msg);
    }

    List<IdDeclaration> buildIdDeclarationList(List<BasicParser.ParamIdDeclContext> params) {
        var decls = new ArrayList<IdDeclaration>();
        var names = new HashSet<>();
        if (params != null) {
            params.forEach(idDecl -> {
                String id = config.applyCaseStrategy(idDecl.identifier().getText());
                if (names.contains(id)) {
                    throw new RuntimeException("parameter already defined: " + id);
                }
                int numDimensions = 0;
                if (idDecl.getText().contains("(")) {
                    // count the number of commas to figure out dimensions.  "SUB NAME(ARRAY(,,) AS INTEGER)" => 3 dimensions
                    numDimensions = idDecl.getText().length() - idDecl.getText().replace(",", "").length() + 1;
                }
                List<Expression> dimensions = new ArrayList<>();
                for (int i=0; i<numDimensions; i++) {
                    // not actual dimension size; but Symbol holds on to the defining dimension expressions
                    dimensions.add(PlaceholderExpression.of(DataType.INTEGER));
                }
                DataType dt = buildDataType(id, idDecl.datatype(), dimensions.isEmpty());
                List<Expression> defaultValues = new ArrayList<>();
                if (idDecl.optional() != null || idDecl.expr() != null) {
                    if (idDecl.expr() == null) {
                        throw new RuntimeException("Optional parameter specified but no default value given: " + idDecl.getText());
                    }
                    var defaultValue = visit(idDecl.expr());
                    if (!defaultValue.isConstant()) {
                        throw new RuntimeException("paramater default values must be a constant: " + idDecl.getText());
                    }
                    defaultValues.add(defaultValue);
                }
                PassingMode passingMode = PassingMode.BYVAL;
                if (idDecl.passingMode() != null) {
                    passingMode = PassingMode.valueOf(idDecl.passingMode().getText().toUpperCase());
                }
                names.add(id);
                decls.add(new IdDeclaration(Set.of(), id, passingMode, dt, dimensions, defaultValues));
            });
        }
        return decls;
    }

    @Override
    public Expression visitSubDecl(BasicParser.SubDeclContext ctx) {
        List<Symbol.Builder> params = Collections.emptyList();
        if (ctx.paramDecl() != null) {
            params = buildIdDeclarationList(ctx.paramDecl().paramIdDecl()).stream()
                    .map(IdDeclaration::toParameter)
                    .collect(Collectors.toList());
        }

        var sub = model.subDeclBegin(ctx.id.getText(), params);
        applyModifiers(ctx.modifiers(), sub);
        applyVisibility(ctx.visibility(), sub);
        ensureValidConstruct(sub);
        visit(ctx.s);
        model.subDeclEnd();
        // TODO free any allocated memory!
        return null;
    }

    void applyModifiers(List<BasicParser.ModifiersContext> ctx, Subroutine subOrFunc) {
        if (ctx != null) {
            ctx.forEach(modifier -> {
                switch (modifier.getText().toLowerCase()) {
                    case "inline" -> subOrFunc.add(Subroutine.Modifier.INLINE);
                    case "export" -> subOrFunc.add(Subroutine.Modifier.EXPORT);
                    case "volatile" -> subOrFunc.add(Subroutine.Modifier.VOLATILE);
                    default -> {
                        var msg = String.format("Unknown modifier '%s' encountered", modifier.getText());
                        throw new RuntimeException(msg);
                    }
                }
            });
        }
    }
    void applyVisibility(BasicParser.VisibilityContext ctx, Subroutine subOrFunc) {
        if (ctx != null) {
            switch (ctx.getText().toLowerCase()) {
                case "public" -> subOrFunc.set(Visibility.PUBLIC);
                case "private" -> subOrFunc.set(Visibility.PRIVATE);
                default -> {
                    var msg = String.format("Unknown visibility '%s' encountered", ctx.getText());
                    throw new RuntimeException(msg);
                }
            }
        }
    }

    void ensureValidConstruct(Subroutine subOrFunc) {
        if (subOrFunc.is(Visibility.PRIVATE) && subOrFunc.is(Subroutine.Modifier.EXPORT)) {
            throw new RuntimeException("cannot export a private routine: " + subOrFunc.getFullPathName());
        }
    }

    @Override
    public Expression visitFuncDecl(BasicParser.FuncDeclContext ctx) {
        List<Symbol.Builder> params = Collections.emptyList();
        if (ctx.paramDecl() != null) {
            params = buildIdDeclarationList(ctx.paramDecl().paramIdDecl()).stream()
                    .map(IdDeclaration::toParameter)
                    .collect(Collectors.toList());
        }
        var id = ctx.id.getText();
        DataType dt = buildDataType(id, ctx.datatype(), true);  // we know this is not an array ;-)

        var func = model.funcDeclBegin(id, dt, params);
        applyModifiers(ctx.modifiers(), func);
        applyVisibility(ctx.visibility(), func);
        ensureValidConstruct(func);
        visit(ctx.s);
        // TODO free any allocated memory!
        model.funcDeclEnd();
        return null;
    }

    @Override
    public Expression visitCallSub(BasicParser.CallSubContext ctx) {
        List<Expression> params = new ArrayList<>();
        if (ctx.p != null) {
            ctx.parameters().expr().stream().map(this::visit).forEach(params::add);
        }
        model.callSubroutine(ctx.id.getText(), params);
        return null;
    }

    @Override
    public Expression visitDimStmt(BasicParser.DimStmtContext ctx) {
        buildDeclarationList(ctx.idDecl()).forEach(decl -> {
            if (decl.isArray()) {
                // FIXME defaults only apply to 1 dimension
                if (decl.hasDefaultValues() && decl.dimensions.size() == 1) {
                    decl.defaultValues().forEach(expr -> {
                        if (!expr.isConstant()) {
                            throw new RuntimeException("default array values must be constant");
                        }
                    });
                    model.addArrayDefaultVariable(decl.name(), decl.dataType(),
                            decl.dimensions(), decl.defaultValues());
                }
                else {
                    var symbol = model.addArrayVariable(decl.name(), decl.dataType(), decl.dimensions());
                    model.allocateIntegerArray(symbol, decl.dimensions());
                }
            }
            else {
                var symbol = model.addVariable(decl.name(), decl.dataType());
                if (decl.hasDefaultValues()) {
                    if (decl.defaultValues().size() != 1) {
                        throw new RuntimeException("can only assign one default value: " + decl.name());
                    }
                    model.assignStmt(VariableReference.with(symbol), decl.defaultValues().getFirst());
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
                // we need to collapse any expressions to a single value
                e = switch (e.getType()) {
                    case INTEGER -> new IntegerConstant(e.asInteger().orElseThrow());
                    case BOOLEAN -> new BooleanConstant(e.asBoolean().orElseThrow());
                    case STRING -> new StringConstant(e.asString().orElseThrow());
                    default -> e;
                };
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
            variableTest.test(id);
            return model.addVariable(id, determineDataType(id, DataType.INTEGER));
        });
        return VariableReference.with(symbol);
    }

    /**
     * Determine the data type if we only have the ID.
     * '$' = STRING, '%' = INTEGER, no decoration is indeterminate
     * and can be configured.
     */
    public DataType determineDataType(String id, DataType defaultDataType) {
        if (id.endsWith("$")) {
            return DataType.STRING;
        }
        else if (id.endsWith("%")) {
            return DataType.INTEGER;
        }
        return defaultDataType;
    }

    private final Map<String, BiFunction<List<Expression>,ParseTree,Expression>> FUNCTION_HANDLERS;
    {
        FUNCTION_HANDLERS = new HashMap<>();
        FUNCTION_HANDLERS.putAll(Map.of(
            "addrof", this::handleAddrOfFunction,
            "caddr", this::handleCAddrFunction,
            "cbool", this::handleCBoolFunction,
            "cbyte", this::handleCByteFunction,
            "cint", this::handleCIntFunction,
            "peek", this::handlePeekFunction,
            "peekw", this::handlePeekwFunction,
            "ubound", this::handleUboundFunction
        ));
    }

    Expression handleUboundFunction(List<Expression> params, ParseTree ctx) {
        if (params.size() == 1 && params.getFirst() instanceof VariableReference varRef) {
            return new ArrayLengthFunction(varRef.getSymbol(), 1);
        }
        else if (params.size() == 2 && params.getFirst() instanceof VariableReference varRef && params.getLast().isConstant()) {
            return new ArrayLengthFunction(varRef.getSymbol(), params.getLast().asInteger().orElseThrow());
        }
        throw new RuntimeException("ubound expects a variable name (and optionally an index number) as its argument: " + ctx.getText());
    }
    Expression handlePeekFunction(List<Expression> params, ParseTree ctx) {
        if (params.size() == 1) {
            return derefByte(params.getFirst());
        }
        throw new RuntimeException("peek expects one paramter: " + ctx.getText());
    }
    Expression handlePeekwFunction(List<Expression> params, ParseTree ctx) {
        if (params.size() == 1) {
            return derefWord(params.getFirst());
        }
        throw new RuntimeException("peekw expects one paramter: " + ctx.getText());
    }
    Expression handleAddrOfFunction(List<Expression> params, ParseTree ctx) {
        if (params.size() == 1 && params.getFirst() instanceof VariableReference varRef) {
            return new AddressOfOperator(varRef.getSymbol());
        }
        // Array references end up wrapped in a dereference operator, so we need to unwrap it
        else if (params.size() == 1 && params.getFirst() instanceof DereferenceOperator deref && hasAnyArraySymbol(deref.getExpr())) {
            return deref.getExpr();
        }
        throw new RuntimeException("can only take addrof a simple variable or an array index: " + ctx.getText());
    }
    Expression handleCAddrFunction(List<Expression> params, ParseTree ctx) {
        if (params.size() == 1) {
            return new TypeConversionOperator(params.getFirst(), DataType.ADDRESS);
        }
        throw new RuntimeException("can only use CAddr on a simple variable: " + ctx.getText());
    }
    Expression handleCBoolFunction(List<Expression> params, ParseTree ctx) {
        if (params.size() == 1) {
            return new TypeConversionOperator(params.getFirst(), DataType.BOOLEAN);
        }
        throw new RuntimeException("can only use CBool on a simple variable: " + ctx.getText());
    }
    Expression handleCByteFunction(List<Expression> params, ParseTree ctx) {
        if (params.size() == 1) {
            return new TypeConversionOperator(params.getFirst(), DataType.BYTE);
        }
        throw new RuntimeException("can only use CByte on a simple variable: " + ctx.getText());
    }
    Expression handleCIntFunction(List<Expression> params, ParseTree ctx) {
        if (params.size() == 1) {
            return new TypeConversionOperator(params.getFirst(), DataType.INTEGER);
        }
        throw new RuntimeException("can only use CInt on a simple variable: " + ctx.getText());
    }

    void ensureHeapEnabled(ParseTree ctx) {
        if (model.getProgram().getMemoryManagementStrategy().isUsingHeap()) {
            return;
        }
        throw new RuntimeException("requires heap to be enabled: " + ctx.getText());
    }

    @Override
    public Expression visitArrayOrFunctionRef(BasicParser.ArrayOrFunctionRefContext ctx) {
        var id = ctx.extendedID().getText();
        List<Expression> params = new ArrayList<>();
        if (ctx.expr() != null) {
            ctx.expr().stream().map(this::visit).forEach(params::add);
        }

        var fnHandler = FUNCTION_HANDLERS.get(id.toLowerCase());
        if (fnHandler != null) {
            return fnHandler.apply(params, ctx);
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

        params.replaceAll(model::simplify);     // If the parameters are complex, they are evaluated in array check and reference!
        model.checkArrayBounds(existing.get(), params, ctx.getStart().getLine(), ctx.getStart().getTokenSource().getSourceName());
        return arrayReference(existing.get(), params);
    }

    @Override
    public Expression visitIntConstant(BasicParser.IntConstantContext ctx) {
        return new IntegerConstant(parseInteger(ctx.a.getText()));
    }
    public Integer parseInteger(String value) {
        if (value == null) {
            return null;
        }
        if (value.startsWith("0x")) {
            return Integer.parseInt(value.substring(2), 16);
        }
        else if (value.startsWith("0b")) {
            return Integer.parseInt(value.substring(2), 2);
        }
        return Integer.parseInt(value);
    }

    @Override
    public Expression visitStringConstant(BasicParser.StringConstantContext ctx) {
        String value = ctx.s.getText().replaceAll("^\"|\"$", "");
        return new StringConstant(config.applyControlCharsFn(value));
    }

    @Override
    public Expression visitBinaryExpr(BasicParser.BinaryExprContext ctx) {
        Expression l = visit(ctx.a);
        Expression r = visit(ctx.b);
        String op = ctx.op.getText();

        // FIXME need to handle differing data types better
        if (l.isType(DataType.STRING) && r.isType(DataType.STRING)) {
            if ("=".equals(op) || "<>".equals(op)) {
                // strcmp(l,r) op 0
                l = handleStringConcatenation(l);
                r = handleStringConcatenation(r);
                return new BinaryExpression(
                        model.callFunction("strings.strcmp", Arrays.asList(l,r)),
                        IntegerConstant.ZERO,
                        op);
            }
            else if ("+".equals(op)) {
                // Note that we are deferring code to later
                return l.plus(r);
            }
            else {
                throw new RuntimeException("strings only support =,<>,+ operations: " + ctx.getText());
            }
        }
        else {
            if ("^".equals(op)) {
                return model.callFunction("math.ipow", Arrays.asList(l,r));
            }
            return new BinaryExpression(l, r, op);
        }
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
                         PassingMode passingMode,
                         DataType dataType,
                         List<Expression> dimensions,
                         List<Expression> defaultValues) {
        /** Validate this is appropriate for a parameter and transform it. */
        public Symbol.Builder toParameter() {
            var builder = Symbol.variable(name, SymbolType.PARAMETER)
                    .dataType(dataType)
                    .dimensions(dimensions)
                    .passingMode(passingMode);
            if (defaultValues != null && !defaultValues.isEmpty()) {
                builder.defaultValues(defaultValues);
            }
            return builder;
        }
        public boolean isArray() {
            return dimensions != null && !dimensions.isEmpty();
        }
        public boolean hasDefaultValues() {
            return defaultValues != null && !defaultValues.isEmpty();
        }
    }
    record LoopFrame(Symbol continueLabel, Symbol exitLabel) {}
}
