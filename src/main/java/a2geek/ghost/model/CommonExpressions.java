package a2geek.ghost.model;

import a2geek.ghost.model.expression.ArrayLengthFunction;
import a2geek.ghost.model.expression.DereferenceOperator;
import a2geek.ghost.model.expression.IntegerConstant;
import a2geek.ghost.model.expression.VariableReference;

import java.util.List;

/**
 * A set of common expression evaluations that can be added into classes.
 */
public class CommonExpressions {
    private CommonExpressions() {
        // prevent construction
    }

    public static DereferenceOperator arrayReference(Symbol array, List<Expression> indexes) {
        if (array.numDimensions() != indexes.size()) {
            var msg = String.format("dimension mismatch - symbol: '%s', indexes: %s", array, indexes);
            throw new RuntimeException(msg);
        }
        // skip past the boundaries (INTEGER * number of dimensions)
        var overheadBytes =  new IntegerConstant(array.numDimensions() * DataType.INTEGER.sizeof());
        // compute element placement
        Expression elementNumber = indexes.getLast();
        for (int i=0; i<array.numDimensions()-1; i++) {
            Expression dimsize = ubound(array,i+1);
            if (array.dimensions().get(i+1).isConstant()) {
                dimsize = array.dimensions().get(i+1).plus(IntegerConstant.ONE);
            }
            elementNumber = indexes.get(i).times(dimsize).plus(elementNumber);
        }
        // this is the index for the last dimension
        var sizeof = new IntegerConstant(array.dataType().sizeof());
        var offset = VariableReference.with(array).plus(overheadBytes).plus(elementNumber.times(sizeof));
        // *(array+((index+1)*sizeof(datatype))
        // *(array+(ubound(array,1)*sizeof(datatype)+(index+dims)*sizeof(datatype))
        return offset.deref(array.dataType());
    }

    public static ArrayLengthFunction ubound(Symbol symbol, int dimensionNumber) {
        return new ArrayLengthFunction(symbol, dimensionNumber);
    }

    public static DereferenceOperator derefByte(Expression address) {
        return address.deref(DataType.BYTE);
    }

    public static DereferenceOperator derefWord(Expression address) {
        return address.deref(DataType.INTEGER);
    }
}
