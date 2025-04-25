package a2geek.ghost.target.mos6502;

public interface ExpressionGenerator {
    Terminal toTerminal(AssemblyWriter asm, TempSupplier supplier);

    interface Terminal extends ExpressionGenerator {
        @Override
        default Terminal toTerminal(AssemblyWriter asm, TempSupplier supplier) {
            return this;
        }
        void generateByteOp(AssemblyWriter asm, String op, int offset);
        int size();
    }

    interface TempSupplier {
        Terminal get();
        void free(Terminal temp);
    }
}