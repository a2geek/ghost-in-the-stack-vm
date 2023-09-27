package a2geek.ghost.target.ghost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class LabelOptimizer {
    private static final Pattern LINE_NUMBER = Pattern.compile("_L\\d+_\\d+", Pattern.CASE_INSENSITIVE);
    private static Map<String,String> replaceLabels = new HashMap<>();

    public static void optimize(List<Instruction> code) {
        replaceLabels.clear();
        // Pass 1: Gather metadata, delete duplicate labels
        var ctx = new InstructionContext(code);
        while (ctx.hasNext()) {
            pass1(ctx);
            ctx.next();
        }
        // Pass 2: Replace labels on instructions
        ctx = new InstructionContext(code);
        while (ctx.hasNext()) {
            pass2(ctx);
            ctx.next();
        }
    }

    static void pass1(InstructionContext ctx) {
        var twoInstructions = ctx.slice(2);
        if (twoInstructions.isPresent()) {
            var list = twoInstructions.get();
            var inst1 = list.get(0);
            var inst2 = list.get(1);
            // Tracking two consecutive labels and removing the first as unneeded... (skips if 1st label is line number format)
            if (inst1.isLabelOnly() && inst2.isLabelOnly() && !LINE_NUMBER.matcher(inst1.label()).matches()
                    && !replaceLabels.containsKey(inst1.label())) {
                list.remove(0);
                replaceLabels.put(inst1.label(), inst2.label());
                // if we already have A->B and we get B->C, make the first A->C to shorten work!
                replaceLabels.replaceAll((k,v) -> v.equals(inst1.label()) ? inst2.label() : v);
            }
        }
    }

    static void pass2(InstructionContext ctx) {
        var oneInstruction = ctx.slice(1);
        if (oneInstruction.isPresent()) {
            var list = oneInstruction.get();
            var inst = list.get(0);
            if (is(inst.opcode(), Opcode.LOADA, Opcode.IFTRUE, Opcode.IFFALSE, Opcode.GOTO, Opcode.GOSUB)
                    && replaceLabels.containsKey(inst.label())) {
                list.set(0, new Instruction(replaceLabels.get(inst.label()), inst.opcode(), null, null, null));
            }
        }
    }

    static boolean is(Opcode primary, Opcode... secondaries) {
        for (Opcode secondary : secondaries) {
            if (primary == secondary) return true;
        }
        return false;
    }
}
