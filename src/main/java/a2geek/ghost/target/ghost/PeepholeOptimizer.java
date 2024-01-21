package a2geek.ghost.target.ghost;

import java.util.List;

public class PeepholeOptimizer {
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
            if (inst.opcode() == Opcode.POPN && Integer.valueOf(0).equals(inst.arg())) {
                list.removeFirst();
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
            if (inst1.opcode() == Opcode.GLOBAL_STORE && inst2.opcode() == Opcode.GLOBAL_LOAD && inst1.arg().equals(inst2.arg())) {
                list.set(0, new Instruction(null, Opcode.DUP, null, null, null));
                list.set(1, inst1);
                return true;
            }
            // LOCAL_STORE offset ==>  DUP
            // LOCAL_LOAD offset  ==>  LOCAL_STORE offset
            if (inst1.opcode() == Opcode.LOCAL_STORE && inst2.opcode() == Opcode.LOCAL_LOAD && inst1.arg().equals(inst2.arg())) {
                list.set(0, new Instruction(null, Opcode.DUP, null, null, null));
                list.set(1, inst1);
                return true;
            }
            // Handle any combination of loads that load the same thing twice
            // GLOBAL_LOAD|LOCAL_LOAD|LOADC arg  ==>  *LOAD arg
            // GLOBAL_LOAD|LOCAL_LOAD|LOADC arg  ==>  DUP
            if (is(inst1.opcode(), Opcode.GLOBAL_LOAD, Opcode.LOCAL_LOAD, Opcode.LOADC)
                    && inst1.opcode() == inst2.opcode() && inst1.arg().equals(inst2.arg())) {
                list.set(1, new Instruction(null, Opcode.DUP, null, null, null));
                return true;
            }
            // LOADA label  ==>  LOADA label
            // LOADA label  ==>  DUP
            if (inst1.opcode() == Opcode.LOADA && inst2.opcode() == Opcode.LOADA && inst1.label().equals(inst2.label())) {
                list.set(1, new Instruction(null, Opcode.DUP, null, null, null));
                return true;
            }
            // DECR or INCR  ==> remove
            // INCR or DECR  ==> remove
            if ((inst1.opcode() == Opcode.DECR && inst2.opcode() == Opcode.INCR)
                    || (inst1.opcode() == Opcode.INCR && inst2.opcode() == Opcode.DECR)) {
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
            if (inst1.opcode() == Opcode.LOADC && Integer.valueOf(0).equals(inst1.arg()) && inst2.opcode() == Opcode.PUSHZ) {
                list.remove(1);
                list.remove(0);
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
            //     IFFALSE label    ==>     IFTRUE other_label
            //     GOTO other_label ==>         ; (deleted)
            // label:               ==> label:  ; no change
            if (inst1.opcode() == Opcode.IFZ && inst2.opcode() == Opcode.GOTO && inst3.isLabelOnly()
                    && inst1.label().equals(inst3.label())) {
                list.set(0, new Instruction(inst2.label(), Opcode.IFNZ, null, null, null));
                list.remove(1);
                return true;
            }
            //     IFTRUE label     ==>     IFFALSE other_label
            //     GOTO other_label ==>         ; (deleted)
            // label:               ==> label:  ; no change
            if (inst1.opcode() == Opcode.IFNZ && inst2.opcode() == Opcode.GOTO && inst3.isLabelOnly()
                    && inst1.label().equals(inst3.label())) {
                list.set(0, new Instruction(inst2.label(), Opcode.IFZ, null, null, null));
                list.remove(1);
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
}
