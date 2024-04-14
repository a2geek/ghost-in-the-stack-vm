package a2geek.ghost.target.ghost;

public class Instructions {
    private Instructions() {
        // prevent construction
    }

    private static Instruction build(Opcode opcode) {
        return new Instruction(null, opcode, null, null, null,null);
    }
    private static Instruction build(Opcode opcode, int arg) {
        return new Instruction(null, opcode, null, arg, null, null);
    }
    private static Instruction build(Opcode opcode, int arg, int arg2) {
        return new Instruction(null, opcode, null, arg, arg2, null);
    }
    private static Instruction build(Opcode opcode, String label) {
        return new Instruction(label, opcode, null, null, null, null);
    }

    public static Instruction RETURN() {
        return build(Opcode.RETURN);
    }
    public static Instruction RETURN2() {
        return build(Opcode.RETURN2);
    }
    public static Instruction LOAD0() {
        return build(Opcode.LOAD0);
    }
    public static Instruction LOAD1() {
        return build(Opcode.LOAD1);
    }
    public static Instruction LOAD2() {
        return build(Opcode.LOAD2);
    }
    public static Instruction POP2() {
        return build(Opcode.POP2);
    }
    public static Instruction DUP() {
        return build(Opcode.DUP);
    }
    public static Instruction LOADC(int arg) {
        return build(Opcode.LOADC, arg);
    }
    public static Instruction GLOBAL_SETC(int arg, int arg2) {
        return build(Opcode.GLOBAL_SETC, arg, arg2);
    }
    public static Instruction LOCAL_SETC(int arg, int arg2) {
        return build(Opcode.LOCAL_SETC, arg, arg2);
    }
    public static Instruction IFZ(String label) {
        return build(Opcode.IFZ, label);
    }
    public static Instruction IFNZ(String label) {
        return build(Opcode.IFNZ, label);
    }
    public static Instruction GLOBAL_INCR(int arg) {
        return build(Opcode.GLOBAL_INCR, arg);
    }
    public static Instruction GLOBAL_DECR(int arg) {
        return build(Opcode.GLOBAL_DECR, arg);
    }
    public static Instruction LOCAL_INCR(int arg) {
        return build(Opcode.LOCAL_INCR, arg);
    }
    public static Instruction LOCAL_DECR(int arg) {
        return build(Opcode.LOCAL_DECR, arg);
    }
    public static Instruction LOCAL_ILOADB(int arg) {
        return build(Opcode.LOCAL_ILOADB, arg);
    }
    public static Instruction LOCAL_ISTOREB(int arg) {
        return build(Opcode.LOCAL_ISTOREB, arg);
    }
    public static Instruction GLOBAL_ILOADB(int arg) {
        return build(Opcode.GLOBAL_ILOADB, arg);
    }
    public static Instruction GLOBAL_ISTOREB(int arg) {
        return build(Opcode.GLOBAL_ISTOREB, arg);
    }
    public static Instruction LOCAL_ILOADW(int arg) {
        return build(Opcode.LOCAL_ILOADW, arg);
    }
    public static Instruction LOCAL_ISTOREW(int arg) {
        return build(Opcode.LOCAL_ISTOREW, arg);
    }
    public static Instruction GLOBAL_ILOADW(int arg) {
        return build(Opcode.GLOBAL_ILOADW, arg);
    }
    public static Instruction GLOBAL_ISTOREW(int arg) {
        return build(Opcode.GLOBAL_ISTOREW, arg);
    }
}
