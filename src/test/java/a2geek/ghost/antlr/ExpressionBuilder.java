package a2geek.ghost.antlr;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Reference;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.expression.*;

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
        public static IdentifierExpression identifier(String name, DataType dataType, Scope.Type scopeType) {
            var ref = Reference.builder(name, scopeType).dataType(dataType).build();
            return new IdentifierExpression(ref);
        }
        public static BinaryExpression binary(String operator, Expression lhs, Expression rhs) {
            return new BinaryExpression(lhs, rhs, operator);
        }
        public static UnaryExpression unary(String operator, Expression expr) {
            return new UnaryExpression(operator, expr);
        }
    }
