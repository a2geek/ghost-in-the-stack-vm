package a2geek.ghost.antlr;

import a2geek.ghost.model.basic.*;
import a2geek.ghost.model.basic.expression.*;

import java.util.Arrays;

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
        public static VariableReference identifier(String name, DataType dataType, Scope.Type scopeType) {
            var symbol = Symbol.variable(name, scopeType).dataType(dataType).build();
            return new VariableReference(symbol);
        }
        public static VariableReference arrayReference(String name, DataType dataType, Scope.Type scopeType, Expression expr) {
            var symbol = Symbol.variable(name + "()", scopeType).dataType(dataType).dimensions(1).build();
            return new VariableReference(symbol, Arrays.asList(expr));
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
        public static ArrayLengthFunction ubound(ModelBuilder model, Symbol arrayRef) {
            return new ArrayLengthFunction(model, arrayRef);
        }
    }
