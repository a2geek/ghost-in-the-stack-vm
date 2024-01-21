package a2geek.ghost.target.ghost;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class LabelOptimizer {
    private static final Pattern LINE_NUMBER = Pattern.compile("_L\\d+_\\d+", Pattern.CASE_INSENSITIVE);
    private static Map<String,String> replaceLabels = new HashMap<>();
    private static Set<String> usedLabels = new HashSet<>();

    public static void optimize(List<Instruction> code) {
        replaceLabels.clear();
        // Pass 1: Gather metadata, delete duplicate labels
        process(code, LabelOptimizer::findAndCleanupIdenticalLabels);
        // Pass 2: Replace labels on instructions
        process(code, LabelOptimizer::replaceMovedLabels);

        usedLabels.clear();
        // Pass 3: Gather all used labels
        process(code, LabelOptimizer::populateUsedLabels);
        // Pass 4: Remove unused labels
        process(code, LabelOptimizer::removeUnusedLabels);
    }

    static void process(List<Instruction> code, Consumer<InstructionContext> consumer) {
        var ctx = new InstructionContext(code);
        while (ctx.hasNext()) {
            consumer.accept(ctx);
            ctx.next();
        }
    }

    static void populateUsedLabels(InstructionContext ctx) {
        var oneInstruction = ctx.slice(1);
        if (oneInstruction.isPresent()) {
            var inst = oneInstruction.get().getFirst();
            if (inst.opcode() != null && inst.label() != null) {
                usedLabels.add(inst.label());
            }
            else if (inst.directive() == Directive.CONSTANT
                     && inst.constantValue().constantType() == ConstantType.LABEL_ARRAY_LESS_1) {
                usedLabels.addAll(inst.constantValue().stringArray());
            }
        }
    }

    static void removeUnusedLabels(InstructionContext ctx) {
        var oneInstruction = ctx.slice(1);
        if (oneInstruction.isPresent()) {
            var list = oneInstruction.get();
            var inst = list.getFirst();
            if (inst.isLabelOnly()
                    && inst.label().startsWith("_")     // HACK: don't remove library labels, even if not used. TBD.
                    && !LINE_NUMBER.matcher(inst.label()).matches()
                    && !usedLabels.contains(inst.label())) {
                list.removeFirst();
            }
        }
    }

    static void findAndCleanupIdenticalLabels(InstructionContext ctx) {
        var twoInstructions = ctx.slice(2);
        if (twoInstructions.isPresent()) {
            var list = twoInstructions.get();
            var inst1 = list.get(0);
            var inst2 = list.get(1);
            // Tracking two consecutive labels and removing the first as unneeded... (skips if 1st label is line number format)
            if (inst1.isLabelOnly() && inst2.isLabelOnly() && !LINE_NUMBER.matcher(inst1.label()).matches()
                    && !replaceLabels.containsKey(inst1.label())) {
                list.removeFirst();
                replaceLabels.put(inst1.label(), inst2.label());
                // if we already have A->B and we get B->C, make the first A->C to shorten work!
                replaceLabels.replaceAll((k,v) -> v.equals(inst1.label()) ? inst2.label() : v);
            }
        }
    }

    static void replaceMovedLabels(InstructionContext ctx) {
        var oneInstruction = ctx.slice(1);
        if (oneInstruction.isPresent()) {
            var list = oneInstruction.get();
            var inst = list.getFirst();
            if (is(inst.opcode(), Opcode.LOADA, Opcode.IFNZ, Opcode.IFZ, Opcode.GOTO, Opcode.GOSUB)
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
