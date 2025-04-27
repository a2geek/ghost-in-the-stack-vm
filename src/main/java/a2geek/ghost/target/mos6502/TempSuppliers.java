package a2geek.ghost.target.mos6502;

import a2geek.ghost.model.Symbol;

public class TempSuppliers {
    private TempSuppliers() {
        // prevent construction
    }

    public static ExpressionGenerator.TempSupplier symbolSupplier(final Symbol symbol) {
        return new ExpressionGenerator.TempSupplier() {
            @Override
            public ExpressionGenerator.Terminal get() {
                return Terminals.symbolReference(symbol);
            }
            @Override
            public void free(ExpressionGenerator.Terminal temp) {
                // TODO free only if Terminal is a temp.
            }
        };
    }

    public static ExpressionGenerator.TempSupplier terminalSupplier(final ExpressionGenerator.Terminal term) {
        return new ExpressionGenerator.TempSupplier() {
            @Override
            public ExpressionGenerator.Terminal get() {
                return term;
            }
            @Override
            public void free(ExpressionGenerator.Terminal temp) {
                // empty
            }
        };
    }
}
