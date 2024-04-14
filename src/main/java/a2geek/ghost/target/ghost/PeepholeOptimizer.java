package a2geek.ghost.target.ghost;

import java.util.ArrayList;
import java.util.List;

public class PeepholeOptimizer {
    private static final Integer ZERO = 0;
    private static final Integer ONE = 1;
    private static final Integer TWO = 2;
    private static final List<String> LABEL_RETURN = new ArrayList<>();

    public static int optimize(List<Instruction> code) {
        var changed = 0;
        var ctx = new InstructionContext(code);
        while (ctx.hasNext()) {
            if (optimize1(ctx) || optimize2(ctx) || optimize3(ctx)) {
                changed++;
            }
            ctx.next();
        }
        return changed;
    }

    static boolean optimize1(InstructionContext ctx) {
        var oneInstruction = ctx.slice(1);
        if (oneInstruction.isPresent()) {
            var list = oneInstruction.get();
            var inst = list.getFirst();
            // POPN 0  ==>  remove
            if (inst.opcode() == Opcode.POPN && ZERO.equals(inst.arg())) {
                list.removeFirst();
                return true;
            }
            // GOTO _SUBEXIT58  ==>  RETURN     (where label is just before a RETURN)
            if (inst.opcode() == Opcode.GOTO && LABEL_RETURN.contains(inst.label())) {
                list.set(0, Instructions.RETURN());
                return true;
            }
            // LOADC 0000  ==> LOAD0
            if (inst.opcode() == Opcode.LOADC && ZERO.equals(inst.arg())) {
                list.set(0, Instructions.LOAD0());
                return true;
            }
            // LOADC 0001  ==> LOAD1
            if (inst.opcode() == Opcode.LOADC && ONE.equals(inst.arg())) {
                list.set(0, Instructions.LOAD1());
                return true;
            }
            // LOADC 0002  ==> LOAD2
            if (inst.opcode() == Opcode.LOADC && TWO.equals(inst.arg())) {
                list.set(0, Instructions.LOAD2());
                return true;
            }
            // POPN 0002  ==> POP2
            if (inst.opcode() == Opcode.POPN && TWO.equals(inst.arg())) {
                list.set(0, Instructions.POP2());
                return true;
            }
            // RETURNN 0000 ==> RETURN
            if (inst.opcode() == Opcode.RETURNN && ZERO.equals(inst.arg())) {
                list.set(0, Instructions.RETURN());
                return true;
            }
            // RETURNN 0002 ==> RETURN2
            if (inst.opcode() == Opcode.RETURNN && TWO.equals(inst.arg())) {
                list.set(0, Instructions.RETURN2());
                return true;
            }
        }
        return false;
    }

    static boolean optimize2(InstructionContext ctx) {
        var twoInstructions = ctx.slice(2);
        if (twoInstructions.isPresent()) {
            var list = twoInstructions.get();
            var inst1 = list.get(0);
            var inst2 = list.get(1);
            //     GOTO label ==> remove GOTO
            // label:
            if (inst1.opcode() == Opcode.GOTO && inst1.label().equals(inst2.label())) {
                list.removeFirst();
                return true;
            }
            // GLOBAL_STORE offset ==>  DUP
            // GLOBAL_LOAD offset  ==>  GLOBAL_STORE offset
            if (seq(list, Opcode.GLOBAL_STORE, Opcode.GLOBAL_LOAD) && inst1.arg().equals(inst2.arg())) {
                list.set(0, Instructions.DUP());
                list.set(1, inst1);
                return true;
            }
            // LOCAL_STORE offset ==>  DUP
            // LOCAL_LOAD offset  ==>  LOCAL_STORE offset
            if (seq(list, Opcode.LOCAL_STORE, Opcode.LOCAL_LOAD) && inst1.arg().equals(inst2.arg())) {
                list.set(0, Instructions.DUP());
                list.set(1, inst1);
                return true;
            }
            // Handle any combination of loads that load the same thing twice
            // GLOBAL_LOAD|LOCAL_LOAD|LOADC arg  ==>  *LOAD arg
            // GLOBAL_LOAD|LOCAL_LOAD|LOADC arg  ==>  DUP
            if (is(inst1.opcode(), Opcode.GLOBAL_LOAD, Opcode.LOCAL_LOAD, Opcode.LOADC)
                    && inst1.opcode() == inst2.opcode() && inst1.arg().equals(inst2.arg())) {
                list.set(1, Instructions.DUP());
                return true;
            }
            // LOADA label  ==>  LOADA label
            // LOADA label  ==>  DUP
            if (seq(list, Opcode.LOADA, Opcode.LOADA) && inst1.label().equals(inst2.label())) {
                list.set(1, Instructions.DUP());
                return true;
            }
            // DECR or INCR  ==> remove
            // INCR or DECR  ==> remove
            if (seq(list, Opcode.DECR, Opcode.INCR) || seq(list, Opcode.INCR, Opcode.DECR)) {
                list.remove(1);
                list.remove(0);
                return true;
            }
            // GOTO _FUNCXIT18  ==>  GOTO _FUNCXIT18
            // GOTO _IF_EXIT19  ==>  remove ANY CODE after a GOTO!
            if (inst1.opcode() == Opcode.GOTO && inst2.opcode() != null) {
                list.remove(1);
                return true;
            }
            // LOADC 0000  ==> remove
            // PUSHZ       ==> remove
            if (seq(list, Opcode.LOADC, Opcode.PUSHZ) && ZERO.equals(inst1.arg())) {
                list.remove(1);
                list.remove(0);
                return true;
            }
            // LOAD0  ==> remove
            // PUSHZ  ==> remove
            if (seq(list, Opcode.LOAD0, Opcode.PUSHZ)) {
                list.remove(1);
                list.remove(0);
                return true;
            }
            // Note: These appear to be side effects of how inlining is handled.
            // LOADC 1234  ==> LOADC 1233|1235
            // INCR|DECR   ==> remove
            if (inst1.opcode() == Opcode.LOADC && (is(inst2.opcode(), Opcode.INCR, Opcode.DECR))) {
                list.set(0, switch (inst2.opcode()) {
                    case INCR -> Instructions.LOADC(inst1.arg()+1);
                    case DECR -> Instructions.LOADC(inst1.arg()-1);
                    default -> throw new RuntimeException("unexpected opcode for instruction 2");
                });
                list.remove(1);
                return true;
            }
            // _SUBEXIT58:
            //     RETURN
            if (inst1.isLabelOnly() && inst2.opcode() == Opcode.RETURN) {
                LABEL_RETURN.add(inst1.label());
                // Just tracking, so don't have to return true since nothing was modified
            }
            // LOADC 0002  ==>  LOAD0
            // PUSHZ       ==>  remove
            if (seq(list, Opcode.LOADC, Opcode.PUSHZ) && TWO.equals(inst1.arg())) {
                list.set(0, Instructions.LOAD0());
                list.remove(1);
                return true;
            }
            // LOAD2  ==>  LOAD0
            // PUSHZ  ==>  remove
            if (seq(list, Opcode.LOAD2, Opcode.PUSHZ)) {
                list.set(0, Instructions.LOAD0());
                list.remove(1);
                return true;
            }
            // LOADC 008C         ==> GLOBAL_SETC 0014,008C
            // GLOBAL_STORE 0014  ==> remove
            if (seq(list, Opcode.LOADC, Opcode.GLOBAL_STORE)) {
                list.set(0, Instructions.GLOBAL_SETC(inst2.arg(), inst1.arg()));
                list.remove(1);
                return true;
            }
            // LOADC 008C        ==> LOCAL_SETC 0014,008C
            // LOCAL_STORE 0014  ==> remove
            if (seq(list, Opcode.LOADC, Opcode.LOCAL_STORE)) {
                list.set(0, Instructions.LOCAL_SETC(inst2.arg(), inst1.arg()));
                list.remove(1);
                return true;
            }
            // LOCAL_LOAD nn    ==> LOCAL_(ILOADB|ISTOREB) nn   (or ILOADW|STOREW)
            // (ILOADB|ISTOREB) ==> remove
            if (inst1.opcode() == Opcode.LOCAL_LOAD && is(inst2.opcode(), Opcode.ILOADB, Opcode.ISTOREB, Opcode.ILOADW, Opcode.ISTOREW)) {
                list.set(0, switch (inst2.opcode()) {
                    case ILOADB -> Instructions.LOCAL_ILOADB(inst1.arg());
                    case ISTOREB -> Instructions.LOCAL_ISTOREB(inst1.arg());
                    case ILOADW -> Instructions.LOCAL_ILOADW(inst1.arg());
                    case ISTOREW -> Instructions.LOCAL_ISTOREW(inst1.arg());
                    default -> throw new RuntimeException("unexpected opcode for instruction 2");
                });
                list.remove(1);
                return true;
            }
            // GLOBAL_LOAD nn    ==> GLOBAL_(ILOADB|ISTOREB) nn   (or ILOADW|STOREW)
            // (ILOADB|ISTOREB)  ==> remove
            if (inst1.opcode() == Opcode.GLOBAL_LOAD && is(inst2.opcode(), Opcode.ILOADB, Opcode.ISTOREB, Opcode.ILOADW, Opcode.ISTOREW)) {
                list.set(0, switch (inst2.opcode()) {
                    case ILOADB -> Instructions.GLOBAL_ILOADB(inst1.arg());
                    case ISTOREB -> Instructions.GLOBAL_ISTOREB(inst1.arg());
                    case ILOADW -> Instructions.GLOBAL_ILOADW(inst1.arg());
                    case ISTOREW -> Instructions.GLOBAL_ISTOREW(inst1.arg());
                    default -> throw new RuntimeException("unexpected opcode for instruction 2");
                });
                list.remove(1);
                return true;
            }
        };
        return false;
    }

    static boolean optimize3(InstructionContext ctx) {
        var threeInstructions = ctx.slice(3);
        if (threeInstructions.isPresent()) {
            var list = threeInstructions.get();
            var inst1 = list.get(0);
            var inst2 = list.get(1);
            var inst3 = list.get(2);
            //     IFZ label        ==>     IFNZ other_label
            //     GOTO other_label ==>         ; (deleted)
            // label:               ==> label:  ; no change
            if (inst1.opcode() == Opcode.IFZ && inst2.opcode() == Opcode.GOTO && inst3.isLabelOnly()
                    && inst1.label().equals(inst3.label())) {
                list.set(0, Instructions.IFNZ(inst2.label()));
                list.remove(1);
                return true;
            }
            //     IFNZ label       ==>     IFZ other_label
            //     GOTO other_label ==>         ; (deleted)
            // label:               ==> label:  ; no change
            if (inst1.opcode() == Opcode.IFNZ && inst2.opcode() == Opcode.GOTO && inst3.isLabelOnly()
                    && inst1.label().equals(inst3.label())) {
                list.set(0, Instructions.IFZ(inst2.label()));
                list.remove(1);
                return true;
            }
            // GLOBAL_LOAD 0000   ==>  GLOBAL_INCR|GLOBAL_DECR 0000
            // INCR|DECR          ==>  removed
            // GLOBAL_STORE 0000  ==>  removed
            if (seq(list, Opcode.GLOBAL_LOAD, Opcode.INCR, Opcode.GLOBAL_STORE) && inst1.arg().equals(inst3.arg())) {
                list.remove(2);
                list.remove(1);
                list.set(0, Instructions.GLOBAL_INCR(inst1.arg()));
                return true;
            }
            if (seq(list, Opcode.GLOBAL_LOAD, Opcode.DECR, Opcode.GLOBAL_STORE) && inst1.arg().equals(inst3.arg())) {
                list.remove(2);
                list.remove(1);
                list.set(0, Instructions.GLOBAL_DECR(inst1.arg()));
                return true;
            }
            // LOCAL_LOAD 0000   ==>  LOCAL_INCR|LOCAL_DECR 0000
            // INCR|DECR         ==>  removed
            // LOCAL_STORE 0000  ==>  removed
            if (seq(list, Opcode.LOCAL_LOAD, Opcode.INCR, Opcode.LOCAL_STORE) && inst1.arg().equals(inst3.arg())) {
                list.remove(2);
                list.remove(1);
                list.set(0, Instructions.LOCAL_INCR(inst1.arg()));
                return true;
            }
            if (seq(list, Opcode.LOCAL_LOAD, Opcode.DECR, Opcode.LOCAL_STORE) && inst1.arg().equals(inst3.arg())) {
                list.remove(2);
                list.remove(1);
                list.set(0, Instructions.LOCAL_DECR(inst1.arg()));
                return true;
            }
        };
        return false;
    }

    static boolean is(Opcode primary, Opcode... secondaries) {
        for (Opcode secondary : secondaries) {
            if (primary == secondary) return true;
        }
        return false;
    }
    static boolean seq(List<Instruction> list, Opcode... opcodes) {
        if (list.size() != opcodes.length) {
            throw new RuntimeException("expecting number of instructions to match number of opcodes");
        }
        for (int i=0; i<opcodes.length; i++) {
            if (list.get(i).opcode() != opcodes[i]) {
                return false;
            }
        }
        return true;
    }
}
