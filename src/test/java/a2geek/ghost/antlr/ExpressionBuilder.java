package a2geek.ghost.antlr;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.model.expression.*;

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
        public static VariableReference identifier(String name, DataType dataType, Scope.Type scopeType) {
            var symbol = Symbol.builder(name, scopeType).dataType(dataType).build();
            return new VariableReference(symbol);
        }
        public static VariableReference arrayReference(String name, DataType dataType, Scope.Type scopeType, Expression expr) {
            var symbol = Symbol.builder(name + "()", scopeType).dataType(dataType).dimensions(1).build();
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
    }
