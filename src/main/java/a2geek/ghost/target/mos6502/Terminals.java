package a2geek.ghost.target.mos6502;

import a2geek.ghost.model.Intrinsic;
import a2geek.ghost.model.Symbol;
import a2geek.ghost.target.mos6502.ExpressionGenerator.Terminal;

import static a2geek.ghost.Util.errorf;
import static a2geek.ghost.target.mos6502.CodeGenerationVisitor.*;

public class Terminals {
    private Terminals() {
        // prevent construction
    }

    public static Terminal symbolReference(final Symbol symbol) {
        return switch (symbol.declarationType()) {
            case LOCAL -> localVariable(symbol);
            case GLOBAL -> globalVariable(symbol);
            case INTRINSIC -> intrinsicVariable(symbol);
        };
    }

    public static Terminal localVariable(final Symbol symbol) {
        return new Terminal() {
            @Override
            public void generateByteOp(AssemblyWriter asm, String op, int offset) {
                // TODO - offset in this case is offset to the local variable
                // TODO - really need to track register value to prevent a bunch of extra LDX references
                asm.LDX(FRAME_PTR);
                asm.op(op, "stack+%d,x", offset);
            }
            @Override
            public int size() {
                return symbol.dataType().sizeof();
            }
            @Override
            public String toString() {
                return String.format("stack+%s,x", symbol.name());
            }
        };
    }

    public static Terminal globalVariable(final Symbol symbol) {
        return new Terminal() {
            @Override
            public void generateByteOp(AssemblyWriter asm, String op, int offset) {
                asm.op(op, "%s+%d", symbol.name(), offset);
            }
            @Override
            public int size() {
                return symbol.dataType().sizeof();
            }
            @Override
            public String toString() {
                return symbol.name();
            }
        };
    }

    public static Terminal intrinsicVariable(final Symbol symbol) {
        final var name = switch (symbol.name().toLowerCase()) {
            case Intrinsic.CPU_REGISTER_A -> REG_A;
            case Intrinsic.CPU_REGISTER_X -> REG_X;
            case Intrinsic.CPU_REGISTER_Y -> REG_Y;
            default -> throw errorf("unknown intrinsic variable '%s'", symbol.name());
        };
        return new ExpressionGenerator.Terminal() {
            @Override
            public void generateByteOp(AssemblyWriter asm, String op, int offset) {
                asm.op(op, "%s+%d", name, offset);
            }
            @Override
            public int size() {
                return 1;
            }
            @Override
            public String toString() {
                return symbol.name();
            }
        };
    }

    public static Terminal indyReference(final String label) {
        return new Terminal() {
            @Override
            public void generateByteOp(AssemblyWriter asm, String op, int offset) {
                if (offset == 0) {
                    asm.LDY("#0")
                            .op(op, "(%s),y", label);
                }
                else {
                    asm.LDY("#1")
                            .op(op, "(%s),y", label);
                }
            }
            @Override
            public int size() {
                return 2;   // TODO ???
            }
            @Override
            public String toString() {
                return String.format("(%s),y", label);
            }
        };
    }

    public static Terminal tosReference() {
        return new Terminal() {
            @Override
            public void generateByteOp(AssemblyWriter asm, String op, int offset) {
                switch (op.toUpperCase()) {
                    case "LDA" -> asm.PLA();
                    case "STA" -> asm.PHA();
                    default -> throw errorf("can't do '%s' with TOS reference", op);
                }
            }
            @Override
            public int size() {
                return 2;   // TODO ???
            }
            @Override
            public String toString() {
                return "STACK";
            }
        };
    }

    public static Terminal intConstant(int value) {
        return new Terminal() {
            @Override
            public void generateByteOp(AssemblyWriter asm, String op, int offset) {
                var n = switch(offset) {
                    case 0 -> value & 0xff;
                    case 1 -> value << 8;
                    default -> throw errorf("unable to get value for integer constant %d at offset %d", value, offset);
                };
                asm.op(op, "#$%02x", n);
            }
            @Override
            public int size() {
                return 2;
            }
            @Override
            public String toString() {
                return String.format("#%d", value);
            }
        };
    }

    public static Terminal labelReference(String label) {
        return new Terminal() {
            @Override
            public void generateByteOp(AssemblyWriter asm, String op, int offset) {
                if (offset == 0) {
                    asm.op(op, "%s", label);
                }
                else {
                    asm.op(op, "%s+%d", label, offset);
                }
            }
            @Override
            public int size() {
                return 2;
            }
            @Override
            public String toString() {
                return label;
            }
        };
    }

    public static Terminal addressReference(final int address, final int size) {
        return new Terminal() {
            @Override
            public void generateByteOp(AssemblyWriter asm, String op, int offset) {
                asm.op(op, "%d", address + offset);
            }
            @Override
            public int size() {
                return size;
            }
            @Override
            public String toString() {
                return String.format("%04x", address);
            }
        };
    }
}
