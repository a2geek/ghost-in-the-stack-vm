package a2geek.ghost.antlr;

import a2geek.ghost.antlr.generated.BasicBaseVisitor;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.model.Expression;

public abstract class BasicBaseVisitorWrapper extends BasicBaseVisitor<Expression> {
    public abstract void enterExpression();
    public abstract Expression exitExpression(Expression expr);

    @Override
    public Expression visitIfShortExpr(BasicParser.IfShortExprContext ctx) {
        enterExpression();
        return exitExpression(super.visitIfShortExpr(ctx));
    }

    @Override
    public Expression visitArrayOrFunctionRef(BasicParser.ArrayOrFunctionRefContext ctx) {
        enterExpression();
        return exitExpression(super.visitArrayOrFunctionRef(ctx));
    }

    @Override
    public Expression visitBinaryExpr(BasicParser.BinaryExprContext ctx) {
        enterExpression();
        return exitExpression(super.visitBinaryExpr(ctx));
    }

    @Override
    public Expression visitUnaryExpr(BasicParser.UnaryExprContext ctx) {
        enterExpression();
        return exitExpression(super.visitUnaryExpr(ctx));
    }

    @Override
    public Expression visitParenExpr(BasicParser.ParenExprContext ctx) {
        enterExpression();
        return exitExpression(super.visitParenExpr(ctx));
    }

    @Override
    public Expression visitBoolConstant(BasicParser.BoolConstantContext ctx) {
        enterExpression();
        return exitExpression(super.visitBoolConstant(ctx));
    }

    @Override
    public Expression visitIntConstant(BasicParser.IntConstantContext ctx) {
        enterExpression();
        return exitExpression(super.visitIntConstant(ctx));
    }

    @Override
    public Expression visitStringConstant(BasicParser.StringConstantContext ctx) {
        enterExpression();
        return exitExpression(super.visitStringConstant(ctx));
    }

    @Override
    public Expression visitVariableOrFunctionRef(BasicParser.VariableOrFunctionRefContext ctx) {
        enterExpression();
        return exitExpression(super.visitVariableOrFunctionRef(ctx));
    }
}
