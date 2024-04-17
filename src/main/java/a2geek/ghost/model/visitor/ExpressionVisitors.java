package a2geek.ghost.model.visitor;

import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.expression.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This class contains simple visitors that are implemented in a single recursive method.
 */
public class ExpressionVisitors {
    private ExpressionVisitors() {
        // prevent construction
    }

    /**
     * Determine if the expression uses symbol.
     */
    public static boolean hasSymbol(Expression expr, Symbol symbol) {
        return captureSymbols(expr).contains(symbol);
    }

    /**
     * Determine if the expression uses any type of array symbol.
     */
    public static boolean hasAnyArraySymbol(Expression expr) {
        return captureSymbols(expr).stream().anyMatch(s -> s.numDimensions() > 0);
    }

    /**
     * Capture all symbols in the given expression.
     */
    public static Set<Symbol> captureSymbols(Expression expr) {
        Set<Symbol> symbols = new HashSet<>();
        switch (expr) {
            case AddressOfOperator addrOf -> symbols.add(addrOf.getSymbol());
            case ArrayLengthFunction arrayLen -> symbols.add(arrayLen.getSymbol());
            case BinaryExpression bin -> {
                symbols.addAll(captureSymbols(bin.getL()));
                symbols.addAll(captureSymbols(bin.getR()));
            }
            case BooleanConstant ignored -> {}
            case ByteConstant ignored -> {}
            case DereferenceOperator deref -> symbols.addAll(captureSymbols(deref.getExpr()));
            case FunctionExpression func -> func.getParameters().stream().map(ExpressionVisitors::captureSymbols).forEach(symbols::addAll);
            case IntegerConstant ignored -> {}
            case PlaceholderExpression ignored -> {}
            case StringConstant ignored -> {}
            case TypeConversionOperator conversion -> symbols.addAll(captureSymbols(conversion.getExpr()));
            case UnaryExpression unary -> symbols.addAll(captureSymbols(unary.getExpr()));
            case VariableReference ref -> symbols.add(ref.getSymbol());
            default -> throw new RuntimeException("[compiler bug] unsupported expression type: " + expr);
        }
        return symbols;
    }

    /**
     * Determine if expression contains target.
     */
    public static boolean hasSubexpression(Expression expression, Expression target) {
        if (Objects.equals(expression, target)) {
            return true;
        }
        return switch (expression) {
            case AddressOfOperator ignored -> false;
            case ArrayLengthFunction ignored -> false;
            case BinaryExpression bin -> hasSubexpression(bin.getL(), target) || hasSubexpression(bin.getR(), target);
            case BooleanConstant ignored -> false;
            case ByteConstant ignored -> false;
            case DereferenceOperator deref -> hasSubexpression(deref.getExpr(), target);
            case FunctionExpression func -> func.getParameters().stream().map(param -> hasSubexpression(param, target)).reduce(Boolean::logicalOr).orElse(false);
            case IntegerConstant ignored -> false;
            case PlaceholderExpression ignored -> false;
            case StringConstant ignored -> false;
            case TypeConversionOperator conversion -> hasSubexpression(conversion.getExpr(), target);
            case UnaryExpression unary -> hasSubexpression(unary.getExpr(), target);
            case VariableReference ignored -> false;
            default -> throw new RuntimeException("[compiler bug] unsupported expression type: " + expression);
        };
    }

    /**
     * Recursively look for a volatile function. We don't want to optimize functions like
     * RND, ALLOC, etc. This is either marked in the function definition or in the intrinsic
     * definition.
     */
    public static boolean hasVolatileFunction(Expression expression) {
        return switch (expression) {
            case AddressOfOperator ignored -> false;
            case ArrayLengthFunction ignored -> false;
            case BinaryExpression binaryExpression -> hasVolatileFunction(binaryExpression.getL()) || hasVolatileFunction(binaryExpression.getR());
            case BooleanConstant ignored -> false;
            case ByteConstant ignored -> false;
            case DereferenceOperator deref -> hasVolatileFunction(deref.getExpr());
            case FunctionExpression functionExpression -> {
                if (functionExpression.isVolatile()) {
                    yield true;
                }
                yield functionExpression.getParameters().stream().map(ExpressionVisitors::hasVolatileFunction).reduce(Boolean::logicalOr).orElse(false);
            }
            case IntegerConstant ignored -> false;
            case PlaceholderExpression ignored -> false;
            case StringConstant ignored -> false;
            case TypeConversionOperator conversion -> hasVolatileFunction(conversion.getExpr());
            case UnaryExpression unaryExpression -> hasVolatileFunction(unaryExpression.getExpr());
            case VariableReference ignored -> false;
            default -> throw new RuntimeException("[compiler bug] unexpected expression: " + expression);
        };
    }

    /**
     * Calculate the "weight" of an expression. Used to determine if this expression should be
     * considered for optimizations. Note that simple constants and variable references are a 1;
     * everything else will be larger. Hypothetically, this could be used to prioritize which
     * expressions are optimized first, should that become useful.
     */
    public static int weight(Expression expression) {
        return switch (expression) {
            case AddressOfOperator ignored -> 1;
            case ArrayLengthFunction ignored -> 2;
            case BinaryExpression binaryExpression -> weight(binaryExpression.getL()) + weight(binaryExpression.getR());
            case BooleanConstant ignored -> 1;
            case ByteConstant ignored -> 1;
            case DereferenceOperator deref -> 1 + weight(deref.getExpr());
            case FunctionExpression functionExpression -> 2 + functionExpression.getParameters().stream().mapToInt(ExpressionVisitors::weight).sum();
            case IntegerConstant ignored -> 1;
            case PlaceholderExpression ignored -> 1;
            case StringConstant ignored -> 1;
            case TypeConversionOperator conversion -> 1 + weight(conversion.getExpr());
            case UnaryExpression unaryExpression -> 1 + weight(unaryExpression.getExpr());
            case VariableReference ignored -> 1;
            default -> throw new RuntimeException("[compiler bug] unexpected expression: " + expression);
        };
    }
}
