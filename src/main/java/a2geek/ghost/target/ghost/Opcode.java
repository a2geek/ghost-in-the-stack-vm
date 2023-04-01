package a2geek.ghost.target.ghost;

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
    NE(0),
    OR(0),
    AND(0),
    XOR(0),
    SHIFTL(0),
    SHIFTR(0),
    SETACC(0),
    GETACC(0),
    SETXREG(0),
    GETXREG(0),
    SETYREG(0),
    GETYREG(0),
    CALL(0),
    RETURN(0),
    DUP(0),
    INCR(0),
    DECR(0),
    GLOBAL_RESERVE(1),
    LOCAL_RESERVE(1),
    LOCAL_FREE(1),
    LOCAL_LOAD(1),
    LOCAL_STORE(1),
    GLOBAL_LOAD(1),
    GLOBAL_STORE(1),
    POPN(1),
    GOTO(2),
    GOSUB(2),
    IFTRUE(2),
    IFFALSE(2),
    LOADC(2),
    LOADA(2);


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
