package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.IntegerBaseVisitor;
import a2geek.ghost.antlr.generated.IntegerParser;
import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.ForFrame;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.statement.EndStatement;
import a2geek.ghost.model.statement.IfStatement;
import a2geek.ghost.model.statement.PopStatement;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

import static a2geek.ghost.model.CommonExpressions.arrayReference;
import static a2geek.ghost.model.ModelBuilder.*;

public class IntegerBasicVisitor extends IntegerBaseVisitor<Expression> {
    /** This is a "fake" function that is only used within the visitor itself. */
    public static final String LHS_STRING_NAME = "_LHS_STRING";
    public final Function LHS_STRING;
    public static final String LINE_NUMBERS = "_line_numbers";
    public static final String LINE_LABELS = "_line_labels";

    private final ModelBuilder model;
    private final SortedMap<Integer,Symbol> lineLabels = new TreeMap<>();
    private final Map<Symbol,ForFrame> forFrames = new HashMap<>();
    private final Map<Symbol,Expression> stringsDimmed = new HashMap<>();
    private final Map<Symbol,Integer> knownSizeTempStringVariables = new HashMap<>();
    private final Map<Symbol,Set<Symbol>> unknownSizetempStringVariables = new HashMap<>();
    private final Set<Symbol> tempStringVariablesInUse = new HashSet<>();

    public IntegerBasicVisitor(ModelBuilder model) {
        this.model = model;
        model.setArrayNameStrategy(s -> s + "()");
        model.uses(MATH_LIBRARY, exportSpecified("ABS", "SGN", "RND"));
        model.uses(STRINGS_LIBRARY, exportSpecified("LEN"));
        model.uses(MISC_LIBRARY, exportSpecified("PDL"));
        model.uses(RUNTIME_LIBRARY, nothingExported()); // must be last to ensure exports are handled!
        //
        LHS_STRING = model.funcDeclBegin(LHS_STRING_NAME, DataType.STRING, List.of(
                Symbol.variable("STRING", SymbolType.PARAMETER).dataType(DataType.STRING),
                Symbol.variable("START", SymbolType.PARAMETER).dataType(DataType.INTEGER)));
        model.funcDeclEnd();
    }

    public ModelBuilder getModel() {
        return model;
    }

    @Override
    public Expression visit(ParseTree tree) {
        var expr = super.visit(tree);
        if (expr == null) {
            tempStringVariablesInUse.clear();
        }
        return expr;
    }

    @Override
    public Expression visitProgram(IntegerParser.ProgramContext ctx) {
        super.visitProgram(ctx);
        if (!model.getProgram().isLastStatement(EndStatement.class)) {
            model.endStmt();
        }
        // If we need the line number references for dynamic goto/gosub, create them...
        model.findSymbol(model.fixArrayName(LINE_NUMBERS)).ifPresent(symbol -> {
            lineLabels.keySet().stream()
                    .map(IntegerConstant::new)
                    .map(Expression.class::cast)
                    .forEach(expr -> symbol.defaultValues().add(expr));
        });
        model.findSymbol(model.fixArrayName(LINE_LABELS)).ifPresent(symbol -> {
            lineLabels.values().stream()
                    .map(AddressOfFunction::new)
                    .forEach(expr -> symbol.defaultValues().add(expr));
        });
        // Any strings that do not have a DIM, we need to dim as length 1.
        // Note that we don't/can't validate that the strings dimmed are dimmed first.
        stringsDimmed.forEach((symbol,expr) -> {
            if (expr == null) {
                model.pushStatementBlock(new StatementBlock());
                model.allocateStringArray(symbol, IntegerConstant.ONE);
                var sb = model.popStatementBlock();
                model.addInitializationStatements(sb);
            }
        });
        // Need to generate temp string DIM statements
        knownSizeTempStringVariables.forEach((symbol,size) -> {
            model.pushStatementBlock(new StatementBlock());
            model.allocateStringArray(symbol, new IntegerConstant(size));
            var sb = model.popStatementBlock();
            model.addInitializationStatements(sb);
        });
        unknownSizetempStringVariables.forEach((symbol,symbols) -> {
            var sizes = symbols.stream()
                    .map(stringsDimmed::get)
                    .map(e -> e == null ? IntegerConstant.ONE : e)
                    .toList();
            var constant = sizes.stream().map(Expression::isConstant).reduce((a,b) -> a && b).orElseThrow();
            if (constant) {
                int size = sizes.stream().map(Expression::asInteger).map(Optional::orElseThrow).reduce(Math::max).orElseThrow();
                model.pushStatementBlock(new StatementBlock());
                model.allocateStringArray(symbol, new IntegerConstant(size));
                var sb = model.popStatementBlock();
                model.addInitializationStatements(sb);
            }
            else {
                var msg = String.format("indeterminate temp string size for %s: based on %s", symbol, symbols);
                throw new RuntimeException(msg);
            }
        });
        return null;
    }

    void callSubroutine(String name, ParseTree... expressions) {
        var exprs = new ArrayList<Expression>();
        for (var expression : expressions) {
            exprs.add(visit(expression));
        }
        model.callLibrarySubroutine(name, exprs.toArray(new Expression[0]));
    }

    Symbol gotoGosubLabel(int linenum) {
        return lineLabels.computeIfAbsent(
                linenum,
                n -> model.addLabels(String.format("L%d_", n)).getFirst());
    }

    @Override
    public Expression visitProgramLine(IntegerParser.ProgramLineContext ctx) {
        var lineNumber = Integer.parseInt(ctx.INTEGER().getText());
        var lineLabel = gotoGosubLabel(lineNumber);

        model.labelStmt(lineLabel);
        try {
            visit(ctx.statements());
        } catch (Exception ex) {
            System.out.println(ctx.getText());
            var msg = String.format("Error in line %d: %s", lineNumber, ex.getMessage());
            throw new RuntimeException(msg, ex);
        }
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
    public Expression visitIntDimVar(IntegerParser.IntDimVarContext ctx) {
        var symbol = model.addArrayVariable(ctx.n.getText(), DataType.INTEGER, 1);
        var expr = visit(ctx.e);

        model.allocateIntegerArray(symbol, expr);
        model.registerDimArray(symbol, expr);
        return null;
    }

    @Override
    public Expression visitStrDimVar(IntegerParser.StrDimVarContext ctx) {
        var symbol = model.addVariable(ctx.n.getText(), DataType.STRING);
        var expr = visit(ctx.e);

        model.allocateStringArray(symbol, expr);
        model.registerDimArray(symbol, expr);
        return null;
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

    /**
     * Build a FOR statement.
     * <p>
     * Items handled:<ul>
     * <li>The FOR and NEXT statement are independent of each other and, therefore, unstructured.</li>
     * <li>The compiler needs to track state, where to go, how to exit, etc for the loop constructs.</li>
     * <li>The test condition varies based on if the STEP increment is positive or negative, so that
     *   code is more complicated than expected.</li>
     * <li>We trust the optimizer to collapse any code with constants into a more optimal form.</li>
     * </ul>
     * <p>
     * Sample FOR statement:
     * <pre>
     * FOR X = 1 TO 10 [ STEP 1 ]
     * </pre>
     * <p>
     * Target intermediate pseudocode; "(name)" is a label:
     * <pre>
     * X = START
     * X_END = END
     * X_STEP = STEP
     * X_NEXT_ADDR = ADDROF(NEXT)-1
     * GOTO (LOOP)
     * (NEXT)
     * X = X + X_STEP
     * IF SGN(X_STEP) >= 0 THEN  ' positive increment/zero
     *     IF X <= X_END THEN
     *         GOTO (LOOP)
     *     END IF
     * ELSE                      ' decrement
     *     IF X >= END THEN
     *         GOTO (LOOP)
     *     END IF
     * END IF
     * GOTO *X_EXIT_ADDR         ' dynamic GOTO
     * (LOOP)
     * </pre>
     */
    @Override
    public Expression visitForStatement(IntegerParser.ForStatementContext ctx) {
        var ref = model.addVariable(ctx.ivar().getText(), DataType.INTEGER);
        var first = visit(ctx.first);
        var last = visit(ctx.last);
        Expression step = visitOrDefault(ctx.step, IntegerConstant.ONE);
        ForFrame frame = forFrames.computeIfAbsent(ref, r -> new ForFrame(r, model.peekScope()));

        var labels = model.addLabels("FOR_LOOP", "FOR_NEXT");
        var loopLabel = labels.get(0);
        var nextLabel = labels.get(1);

        var varRef = new VariableReference(ref);
        var endRef = new VariableReference(frame.getEndRef());
        var stepRef = new VariableReference(frame.getStepRef());
        var nextRef = new VariableReference(frame.getNextRef());

        // setup FOR statement
        model.assignStmt(varRef, first);
        model.assignStmt(endRef, last);
        model.assignStmt(stepRef, step);
        model.assignStmt(nextRef, new AddressOfFunction(nextLabel).minus1());
        model.gotoGosubStmt("goto", loopLabel);

        // handle loop increment and test
        model.labelStmt(nextLabel);
        model.assignStmt(varRef, varRef.plus(stepRef));
        model.pushStatementBlock(new StatementBlock());
        model.gotoGosubStmt("goto", loopLabel);
        var sb = model.popStatementBlock();
        var positive = new IfStatement(varRef.le(endRef), sb, null);
        var negative = new IfStatement(varRef.ge(endRef), sb, null);
        model.ifStmt(model.callFunction("SGN", step).ge(IntegerConstant.ZERO),
                StatementBlock.with(positive), StatementBlock.with(negative));
        model.dynamicGotoGosubStmt("goto", VariableReference.with(frame.getExitRef()), false);

        // continue FOR loop
        model.labelStmt(loopLabel);

        return null;
    }

    /**
     * Build a NEXT statement.
     * <p>
     * Items handled:<ul>
     * <li>The FOR and NEXT statement are independent of each other and, therefore, unstructured.</li>
     * <li>The NEXT statement may come first in the code</li>
     * <li>The compiler needs to track state, where to go, how to exit, etc for the loop constructs.</li>
     * </ul>
     * <p>
     * Sample NEXT statement:
     * <pre>
     * NEXT X
     * </pre>
     * <p>
     * Target intermediate pseudocode; "(name)" is a label:
     * <pre>
     * X_EXIT_ADDR = ADDROF(EXIT)-1
     * GOTO *X_NEXT_ADDR         ' dynamic GOTO
     * (LOOP)
     * </pre>
     */
    @Override
    public Expression visitNextStatement(IntegerParser.NextStatementContext ctx) {
        for (var ivar : ctx.ivar()) {
            var ref = model.addVariable(ivar.getText(), DataType.INTEGER);
            ForFrame frame = forFrames.computeIfAbsent(ref, r -> new ForFrame(r, model.peekScope()));
            var exitRef = new VariableReference(frame.getExitRef());

            var labels = model.addLabels("FOR_EXIT");
            var exitLabel = labels.getFirst();

            model.assignStmt(exitRef, new AddressOfFunction(exitLabel).minus1());
            model.dynamicGotoGosubStmt("goto", VariableReference.with(frame.getNextRef()), false);
            model.labelStmt(exitLabel);
        }
        return null;
    }

    /**
     * Handle GOTO/GOSUB statement. Note that we can do a static jump if the expression is just
     * a constant (GOTO 10) but also have to handle if the expression is more complex as well
     * (GOTO i*10).  In the first case, we just generate a direct GOTO or GOSUB.  In the second
     * case, we build a "dynamic" GOTO/GOSUB with appropriate array lookups.
     * <p>
     * Pseudocode for dynamic GOTO/GOSUB. NOTE: the upper range check needs to extend array length
     * by one since the array has a zero index but ON ... GOTO/GOSUB starts at index 1.
     * <pre>
     * _TEMP = line_index(expr, line_numbers)
     * IF _TEMP > 0 AND _TEMP <= UBOUND(line_numbers) THEN
     *     ( GOTO | GOSUB ) *line_labels[_TEMP - 1]
     * END IF
     * </pre>
     */
    @Override
    public Expression visitGosubGotoStatement(IntegerParser.GosubGotoStatementContext ctx) {
        var expr = visit(ctx.e);
        var op = ctx.g.getText();
        if (!expr.isType(DataType.INTEGER)) {
            throw new RuntimeException("GOTO/GOSUB must have an integer target: " + ctx.getText());
        }
        if (expr.isConstant()) {
            var line = expr.asInteger().map(this::gotoGosubLabel).orElseThrow();
            model.gotoGosubStmt(op, line);
        }
        else {
            var lineNumbers = model.addArrayDefaultVariable(LINE_NUMBERS, DataType.INTEGER, 1,
                new ArrayList<>());
            var lineLabels = model.addArrayDefaultVariable(LINE_LABELS, DataType.ADDRESS, 1,
                new ArrayList<>());

            var temp = VariableReference.with(model.addTempVariable(DataType.INTEGER));
            var test = temp.gt(IntegerConstant.ZERO).and(temp.le(new ArrayLengthFunction(model, lineNumbers)));

            model.pushStatementBlock(new StatementBlock());
            model.dynamicGotoGosubStmt(op, arrayReference(lineLabels, temp.minus1()), true);
            var sb = model.popStatementBlock();

            model.assignStmt(temp, model.callFunction("runtime.line_index", Arrays.asList(expr, new VariableReference(lineNumbers))));
            model.ifStmt(test, sb, null);
        }
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
        var linenum = Integer.parseInt(ctx.l.getText());
        model.gotoGosubStmt("goto", gotoGosubLabel(linenum));
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
        if (ctx.prompt != null) {
            String value = ctx.prompt.getText().replaceAll("^\"|\"$", "");
            model.callLibrarySubroutine("print_string", new StringConstant(model.fixControlChars(value)));
        }
        if (!ctx.getText().contains("$")) {
            // ? prompt is always shown for number input AFAICT.
            model.callLibrarySubroutine("print_string", new StringConstant("?"));
        }
        model.callLibrarySubroutine("input_readline");
        for (var avar : ctx.var()) {
            var expr = visit(avar);
            if (expr instanceof VariableReference varRef) {
                switch (varRef.getType()) {
                    case INTEGER -> model.assignStmt(varRef, model.callFunction("runtime.input_scaninteger", Collections.emptyList()));
                    case STRING ->  model.callLibrarySubroutine("input_scanstring", varRef);
                    default -> throw new RuntimeException("input statement not implemented yet: " + varRef.getType());
                }
            }
            else {
                throw new RuntimeException("unknown variable type: " + avar);
            }
        }
        return null;
    }

    @Override
    public Expression visitIntegerAssignment(IntegerParser.IntegerAssignmentContext ctx) {
        var expr = visit(ctx.iexpr());
        var ivar = visit(ctx.ivar());
        if (ivar instanceof VariableReference ref) {
            model.assignStmt(ref, expr);
        }
        else if (ivar instanceof UnaryExpression unary && "*".equals(unary.getOp())) {
            model.assignStmt(unary, expr);
        }
        else {
            throw new RuntimeException("unknown variable type: " + ivar);
        }
        return null;
    }

    /**
     * Handle string assignments. An attempt is made to optimize the generated code a little
     * bit since the string copy routine takes all the expected indexes as parameters, rather
     * than generating substring parsing code elsewhere.
     */
    @Override
    public Expression visitStringAssignment(IntegerParser.StringAssignmentContext ctx) {
        var srefExpr = visit(ctx.sref());       // LHS
        var stringExpr = visit(ctx.sexpr());    // RHS
        // strcpy arguments
        Expression targetVariable;
        Expression targetStart = IntegerConstant.ONE;
        // TODO? sourceStart and sourceEnd don't actually make it this far for A$(n) = B$(p,q)
        Expression sourceStart = IntegerConstant.ONE;
        Expression sourceEnd = IntegerConstant.ZERO;
        if (srefExpr instanceof VariableReference sref) {
            targetVariable = VariableReference.with(sref.getSymbol());
        } else if (srefExpr instanceof FunctionExpression fref && LHS_STRING_NAME.equals(fref.getName())) {
            var params = fref.getParameters();
            targetVariable = params.get(0);
            targetStart = params.get(1);
        }
        else {
            throw new RuntimeException("unknown assignment variable type: " + srefExpr);
        }
        if (stringExpr instanceof StringConstant str) {
            sourceEnd = new IntegerConstant(str.getValue().length());
        }
        model.callLibrarySubroutine("strcpy", targetVariable, targetStart, stringExpr, sourceStart, sourceEnd);
        return null;
    }

    @Override
    public Expression visitListStatement(IntegerParser.ListStatementContext ctx) {
        System.out.println("LIST not supported; ignoring it.");
        return null;
    }

    @Override
    public Expression visitLomemStatement(IntegerParser.LomemStatementContext ctx) {
        var expr = visit(ctx.addr);
        if (expr.isConstant() && expr.asInteger().isPresent()) {
            int addr = expr.asInteger().orElseThrow();
            if (addr >= 0x803 && addr < 0x1000) {   // totally arbitrary.
                System.err.println("WARNING: LOMEM indicates this application may over write the $803 location.");
            }
        }
        else {
            System.out.println("LOMEM not supported; ignoring it.");
        }
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
        // Assuming that as long as we are in the MAIN (Program) level scope, we are clear.
        // This means that we are NOT in a Function or Subroutine. But we may have GOSUBs on
        // the stack.
        if (model.isCurrentScope(Program.class)) {
            model.addStatement(new PopStatement());
            return null;
        }
        else {
            throw new RuntimeException("POP statement only supported in main program.");
        }
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
                callSubroutine("print_comma");
            }
            else {
                Expression expr = pt.accept(this);
                switch (expr.getType()) {
                    case INTEGER -> model.callLibrarySubroutine("print_integer", expr);
                    case STRING -> model.callLibrarySubroutine("print_string", expr);
                    default -> throw new RuntimeException("Unsupported PRINT type: " + expr.getType());
                }
            }
        }
        if (!semiColonAtEnd) {
            callSubroutine("print_newline");
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

        if ("^".equals(op)) {
            return model.callFunction("ipow", Arrays.asList(left,right));
        }
        if ("#".equals(op)) {
            op = "<>";
        }
        else if ("or".equalsIgnoreCase(op) || "and".equalsIgnoreCase(op)) {
            // Ensure we have Boolean lhs and rhs expression.
            if (left.isType(DataType.INTEGER)) {
                left = left.eq(IntegerConstant.ONE);
            }
            if (right.isType(DataType.INTEGER)) {
                right = right.eq(IntegerConstant.ONE);
            }
        }
        return new BinaryExpression(left, right, op);
    }

    /**
     * String comparison only supports equals a not equals.
     */
    @Override
    public Expression visitBinaryStrExpr(IntegerParser.BinaryStrExprContext ctx) {
        var left = visit(ctx.left);
        var right = visit(ctx.right);
        var op = ctx.op.getText();

        if ("#".equals(op)) {
            op = "<>";
        }
        return new BinaryExpression(
                model.callFunction("strings.strcmp", Arrays.asList(left,right)),
                IntegerConstant.ZERO,
                op);
    }

    @Override
    public Expression visitUnaryIntExpr(IntegerParser.UnaryIntExprContext ctx) {
        var op = ctx.op.getText();
        var e = visit(ctx.e);
        if ("not".equalsIgnoreCase(op)) {
            // The backend treats NOT(integer) as an XOR to flip all bits (following VB as model)
            // So do an equality test instead.
            return e.eq(IntegerConstant.ZERO);
        }
        return new UnaryExpression(op, e);
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
        return visit(ctx.e);
    }

    @Override
    public Expression visitStrConstExpr(IntegerParser.StrConstExprContext ctx) {
        String value = ctx.value.getText().replaceAll("^\"|\"$", "");
        return new StringConstant(model.fixControlChars(value));
    }

    @Override
    public Expression visitIntArgFunc(IntegerParser.IntArgFuncContext ctx) {
        var f = ctx.f.getText();
        var e = visit(ctx.e);
        return model.callFunction(f, e);
    }

    @Override
    public Expression visitStrArgFunc(IntegerParser.StrArgFuncContext ctx) {
        var f = ctx.f.getText();
        var s = visit(ctx.s);
        return model.callFunction(f, s);
    }

    @Override
    public Expression visitScrnFunc(IntegerParser.ScrnFuncContext ctx) {
        var x = visit(ctx.x);
        var y = visit(ctx.y);
        return model.callFunction("lores.scrn", Arrays.asList(x, y));
    }

    @Override
    public Expression visitStrRef(IntegerParser.StrRefContext ctx) {
        // Note (LHS): A$(start)
        var ref = model.addVariable(ctx.n.getText(), DataType.STRING);
        stringsDimmed.computeIfAbsent(ref, k -> null);
        if (ctx.start != null) {
            var start = visit(ctx.start);
            return new FunctionExpression(LHS_STRING, List.of(VariableReference.with(ref), start));
        }
        return VariableReference.with(ref);
    }

    @Override
    public Expression visitIntAryVar(IntegerParser.IntAryVarContext ctx) {
        var ref = model.addArrayVariable(ctx.n.getText(), DataType.INTEGER, 1);
        var expr = visit(ctx.e);
        model.checkArrayBounds(ref, expr, ctx.getStart().getLine());
        return arrayReference(ref, expr);
    }

    @Override
    public Expression visitIntVar(IntegerParser.IntVarContext ctx) {
        var ref = model.addVariable(ctx.n.getText(), DataType.INTEGER);
        return new VariableReference(ref);
    }

    @Override
    public Expression visitStrVar(IntegerParser.StrVarContext ctx) {
        // Note (RHS): A$ [ (start [,end] ) ]
        var ref = model.addVariable(ctx.n.getText(), DataType.STRING);
        stringsDimmed.computeIfAbsent(ref, k -> null);
        if (ctx.start != null) {
            var start = visit(ctx.start);
            if (ctx.end != null) {
                var end = visit(ctx.end);
                var distance = distance(start, end);
                var tempRef = findKnownSizeTempStringVariable(distance);
                model.callLibrarySubroutine("strcpy", VariableReference.with(tempRef), IntegerConstant.ONE,
                        VariableReference.with(ref), start, end);
                return VariableReference.with(tempRef);
            }
            var tempRef = findUnknownSizeTempStringVariable(ref);   // Note we actually ignore start for this
            model.callLibrarySubroutine("strcpy", VariableReference.with(tempRef), IntegerConstant.ONE,
                    VariableReference.with(ref), start, IntegerConstant.ZERO);
            return VariableReference.with(tempRef);
        }
        return VariableReference.with(ref);
    }

    public Symbol findUnknownSizeTempStringVariable(Symbol sourceSymbol) {
        Symbol tempVar = null;
        for (var symbol : unknownSizetempStringVariables.keySet()) {
            if (!tempStringVariablesInUse.contains(symbol)) {
                tempVar = symbol;
                break;
            }
        }
        if (tempVar == null) {
            tempVar = model.addTempVariable(DataType.STRING);
        }
        unknownSizetempStringVariables.compute(tempVar, (k,v) -> {
            if (v == null) {
                v = new HashSet<>();
            }
            v.add(sourceSymbol);
            return v;
        });
        tempStringVariablesInUse.add(tempVar);
        return tempVar;
    }

    public Symbol findKnownSizeTempStringVariable(int distance) {
        Symbol tempVar = null;
        for (var symbol : knownSizeTempStringVariables.keySet()) {
            if (!tempStringVariablesInUse.contains(symbol)) {
                tempVar = symbol;
                break;
            }
        }
        if (tempVar == null) {
            tempVar = model.addTempVariable(DataType.STRING);
        }
        knownSizeTempStringVariables.compute(tempVar, (k,v) -> (v == null) ? distance : Math.max(v,distance));
        tempStringVariablesInUse.add(tempVar);
        return tempVar;
    }

    public int distance(Expression start, Expression end) {
        if (start.isConstant() && end.isConstant()) {
            // (end-start)+1
            return end.minus(start).plus(IntegerConstant.ONE).asInteger().orElseThrow();
        }
        if (start.equals(end)) {
            return 1;
        }
        if (model.isUsingMemory()) {
            return 255;     // largest string supported, but only if we are using heap
        }
        throw new RuntimeException(String.format("cannot evaluate (%s - %s) + 1", end, start));
    }
}
