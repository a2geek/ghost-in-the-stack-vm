package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.PreprocessorGrammar;
import a2geek.ghost.antlr.generated.PreprocessorGrammarBaseVisitor;
import a2geek.ghost.antlr.generated.PreprocessorLexer;
import a2geek.ghost.model.CompilerConfiguration;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.expression.BinaryExpression;
import a2geek.ghost.model.expression.BooleanConstant;
import a2geek.ghost.model.expression.IntegerConstant;
import a2geek.ghost.model.expression.StringConstant;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static a2geek.ghost.TrackingLogger.LOGGER;

public class PreprocessorVisitor extends PreprocessorGrammarBaseVisitor<Expression> {
    public static final Predicate<String> OPTION_REGEX = Pattern.compile(".*option[^\r\n]+heap.+").asPredicate();
    private final Map<String,Expression> variables = new HashMap<>();
    private final StringBuilder code = new StringBuilder();
    /** Tracking code capture state as a simple boolean. */
    private boolean capture = true;
    /**
     * Track which <code>#if</code> directive was true. We only need to know that
     * one was found, but error messages might benefit from the text.
     */
    private String ifDirective = null;

    public PreprocessorVisitor(CompilerConfiguration config) {
        this.variables.putAll(config.getDefines());
    }

    public String getCode() {
        return code.toString();
    }

    @Override
    public Expression visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType() == PreprocessorLexer.CODE) {
            if (OPTION_REGEX.test(node.getText()))  {
                variables.put(CompilerConfiguration.OPTION_HEAP, new BooleanConstant(true));
            }
            if (capture) {
                code.append(node.getText());
            }
        }
        return super.visitTerminal(node);
    }

    // Directives

    @Override
    public Expression visitDefineDirective(PreprocessorGrammar.DefineDirectiveContext ctx) {
        var id = ctx.ID().getText();
        var expr = visit(ctx.expr());
        variables.putIfAbsent(id, expr);
        return null;
    }

    @Override
    public Expression visitIfDirective(PreprocessorGrammar.IfDirectiveContext ctx) {
        var e = visit(ctx.expr());
        capture = false;
        e.asBoolean().ifPresent(b -> {
            capture = b;
            if (b) {
                ifDirective = ctx.getText();
            }
        });
        return null;
    }

    @Override
    public Expression visitElseIfDirective(PreprocessorGrammar.ElseIfDirectiveContext ctx) {
        var e = visit(ctx.expr());
        capture = false;
        e.asBoolean().ifPresent(b -> {
            capture = b;
            if (b) {
                ifDirective = ctx.getText();
            }
        });
        return null;
    }

    @Override
    public Expression visitElseDirective(PreprocessorGrammar.ElseDirectiveContext ctx) {
        capture = false;
        if (ifDirective == null) {
            ifDirective = ctx.getText();
            capture = true;
        }
        return null;
    }

    @Override
    public Expression visitEndIfDirective(PreprocessorGrammar.EndIfDirectiveContext ctx) {
        capture = true;
        ifDirective = null;
        return null;
    }

    // Expressions

    @Override
    public Expression visitComparisonExpr(PreprocessorGrammar.ComparisonExprContext ctx) {
        var op = ctx.op.getText();
        if ("!=".equals(op)) {
            op = "<>";
        }
        var l = visit(ctx.l);
        var r = visit(ctx.r);
        return new BinaryExpression(l, r, op);
    }

    @Override
    public Expression visitIdOrFnExpr(PreprocessorGrammar.IdOrFnExprContext ctx) {
        var id = ctx.ID().getText();
        if (id.equalsIgnoreCase("defined")) {
            if (ctx.expr() == null) {
                LOGGER.errorf("defined function requires an ID");
                return null;
            }
            else {
                return new BooleanConstant(variables.containsKey(ctx.expr().getText()));
            }
        }
        if (variables.containsKey(id)) {
            return variables.get(id);
        }
        LOGGER.errorf("ID '%s' is not defined", id);
        return null;
    }

    @Override
    public Expression visitIntegerExpr(PreprocessorGrammar.IntegerExprContext ctx) {
        return new IntegerConstant(Integer.parseInt(ctx.getText()));
    }

    @Override
    public Expression visitBooleanExpr(PreprocessorGrammar.BooleanExprContext ctx) {
        return new BooleanConstant(Boolean.parseBoolean(ctx.getText()));
    }

    @Override
    public Expression visitStringExpr(PreprocessorGrammar.StringExprContext ctx) {
        return new StringConstant(ctx.getText());
    }
}
