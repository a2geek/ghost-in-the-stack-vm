package a2geek.ghost.target.ghost;

public enum Opcode {
    EXIT(0),
    ADD(0),
    SUB(0),
    MUL(0),
    DIVS(0),
    MODS(0),
    DIVU(0),
    MODU(0),
    NEG(0),
    ILOADB(0),
    ISTOREB(0),
    ILOADW(0),
    ISTOREW(0),
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
    PUSHZ(0),
    POP(0),
    LOADSP(0),
    STORESP(0),
    LOADLP(0),
    STORELP(0),
    LOADGP(0),
    STOREGP(0),
    FIXA(0),
    LOCAL_LOAD(1),
    LOCAL_STORE(1),
    LOCAL_INCR(1),
    LOCAL_DECR(1),
    GLOBAL_LOAD(1),
    GLOBAL_STORE(1),
    GLOBAL_INCR(1),
    GLOBAL_DECR(1),
    POPN(1),
    GOTO(2),
    GOSUB(2),
    IFNZ(2),
    IFZ(2),
    LOADC(2),
    LOADA(2),
    LOAD0(0),
    LOAD1(0),
    LOAD2(0),
    POP2(0),
    GLOBAL_SETC(3),
    LOCAL_SETC(3),
    RETURNN(1),
    RETURN2(0),
    LOCAL_ILOADB(1),
    LOCAL_ISTOREB(1),
    LOCAL_ILOADW(1),
    LOCAL_ISTOREW(1),
    GLOBAL_ILOADB(1),
    GLOBAL_ISTOREB(1),
    GLOBAL_ILOADW(1),
    GLOBAL_ISTOREW(1);

    private final int argc;

    Opcode(int argc) {
        this.argc = argc;
    }

    public byte getByteCode() {
        return (byte)ordinal();
    }
    public int getArgumentCount() {
        return argc;
    }
 }
