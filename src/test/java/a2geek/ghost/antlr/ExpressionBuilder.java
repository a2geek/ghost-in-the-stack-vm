package a2geek.ghost.antlr;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Reference;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.expression.*;

public class ExpressionBuilder {
        public static Expression constant(int value) {
            return new IntegerConstant(value);
        }
        public static Expression constant(boolean value) {
            return new BooleanConstant(value);
        }
        public static Expression constant(String value) {
            return new StringConstant(value);
        }
        public static Expression identifier(String name, DataType dataType, Scope.Type scopeType) {
            var ref = Reference.builder(name, scopeType).dataType(dataType).build();
            return new IdentifierExpression(ref);
        }
        public static Expression binary(String operator, Expression lhs, Expression rhs) {
            return new BinaryExpression(lhs, rhs, operator);
        }
        public static Expression unary(String operator, Expression expr) {
            return new UnaryExpression(operator, expr);
        }
    }
