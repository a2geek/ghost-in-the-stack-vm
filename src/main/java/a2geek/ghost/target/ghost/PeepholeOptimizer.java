package a2geek.ghost.target.ghost;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class PeepholeOptimizer {
    public static int optimize(List<Instruction> code) {
        var changed = 0;
        var ctx = new Context(code);
        while (ctx.hasNext()) {
            if (optimize2(ctx) || optimize3(ctx)) {
                changed++;
            }
            ctx.next();
        }
        return changed;
    }

    static boolean optimize2(Context ctx) {
        var twoInstructions = ctx.slice(2);
        if (twoInstructions.isPresent()) {
            var list = twoInstructions.get();
            var inst1 = list.get(0);
            var inst2 = list.get(1);
            //     GOTO label ==> remove GOTO
            // label:
            if (inst1.opcode() == Opcode.GOTO && inst1.label().equals(inst2.label())) {
                list.remove(0);
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
            // GLOBAL_LOAD offset  ==>  GLOBAL_LOAD offset
            // GLOBAL_LOAD offset  ==>  DUP
            if (inst1.opcode() == Opcode.GLOBAL_LOAD && inst2.opcode() == Opcode.GLOBAL_LOAD && inst1.arg().equals(inst2.arg())) {
                list.set(1, new Instruction(null, Opcode.DUP, null, null, null));
                return true;
            }
            // LOCAL_LOAD offset  ==>  LOCAL_LOAD offset
            // LOCAL_LOAD offset  ==>  DUP
            if (inst1.opcode() == Opcode.LOCAL_LOAD && inst2.opcode() == Opcode.LOCAL_LOAD && inst1.arg().equals(inst2.arg())) {
                list.set(1, new Instruction(null, Opcode.DUP, null, null, null));
                return true;
            }
            // LOADC 0014  ==>  LOADC 0014
            // LOADC 0014  ==>  DUP
            if (inst1.opcode() == Opcode.LOADC && inst2.opcode() == Opcode.LOADC && inst1.arg().equals(inst2.arg())) {
                list.set(1, new Instruction(null, Opcode.DUP, null, null, null));
                return true;
            }
            // GOTO _FUNCXIT18  ==>  GOTO _FUNCXIT18
            // GOTO _IF_EXIT19  ==>  remove ANY CODE after a GOTO!
            if (inst1.opcode() == Opcode.GOTO && inst2.opcode() != null) {
                list.remove(1);
                return true;
            }
        };
        return false;
    }

    static boolean optimize3(Context ctx) {
        var threeInstructions = ctx.slice(3);
        if (threeInstructions.isPresent()) {
            var list = threeInstructions.get();
            var inst1 = list.get(0);
            var inst2 = list.get(1);
            var inst3 = list.get(2);
            //     IFFALSE label    ==>     IFTRUE other_label
            //     GOTO other_label ==>         ; (deleted)
            // label:               ==> label:  ; no change
            if (inst1.opcode() == Opcode.IFFALSE && inst2.opcode() == Opcode.GOTO && inst3.isLabelOnly()
                    && inst1.label().equals(inst3.label())) {
                list.set(0, new Instruction(inst2.label(), Opcode.IFTRUE, null, null, null));
                list.remove(1);
                return true;
            }
            //     IFTRUE label     ==>     IFFALSE other_label
            //     GOTO other_label ==>         ; (deleted)
            // label:               ==> label:  ; no change
            if (inst1.opcode() == Opcode.IFTRUE && inst2.opcode() == Opcode.GOTO && inst3.isLabelOnly()
                    && inst1.label().equals(inst3.label())) {
                list.set(0, new Instruction(inst2.label(), Opcode.IFFALSE, null, null, null));
                list.remove(1);
                return true;
            }
        };
        return false;
    }

    public static class Context implements Iterator<Instruction> {
        private List<Instruction> code;
        private int pos;

        public Context(List<Instruction> code) {
            this.code = code;
        }

        public Optional<List<Instruction>> slice(int size) {
            if (size > 0 && pos + size <= code.size()) {
                return Optional.of(code.subList(pos, pos+size));
            }
            return Optional.empty();
        }

        @Override
        public boolean hasNext() {
            return pos < code.size();
        }
        @Override
        public Instruction next() {
            if (pos >= code.size()) {
                throw new NoSuchElementException();
            }
            return code.get(pos++);
        }
        @Override
        public void remove() {
            if (pos >= code.size()) {
                throw new IllegalStateException();
            }
            removeAt(pos);
        }
        public void removeAt(int i) {
            code.remove(i);
        }
    }
}
