package a2geek.ghost.model.code;

public enum Opcode {
    EXIT(0),
    ADD(0),
    SUB(0),
    MUL(0),
    DIV(0),
    MOD(0),
    ILOAD(0),
    ISTORE(0),
    LT(0),
    LE(0),
    EQ(0),
    SETACC(0),
    SETYREG(0),
    CALL(0),
    RESERVE(1),
    LOAD(1),
    STORE(1),
    GOTO(2),
    IFTRUE(2),
    IFFALSE(2),
    LOADC(2);

    int argc;

    private Opcode(int argc) {
        this.argc = argc;
    }

    public byte getByteCode() {
        return (byte)ordinal();
    }
    public int getArgumentCount() {
        return argc;
    }
 }
