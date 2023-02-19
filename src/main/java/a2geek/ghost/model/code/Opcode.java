package a2geek.ghost.model.code;

public enum Opcode {
    // Single byte instructions (0b00nn nnnn)
    EXIT(0x00,0),
    ADD(0x01,0),
    SUB(0x02,0),
    ISTORE(0x03,0),
    LT(0x04, 0),
    SETACC(0x05, 0),
    SETYREG(0x06, 0),
    CALL(0x07,0),

    // Single byte argument instructions (0b01nn nnnn)
    RESERVE(0x40, 1),
    LOAD(0x41, 1),
    STORE(0x42, 1),

    // Two byte argument instructions (0b10nn nnnn)
    GOTO(0x80, 2),
    IFTRUE(0x81, 2),
    LOADC(0x82, 2);

    int opcode;
    int argc;

    private Opcode(int opcode, int argc) {
        this.opcode = opcode;
        this.argc = argc;
    }
 }
