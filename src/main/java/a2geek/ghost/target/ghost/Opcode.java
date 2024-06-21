package a2geek.ghost.target.ghost;

import java.util.Objects;

import static a2geek.ghost.TrackingLogger.LOGGER;

public enum Opcode {
    EXIT(Mode.ZERO_ARGS),
    ADD(Mode.ZERO_ARGS),
    SUB(Mode.ZERO_ARGS),
    MUL(Mode.ZERO_ARGS),
    DIVS(Mode.ZERO_ARGS),
    MODS(Mode.ZERO_ARGS),
    DIVU(Mode.ZERO_ARGS),
    MODU(Mode.ZERO_ARGS),
    NEG(Mode.ZERO_ARGS),
    ILOADB(Mode.ZERO_ARGS),
    ISTOREB(Mode.ZERO_ARGS),
    ILOADW(Mode.ZERO_ARGS),
    ISTOREW(Mode.ZERO_ARGS),
    LT(Mode.ZERO_ARGS),
    LE(Mode.ZERO_ARGS),
    EQ(Mode.ZERO_ARGS),
    NE(Mode.ZERO_ARGS),
    OR(Mode.ZERO_ARGS),
    AND(Mode.ZERO_ARGS),
    XOR(Mode.ZERO_ARGS),
    SHIFTL(Mode.ZERO_ARGS),
    SHIFTR(Mode.ZERO_ARGS),
    SETACC(Mode.ZERO_ARGS),
    GETACC(Mode.ZERO_ARGS),
    SETXREG(Mode.ZERO_ARGS),
    GETXREG(Mode.ZERO_ARGS),
    SETYREG(Mode.ZERO_ARGS),
    GETYREG(Mode.ZERO_ARGS),
    CALL(Mode.ZERO_ARGS),
    RETURN(Mode.ZERO_ARGS),
    DUP(Mode.ZERO_ARGS),
    INCR(Mode.ZERO_ARGS),
    DECR(Mode.ZERO_ARGS),
    PUSHZ(Mode.ZERO_ARGS),
    POP(Mode.ZERO_ARGS),
    LOADSP(Mode.ZERO_ARGS),
    STORESP(Mode.ZERO_ARGS),
    LOADLP(Mode.ZERO_ARGS),
    STORELP(Mode.ZERO_ARGS),
    LOADGP(Mode.ZERO_ARGS),
    STOREGP(Mode.ZERO_ARGS),
    FIXA(Mode.ZERO_ARGS),
    LOCAL_LOAD(Mode.BYTE_ARG),
    LOCAL_STORE(Mode.BYTE_ARG),
    LOCAL_INCR(Mode.BYTE_ARG),
    LOCAL_DECR(Mode.BYTE_ARG),
    GLOBAL_LOAD(Mode.BYTE_ARG),
    GLOBAL_STORE(Mode.BYTE_ARG),
    GLOBAL_INCR(Mode.BYTE_ARG),
    GLOBAL_DECR(Mode.BYTE_ARG),
    POPN(Mode.BYTE_ARG),
    GOTO(Mode.WORD_ARG),
    GOSUB(Mode.WORD_ARG),
    IFNZ(Mode.WORD_ARG),
    IFZ(Mode.WORD_ARG),
    LOADC(Mode.WORD_ARG),
    LOADA(Mode.WORD_ARG),
    LOAD0(Mode.ZERO_ARGS),
    LOAD1(Mode.ZERO_ARGS),
    LOAD2(Mode.ZERO_ARGS),
    POP2(Mode.ZERO_ARGS),
    GLOBAL_SETC(Mode.BYTE_WORD_ARG),
    LOCAL_SETC(Mode.BYTE_WORD_ARG),
    RETURNN(Mode.BYTE_ARG),
    RETURN2(Mode.ZERO_ARGS),
    LOCAL_ILOADB(Mode.BYTE_ARG),
    LOCAL_ISTOREB(Mode.BYTE_ARG),
    LOCAL_ILOADW(Mode.BYTE_ARG),
    LOCAL_ISTOREW(Mode.BYTE_ARG),
    GLOBAL_ILOADB(Mode.BYTE_ARG),
    GLOBAL_ISTOREB(Mode.BYTE_ARG),
    GLOBAL_ILOADW(Mode.BYTE_ARG),
    GLOBAL_ISTOREW(Mode.BYTE_ARG);

    private Mode mode;

    Opcode(Mode mode) {
        Objects.requireNonNull(mode);
        this.mode = mode;
    }

    public byte getByteCode() {
        return (byte)ordinal();
    }
    public int getSize() {
        return mode.size;
    }
    public byte[] generate(int ...args) {
        return switch (mode) {
            case ZERO_ARGS -> withZeroArgs(args);
            case BYTE_ARG -> withByteArg(args);
            case WORD_ARG -> withWordArg(args);
            case BYTE_WORD_ARG -> withByteAndWordArg(args);
        };
    }
    public String format(Object ...args) {
        return switch (mode) {
            case ZERO_ARGS -> toString();
            case BYTE_ARG -> String.format("%s %s", this, formatByte(args[0]));
            case WORD_ARG -> String.format("%s %s", this, formatWord(args[0]));
            case BYTE_WORD_ARG -> String.format("%s %s,%s", this, formatByte(args[0]), formatWord(args[1]));
        };
    }
    private String formatByte(Object arg) {
        return format(arg, "%02X");
    }
    private String formatWord(Object arg) {
        return format(arg, "%04X");
    }
    private String format(Object arg, String fmt) {
        if (arg instanceof String s) {
            return s;
        }
        else if (arg instanceof Integer i) {
            return String.format(fmt, i & 0xffff);
        }
        LOGGER.failf("Unexpected object type: %s", arg);
        return null;
    }

    private byte[] withZeroArgs(int ...args) {
        if (args.length != 0) {
            LOGGER.failf("%s takes no arguments", this);
        }
        return new byte[] { getByteCode() };
    }
    private byte[] withByteArg(int ...args) {
        if (args.length != 1) {
            LOGGER.failf("%s takes 1 byte argument", this);
        }
        int value = args[0];
        return new byte[] { getByteCode(), (byte)value};
    }
    private byte[] withWordArg(int... args) {
        if (args.length != 1) {
            LOGGER.failf("%s takes 1 word argument", this);
        }
        int value = args[0];
        return new byte[] { getByteCode(),
            (byte)value, (byte)(value >> 8 & 0xff) };
    }
    private byte[] withByteAndWordArg(int... args) {
        if (args.length != 2) {
            LOGGER.failf("%s takes 2 arguments (byte, word)", this);
        }
        int bvalue = args[0];
        int wvalue = args[1];
        return new byte[] { getByteCode(),
            (byte)bvalue,
            (byte)wvalue, (byte)(wvalue >> 8 & 0xff) };
    }

    private enum Mode {
        ZERO_ARGS(1),
        BYTE_ARG(2),
        WORD_ARG(3),
        BYTE_WORD_ARG(4);

        public int size;

        Mode(int size) {
            this.size = size;
        }
    }
 }
