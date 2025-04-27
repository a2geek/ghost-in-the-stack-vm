package a2geek.ghost.target.mos6502;

import java.util.Objects;

public interface ExpressionGenerator {
    Terminal toTerminal(AssemblyWriter asm, TempSupplier supplier);

    interface Terminal extends ExpressionGenerator {
        @Override
        default Terminal toTerminal(AssemblyWriter asm, TempSupplier supplier) {
            return this;
        }
        void generateByteOp(AssemblyWriter asm, String op, int offset);
        int size();
        default boolean equals(Terminal t) {
            return Objects.equals(toString(), t.toString());
        }
    }

    interface TempSupplier {
        Terminal get();
        void free(Terminal temp);
    }
}