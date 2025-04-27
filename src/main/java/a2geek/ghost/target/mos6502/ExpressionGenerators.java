package a2geek.ghost.target.mos6502;

public class ExpressionGenerators {
    private ExpressionGenerators() {
        // prevent construction
    }

    public static ExpressionGenerator assign(ExpressionGenerator exprGen) {
        return (asm, supplier) -> {
            var expr = exprGen.toTerminal(asm, supplier);
            var result = supplier.get();
            if (expr.equals(result)) {
                return expr;    // this expression assigned to our result
            }
            expr.generateByteOp(asm, "LDA", 0);
            result.generateByteOp(asm, "STA", 0);
            if (result.size() > 1) {
                if (expr.size() > 1) {
                    expr.generateByteOp(asm, "LDA", 1);
                }
                else {
                    asm.LDA("#0");
                }
                result.generateByteOp(asm, "STA", 1);
            }
            supplier.free(expr);
            return result;
        };
    }
    public static ExpressionGenerator negate(ExpressionGenerator exprGen) {
        return (asm, supplier) -> {
            var temp = exprGen.toTerminal(asm, supplier);
            var result = supplier.get();
            asm.SEC()
               .LDA("#0");
            temp.generateByteOp(asm, "SBC", 0);
            result.generateByteOp(asm, "STA", 0);
            if (result.size() > 1) {
                asm.LDA("#0");
                if (temp.size() > 1) {
                    temp.generateByteOp(asm, "SBC", 1);
                }
                else {
                    // TODO generate better code
                    asm.SBC("#0");
                }
                result.generateByteOp(asm, "STA", 1);
            }
            supplier.free(temp);
            return result;
        };
    }
    public static ExpressionGenerator not(ExpressionGenerator exprGen, int mask) {
        return (asm, supplier) -> {
            var temp = exprGen.toTerminal(asm, supplier);
            var result = supplier.get();
            asm.TSX()
               .LDA("#>$%04x", mask);
            temp.generateByteOp(asm, "EOR", 0);
            result.generateByteOp(asm, "STA", 0);
            if (result.size() > 1) {
                asm.LDA("#<$%04x", mask);
                if (temp.size() > 1) {
                    temp.generateByteOp(asm, "EOR", 1);
                }
                else {
                    // TODO verify EOR of 16-bit vs 8-bit should just keep high byte as-is?
                }
                result.generateByteOp(asm, "STA", 1);
            }
            supplier.free(temp);
            return result;
        };
    }
}
