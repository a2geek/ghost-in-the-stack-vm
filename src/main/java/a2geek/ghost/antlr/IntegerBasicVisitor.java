package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.IntegerBaseVisitor;
import a2geek.ghost.antlr.generated.IntegerParser;
import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.ForFrame;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.statement.EndStatement;
import a2geek.ghost.model.statement.IfStatement;
import a2geek.ghost.model.statement.PopStatement;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

public class IntegerBasicVisitor extends IntegerBaseVisitor<Expression> {
    public static final String LINE_NUMBERS = "_line_numbers";

    private ModelBuilder model;
    private SortedMap<Integer,Symbol> lineLabels = new TreeMap<>();
    private Map<Symbol, ForFrame> forFrames = new HashMap<>();

    public IntegerBasicVisitor(ModelBuilder model) {
        this.model = model;
        model.setArrayNameStrategy(s -> s + "()");
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
        // Note that the array name gets mangled to keep types distinct by name
        model.findSymbol(model.fixArrayName(LINE_NUMBERS)).ifPresent(symbol -> {
            model.insertDimArray(symbol, new IntegerConstant(lineLabels.size()),
                    lineLabels.keySet().stream()
                            .map(IntegerConstant::new)
                            .map(Expression.class::cast)
                            .toList());
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
                n -> model.addLabels(String.format("L%d_", n)).get(0));
    }

    @Override
    public Expression visitProgramLine(IntegerParser.ProgramLineContext ctx) {
        var lineNumber = Integer.parseInt(ctx.INTEGER().getText());
        var lineLabel = gotoGosubLabel(lineNumber);

        model.labelStmt(lineLabel);
        try {
            visit(ctx.statements());
        } catch (Exception ex) {
            throw new RuntimeException("Error in line " + lineNumber, ex);
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
        model.addDimArray(symbol, expr, null);
        return null;
    }

    @Override
    public Expression visitStrDimVar(IntegerParser.StrDimVarContext ctx) {
        throw new RuntimeException("strings not supported");
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
        model.assignStmt(nextRef, new BinaryExpression(new AddressOfFunction(nextLabel), IntegerConstant.ONE, "-"));
        model.gotoGosubStmt("goto", loopLabel);

        // handle loop increment and test
        model.labelStmt(nextLabel);
        model.assignStmt(varRef, new BinaryExpression(varRef, stepRef, "+"));
        model.pushStatementBlock(new StatementBlock());
        model.gotoGosubStmt("goto", loopLabel);
        var sb = model.popStatementBlock();
        var positive = new IfStatement(new BinaryExpression(varRef, endRef, "<="), sb, null);
        var negative = new IfStatement(new BinaryExpression(varRef, endRef, ">="), sb, null);
        model.ifStmt(new BinaryExpression(model.callFunction("SGN", Arrays.asList(step)), IntegerConstant.ZERO, ">="),
                StatementBlock.with(positive), StatementBlock.with(negative));
        model.dynamicGotoGosubStmt("goto", VariableReference.with(frame.getExitRef()));

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
            var exitLabel = labels.get(0);

            model.assignStmt(exitRef, new BinaryExpression(new AddressOfFunction(exitLabel), IntegerConstant.ONE, "-"));
            model.dynamicGotoGosubStmt("goto", VariableReference.with(frame.getNextRef()));
            model.labelStmt(exitLabel);
        }
        return null;
    }

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
            var lineNumbers = model.addArrayVariable(LINE_NUMBERS, DataType.INTEGER, 1);
            var text = String.format("%s %s", op.toUpperCase(), expr);
            var lookupExpr = model.callFunction("line_index",
                Arrays.asList(expr, new VariableReference(lineNumbers)));
            model.onGotoGosubStmt(op, lookupExpr, () -> lineLabels.values().stream().toList(), text);
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
            model.callLibrarySubroutine("string", new StringConstant(model.fixControlChars(value)));
        }
        else {
            model.callLibrarySubroutine("string", new StringConstant("?"));
        }
        for (var avar : ctx.var()) {
            var expr = visit(avar);
            if (expr instanceof VariableReference varRef) {
                if (varRef.getType() == DataType.INTEGER) {
                    model.assignStmt(varRef, model.callFunction("integer", Collections.emptyList()));
                }
                else {
                    throw new RuntimeException("string input statement not implemented yet");
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
        else {
            throw new RuntimeException("unknown variable type: " + ivar);
        }
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

        if ("^".equals(op)) {
            return model.callFunction("ipow", Arrays.asList(left,right));
        }
        if ("#".equals(op)) {
            op = "<>";
        }
        return new BinaryExpression(left, right, op);
    }

    @Override
    public Expression visitUnaryIntExpr(IntegerParser.UnaryIntExprContext ctx) {
        var op = ctx.op.getText();
        var e = visit(ctx.e);
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
        var ref = model.addArrayVariable(ctx.n.getText(), DataType.INTEGER, 1);
        var expr = visit(ctx.e);
        model.checkArrayBounds(ref, expr, ctx.getStart().getLine());
        return new VariableReference(ref, Arrays.asList(expr));
    }

    @Override
    public Expression visitIntVar(IntegerParser.IntVarContext ctx) {
        var ref = model.addVariable(ctx.n.getText(), DataType.INTEGER);
        return new VariableReference(ref);
    }

    @Override
    public Expression visitStrVar(IntegerParser.StrVarContext ctx) {
        throw new RuntimeException("strings not supported yet");
    }
}
