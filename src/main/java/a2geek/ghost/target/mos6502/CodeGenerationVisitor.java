package a2geek.ghost.target.mos6502;

import a2geek.asm.api.util.LineParts;
import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;
import a2geek.ghost.model.scope.Function;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.scope.Subroutine;
import a2geek.ghost.model.statement.*;
import a2geek.ghost.target.Frame;

import java.util.*;

import static a2geek.ghost.Util.errorf;
import static a2geek.ghost.model.Symbol.in;
import static a2geek.ghost.model.Symbol.named;
import static a2geek.ghost.target.mos6502.ExpressionGenerators.*;
import static a2geek.ghost.target.mos6502.TempSuppliers.symbolSupplier;
import static a2geek.ghost.target.mos6502.TempSuppliers.terminalSupplier;
import static a2geek.ghost.target.mos6502.Terminals.*;

public class CodeGenerationVisitor extends DispatchVisitor<ExpressionGenerator> {
    public static final String INITIAL_SP = "_INITSP";
    public static final String REG_A = "_REGA";
    public static final String REG_X = "_REGX";
    public static final String REG_Y = "_REGY";
    public static final String IF_TEMP = "_IFTEMP";
    public static final String FRAME_PTR = "_FRAMEPTR";

    private final Stack<Frame> frames = new Stack<>();
    private AssemblyWriter code = new AssemblyWriter();
    private int labelNumber;
    private final Set<String> scopesUsed = new HashSet<>();

    // frame overhead: return address (2 bytes) + stack index (1 byte)
    private final int frameOverhead = DataType.ADDRESS.sizeof() + DataType.BYTE.sizeof();

    public List<LineParts> getInstructions() {
        return code.getInstructions();
    }

    List<String> label(final String... names) {
        List<String> labels = new ArrayList<>();
        labelNumber++;
        for (String name : names) {
            labels.add(String.format("_%s%d", name, labelNumber));
        }
        return labels;
    }

    public void emitStore(Symbol symbol) {
        switch (symbol.symbolType()) {
            case VARIABLE, PARAMETER, RETURN_VALUE -> {
                switch (symbol.declarationType()) {
                    case LOCAL -> {
                        code.LDX(FRAME_PTR)
                            .PLA()
                            .STA("stackA,x")
                            .PLA()
                            .STA("stackA+1,x");
                    }
                    case GLOBAL -> {
                        code.PLA()
                            .STA("%s", symbol.name())
                            .PLA()
                            .STA("%s+1", symbol.name());
                    }
                    case INTRINSIC -> {
                        code.PLA()
                            .STA(getRegName(symbol))
                            .PLA(); // toss lass byte away
                    }
                }
            }
            case CONSTANT -> throw errorf("cannot store to a constant value: %s", symbol.name());
        }
    }
    String getRegName(Symbol symbol) {
        return switch (symbol.name().toLowerCase()) {
            case Intrinsic.CPU_REGISTER_A -> REG_A;
            case Intrinsic.CPU_REGISTER_X -> REG_X;
            case Intrinsic.CPU_REGISTER_Y -> REG_Y;
            default -> throw errorf("unknown intrinsic variable '%s'", symbol.name());
        };
    }

    @Override
    public void visit(Program program) {
        var frame = this.frames.push(Frame.create(program));
        // byte global variables
        List.of(INITIAL_SP, REG_A, REG_X, REG_Y, FRAME_PTR, IF_TEMP).forEach(name -> {
            program.addLocalSymbol(Symbol.variable(name, SymbolType.VARIABLE)
                   .dataType(DataType.BYTE)
                   .declarationType(DeclarationType.GLOBAL));
        });

        code.cpu("<MOS6502>")
            .assign("ptr", "$00")
            .assign("stack", "$100")
            .assign("stackA", "stack+1")
            .org(0x803)
            .TSX()
            .STX(INITIAL_SP);

        super.visit(program);

        for (var s : program.getLocalSymbols()) {
            if (s.declarationType() == DeclarationType.GLOBAL && s.dataType() != null) {
                switch (s.dataType().sizeof()) {
                    case 1 -> code.byteVar(s.name(), "0");
                    case 2 -> code.wordVar(s.name(), "0");
                    default -> throw errorf("unexpected data size for '%s'", s);
                }
            }
        }
    }

    @Override
    public void visit(Subroutine subroutine) {
        // TODO handle details
        var exitLabel = label("SUBEXIT").getFirst();
        subroutine.setExitLabel(exitLabel);
//        var hasLocalScope = !subroutine.findAllLocalScope(is(DeclarationType.LOCAL).and(in(SymbolType.VARIABLE, SymbolType.PARAMETER))).isEmpty();
        var frame = frames.push(Frame.create(subroutine, frameOverhead));
        code.label(subroutine.getFullPathName());
//        if (hasLocalScope) setupLocalFrame(frame);
//        saveOnErrContext(subroutine);
//        setupDefaultArrayValues(subroutine);
//        setupDefaultStringValues(subroutine);
        if (subroutine.getStatements() != null) {
            dispatchAll(subroutine, subroutine);
        }
        code.label(exitLabel);
//        if (hasLocalScope) tearDownLocalFrame(frame);
//        restoreOnErrContext(subroutine);
//        if (frame.parameterSize() > 0) {
//            code.emit(Opcode.RETURNN, frame.parameterSize());
//        }
//        else {
//            code.emit(Opcode.RETURN);
//        }
        code.RTS();
        frames.pop();
    }

    @Override
    public void visit(Function function) {
        // TODO handle details
        var frame = frames.push(Frame.create(function, frameOverhead));
        var labels = label("FUNCXIT");
        var exitLabel = labels.getFirst();
        function.setExitLabel(exitLabel);
        code.label(function.getFullPathName());
//        setupLocalFrame(frame);
//        saveOnErrContext(function);
//        setupDefaultArrayValues(function);
//        setupDefaultStringValues(function);
        if (function.getStatements() != null) {
            dispatchAll(function, function);
        }
        code.label(exitLabel);
//        tearDownLocalFrame(frame);
//        restoreOnErrContext(function);
//        code.emit(Opcode.RETURNN, frame.parameterSize());
        code.RTS();
        frames.pop();

    }

    @Override
    public void visit(AssignmentStatement statement, VisitorContext context) {
        // TODO handle constants and simple assignment better!
        //      eg: A=5, A=B
        //      eg: (*-16368)= 0  (aka poke -16368,0),
        //          (*A)=B
        var value = dispatch(statement.getValue()).orElseThrow();
        if (statement.getVar() instanceof VariableReference ref) {
            // A = <value> (simple assignment)
            code.comment("%s = %s", ref.getSymbol().name(), statement.getValue());
            assign(value).toTerminal(code, symbolSupplier(ref.getSymbol()));
        }
        else if (statement.getVar() instanceof DereferenceOperator deref) {
            // *(<expr>) = <value>   (dereferenced assignment)
            code.comment("*(%s) = %s", deref.getExpr(), statement.getValue());
            if (deref.getExpr().isConstant()) {
                assign(value).toTerminal(code, terminalSupplier(addressReference(
                        deref.getExpr().asInteger().orElseThrow(),
                        deref.getType().sizeof())));
            }
            else {
                var addr = dispatch(deref.getExpr()).orElseThrow();
                assign(addr).toTerminal(code, terminalSupplier(labelReference("PTR")));
                assign(value).toTerminal(code, terminalSupplier(indyReference("PTR")));
            }
        }
        else {
            throw errorf("not valid assignment: %s", statement);
        }
    }

    @Override
    public void visit(EndStatement statement, VisitorContext context) {
        // BASIC "END" can be anywhere, so this is an attempt for a clean exit.
        code.comment("END");
        code.LDX(INITIAL_SP)
            .TXS()
            .RTS();
    }

    @Override
    public void visit(CallStatement statement, VisitorContext context) {
        code.comment("CALL %s", statement.getExpr());
        if (statement.getExpr().isConstant()) {
            code.LDA(REG_A)
                .LDX(REG_X)
                .LDY(REG_Y)
                .JSR("$%04X", statement.getExpr().asInteger().orElseThrow())
                .STA(REG_A)
                .STX(REG_X)
                .STY(REG_Y);
        }
        else {
            var labels = label("jmptgt");
            var jmptgt = labels.getFirst();
            code.PLA()
                .STA("%s+1", jmptgt)
                .PLA()
                .STA("%s+2", jmptgt)
                .LDA(REG_A)
                .LDX(REG_X)
                .LDY(REG_Y)
                .label(jmptgt)
                .JSR("$FFFF")
                .STA(REG_A)
                .STX(REG_X)
                .STY(REG_Y);
        }
    }

    @Override
    public void visit(IfStatement statement, VisitorContext context) {
        if (!statement.hasTrueStatements() && !statement.hasFalseStatements()) {
            // Do not generate code if this is somehow an empty IF statement
            return;
        }

        var labels = label("IF_TRUE", "IF_ELSE", "IF_EXIT");
        var trueLabel = labels.get(0);
        var elseLabel = labels.get(1);
        var exitLabel = labels.get(2);

        dispatch(statement.getExpression());
        if (statement.hasTrueStatements()) {
            code.comment("IF %s THEN", statement.getExpression());
            code.PLA()
                .STA(IF_TEMP)
                .PLA()
                .ORA(IF_TEMP)
                .BNE(trueLabel)
                .JMP(statement.hasFalseStatements() ? elseLabel : exitLabel)
                .label(trueLabel);
            dispatchAll(context, statement.getTrueStatements());
            if (statement.hasFalseStatements()) {
                code.comment("ELSE");
                code.JMP(exitLabel);
                code.label(elseLabel);
                dispatchAll(context, statement.getFalseStatements());
            }
        }
        else {
            code.comment("IF NOT %s THEN", statement.getExpression());
            code.PLA()
                .STA(IF_TEMP)
                .PLA()
                .ORA(IF_TEMP)
                .BNE(elseLabel)
                .JMP(exitLabel)
                .label(elseLabel);
            dispatchAll(context, statement.getFalseStatements());
        }
        code.comment("END IF");
        code.label(exitLabel);
    }

    @Override
    public void visit(GotoGosubStatement statement, VisitorContext context) {
        code.comment("%s %s", statement.getOp().toUpperCase(), statement.getLabel().name());
        switch (statement.getOp()) {
            case "goto" -> code.JMP(statement.getLabel().name());
            case "gosub" -> code.JSR(statement.getLabel().name());
            default -> throw errorf("unknown goto/gosub statement type: %s", statement.getOp());
        }
    }

    @Override
    public void visit(DynamicGotoGosubStatement statement, VisitorContext context) {
        // TODO
    }

    @Override
    public void visit(LabelStatement statement, VisitorContext context) {
        code.label(statement.getLabel().name());
    }

    @Override
    public void visit(ReturnStatement statement, VisitorContext context) {
        boolean hasReturnValue = statement.getExpr() != null;
        if (this.frames.peek().scope() instanceof Function f) {
            var refs = this.frames.peek().scope().findAllLocalScope(in(SymbolType.RETURN_VALUE));
            if (refs.size() == 1 && hasReturnValue) {
                dispatch(statement.getExpr());
                emitStore(refs.getFirst());
            } else if (!refs.isEmpty() || hasReturnValue) {
                throw errorf("function return mismatch");
            }
            code.JMP(f.getExitLabel());
        }
        else if (hasReturnValue) {
            throw errorf("cannot return value from a subroutine: %s", this.frames.peek().scope().getName());
        }
        else if (this.frames.peek().scope() instanceof Subroutine s) {
            code.JMP(s.getExitLabel());
        }
        else {
            code.RTS();
        }
    }

    @Override
    public void visit(CallSubroutine statement, VisitorContext context) {
        // Using the "program" frame
        var subFullName = statement.getSubroutine().getFullPathName();
        var scope = frames.getFirst().scope()
                .findFirstLocalScope(named(subFullName).and(in(SymbolType.SUBROUTINE)))
                .map(Symbol::scope)
                .orElseThrow(() -> new RuntimeException(subFullName + " not found"));
        if (scope instanceof Subroutine sub) {
            scopesUsed.add(sub.getFullPathName());
            for (Expression expr : statement.getParameters()) {
                dispatch(expr);
            }
            code.JSR(sub.getFullPathName());
            return;
        }
        throw new RuntimeException(String.format("calling a subroutine but '%s' is not a subroutine?", subFullName));
    }

    @Override
    public void visit(PopStatement statement, VisitorContext context) {
        // pop return address from GOSUB/JSR off of stack (2 bytes)
        code.PLA()
            .PLA();
    }

    @Override
    public void visit(OnErrorStatement statement, VisitorContext context) {
        // TODO
    }

    @Override
    public void visit(RaiseErrorStatement statement, VisitorContext context) {
        // TODO
    }

    @Override
    public ExpressionGenerator visit(AddressOfOperator expression) {
        // TODO
        return intConstant(0xe5);
    }

    @Override
    public ExpressionGenerator visit(ArrayLengthFunction expression) {
        // TODO
        return intConstant(0xe5);
    }

    @Override
    public ExpressionGenerator visit(BinaryExpression expression) {
        var l = dispatch(expression.getL()).orElseThrow();
        var r = dispatch(expression.getR()).orElseThrow();
        return switch (expression.getOp()) {
            case "+" -> addSub(l, r, "CLC", "ADC");
            case "-" -> addSub(l, r, "SEC", "SBC");
            // TODO
            default -> intConstant(0xe5);
        };
    }

    @Override
    public ExpressionGenerator visit(BooleanConstant expression) {
        // TODO ... we just treat a Boolean as a WORD value
        return intConstant(expression.asInteger().orElseThrow());
    }

    @Override
    public ExpressionGenerator visit(ByteConstant expression) {
        // TODO ... we just treat a Byte as a WORD value
        return intConstant(expression.asInteger().orElseThrow());
    }

    @Override
    public ExpressionGenerator visit(DereferenceOperator expression) {
        // TODO
        return intConstant(0xe5);
    }

    @Override
    public ExpressionGenerator visit(FunctionExpression expression) {
        // TODO
        return intConstant(0xe5);
    }

    @Override
    public ExpressionGenerator visit(IfExpression expression) {
        // TODO
        return intConstant(0xe5);
    }

    @Override
    public ExpressionGenerator visit(IntegerConstant expression) {
        return intConstant(expression.getValue());
    }

    @Override
    public ExpressionGenerator visit(PlaceholderExpression expression) {
        // TODO
        return intConstant(0xe5);
    }

    @Override
    public ExpressionGenerator visit(StringConstant expression) {
        var label = label("STRCONST");
        String actual = code.stringConstant(label.getFirst(), expression.getValue());
        return labelReference(actual);
    }

    @Override
    public ExpressionGenerator visit(TypeConversionOperator expression) {
        // TODO we don't really have "real" type conversions yet
        return dispatch(expression.getExpr()).orElseThrow();
    }

    @Override
    public ExpressionGenerator visit(UnaryExpression expression) {
        var expr = dispatch(expression.getExpr()).orElseThrow();
        return switch (expression.getOp().toLowerCase()) {
            case "-" -> negate(expr);
            case "not" -> {
                var mask = switch(expression.getType()) {
                    case INTEGER -> 0xffff;
                    case BOOLEAN -> 0;
                    default -> throw new RuntimeException("cannot only perform a NOT on a Boolean or Integer: " + expression);
                };
                yield not(expr, mask);
            }
            default -> throw errorf("unknown unary operator: '%s'", expression.getOp());
        };
    }

    @Override
    public ExpressionGenerator visit(VariableReference expression) {
        switch (expression.getSymbol().symbolType()) {
            case VARIABLE, PARAMETER, RETURN_VALUE -> {
                return switch (expression.getSymbol().declarationType()) {
                    case LOCAL -> localVariable(expression.getSymbol());
                    case GLOBAL -> globalVariable(expression.getSymbol());
                    case INTRINSIC -> intrinsicVariable(expression.getSymbol());
                };
            }
            case CONSTANT -> {
                return switch (expression.getSymbol().defaultValues().getFirst()) {
                    case IntegerConstant c -> visit(c);
                    case StringConstant s -> visit(s);
                    default -> throw errorf("unable to generate code for constant expression '%s'", expression.getSymbol());
                };
            }
            default -> throw errorf("unexpected symbol type '%s'", expression.getSymbol().symbolType());
        }
    }
}
