package a2geek.ghost.antlr;

import a2geek.ghost.model.*;
import a2geek.ghost.model.expression.*;

import java.util.Arrays;
import java.util.List;

public class ExpressionBuilder {
        public static IntegerConstant constant(int value) {
            return new IntegerConstant(value);
        }
        public static BooleanConstant constant(boolean value) {
            return new BooleanConstant(value);
        }
        public static StringConstant constant(String value) {
            return new StringConstant(value);
        }
        /**
         * Create a symbol. Since the case strategy <em>is not attached</em>,
         * the case strategy needs to be applied outside of this method.
         */
        public static VariableReference identifier(String name, DataType dataType, SymbolType symbolType,
                                                   DeclarationType declarationType) {
            var symbol = Symbol.variable(name, symbolType)
                    .dataType(dataType)
                    .declarationType(declarationType)
                    .build();
            return new VariableReference(symbol);
        }
        public static Expression arrayReference(String name, DataType dataType, SymbolType symbolType,
                                                DeclarationType declarationType, Expression expr, int dimensionSize) {
            var symbol = Symbol.variable(name, symbolType)
                    .dataType(dataType)
                    .declarationType(declarationType)
                    .dimensions(List.of(dimensionSize < 1 ? PlaceholderExpression.of(DataType.INTEGER) : new IntegerConstant(dimensionSize)))
                    .build();
            return CommonExpressions.arrayReference(symbol, List.of(expr));
        }
        public static BinaryExpression binary(String operator, Expression lhs, Expression rhs) {
            return new BinaryExpression(lhs, rhs, operator);
        }
        public static UnaryExpression unary(String operator, Expression expr) {
            return new UnaryExpression(operator, expr);
        }
        public static FunctionExpression function(String name, Expression... parameters) {
            return new FunctionExpression(name, Arrays.asList(parameters));
        }
        public static ArrayLengthFunction ubound(Symbol arrayRef, int dimensionNumber) {
            return new ArrayLengthFunction(arrayRef, dimensionNumber);
        }
    }
