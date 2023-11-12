package a2geek.ghost.command;

import a2geek.ghost.antlr.ParseUtil;
import a2geek.ghost.command.util.ByteFormatter;
import a2geek.ghost.command.util.IntegerTypeConverter;
import a2geek.ghost.command.util.PrettyPrintVisitor;
import a2geek.ghost.model.ModelBuilder;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.visitor.ConstantReductionVisitor;
import a2geek.ghost.model.visitor.DeadCodeEliminationVisitor;
import a2geek.ghost.model.visitor.StrengthReductionVisitor;
import a2geek.ghost.target.ghost.CodeGenerationVisitor;
import a2geek.ghost.target.ghost.Instruction;
import a2geek.ghost.target.ghost.LabelOptimizer;
import a2geek.ghost.target.ghost.PeepholeOptimizer;
import io.github.applecommander.applesingle.AppleSingle;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static picocli.CommandLine.ArgGroup;

@Command(name = "compile", mixinStandardHelpOptions = true, usageHelpAutoWidth = true,
    description = "Compile Ghost BASIC program.")
public class CompileCommand implements Callable<Integer> {
    public static final String INTERPRETER = "/interp-base.as";
    public static final String DEBUG_INTERPRETER = "/interp-debug-base.as";
    @Parameters(index = "0", description = "program to compile")
    private Path sourceCode;

    private Language language = Language.BASIC;
    @Option(names = { "--integer" }, description = "integer basic program")
    private void selectInteger(boolean flag) {
        language = Language.INTEGER_BASIC;
    }

    @Option(names = { "-o", "--output" }, description = "output file name",
            defaultValue = "a.out", showDefaultValue = Visibility.ALWAYS)
    private Path outputFile;

    @Option(names = { "--debug" }, description = "use the debugging interpreter")
    private boolean debugFlag;

    @Option(names = { "--quiet" }, description = "reduce output")
    private boolean quietFlag;

    @Option(names = { "--trace" }, description = "enable stack traces")
    private boolean traceFlag;

    @Option(names = { "--case-sensitive" },
            defaultValue = "false", showDefaultValue = Visibility.ALWAYS,
            description = "allow identifiers to be case sensitive (A is different from a)")
    private boolean caseSensitive;

    @Option(names = { "--fix-control-chars" },
            defaultValue = "false",
            description = "replace '<CONTROL-?>' with the actual control character")
    private boolean fixControlChars;

    @Option(names = { "--heap" },
            defaultValue = "false",
            description = "allocate memory on heap")
    private boolean heapAllocationFlag;

    @Option(names = { "--heap-start", "--lomem" }, defaultValue = "0x8000",
            converter = IntegerTypeConverter.class,
            description = "heap start address (default: ${DEFAULT-VALUE})")
    private int heapStartAddress;

    @ArgGroup(exclusive = false, heading = "Optimizations:%n")
    private OptimizationFlags optimizations = new OptimizationFlags();

    @Option(names = { "-il", "--intermediate-code-listing" }, description = "create intermediate code listing file")
    private Optional<String> intermediateCodeListing;

    @Option(names = { "-tl", "--target-code-listing" }, description = "create listing file")
    private Optional<String> targetCodeListing;

    public static String convertControlCharacterMarkers(String value) {
        Pattern pattern = Pattern.compile("<CTRL-(.)>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(value);
        Set<String> letters = matcher.results()
            .map(mr -> mr.group(1))
            .collect(Collectors.toSet());
        for (String letter : letters) {
            String ctrlString = String.format("<CTRL-%s>", letter);
            String ctrlChar = new String(Character.toChars(letter.charAt(0) - '@'));
            value = value.replace(ctrlString, ctrlChar);
        }
        return value;
    }

    @Override
    public Integer call() throws Exception {
        try {
            compile();
            return 0;
        } catch (Throwable t) {
            if (traceFlag) {
                t.printStackTrace();
            }
            else {
                System.err.println(t.getLocalizedMessage());
            }
            return -1;
        }
    }

    public void compile() throws IOException {
        CharStream stream = CharStreams.fromPath(sourceCode);
        ModelBuilder model = new ModelBuilder(caseSensitive ? s -> s : String::toUpperCase);
        if (fixControlChars) {
            model.setControlCharsFn(CompileCommand::convertControlCharacterMarkers);
        }
        if (heapAllocationFlag) {
            model.useMemoryForHeap(heapStartAddress);
        }
        model.setTrace(traceFlag);
        optimizations.apply(model);
        Program program = switch (language) {
            case INTEGER_BASIC -> ParseUtil.integerToModel(stream, model);
            case BASIC -> ParseUtil.basicToModel(stream, model);
        };

        if (!quietFlag) {
            System.out.println("=== MODEL ===");
            System.out.println(program);
            System.out.println("=== VARIABLES ===");
            var allScopes = new ArrayList<>(program.getScopes());
            allScopes.add(program);
            for (Scope scope : allScopes) {
                System.out.printf("%s - %s\n", scope.getName(), scope.getLocalSymbols());
            }
        }

        optimizations.apply(program);

        if (!quietFlag) {
            System.out.println("=== REWRITTEN ===");
            System.out.println(program);
        }
        intermediateCodeListing.ifPresent(filename -> {
            try {
                Files.write(Path.of(filename), PrettyPrintVisitor.format(program).getBytes());
            }
            catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });

        CodeGenerationVisitor codeGenerationVisitor = new CodeGenerationVisitor();
        codeGenerationVisitor.visit(program);

        List<Instruction> code = codeGenerationVisitor.getInstructions();
        optimizations.apply(code);

        // Assembly first pass: Figure out label values
        Map<String,Integer> addrs = new HashMap<>();
        int addr = 0;
        for (Instruction instruction : code) {
            if (instruction.opcode() == null && instruction.label() != null) {
                addrs.put(instruction.label(), addr);
            }
            addr += instruction.size();
        }

        // Assembly second pass: Generate output
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        addr = 0;
        for (Instruction instruction : code) {
            byte[] data = instruction.assemble(addrs);
            out.writeBytes(data);
            //
            ByteFormatter bf = ByteFormatter.from(data);
            if (instruction.opcode() != null) {
                pw.printf("%04x: %-10.10s  %s\n", addr, bf.get(data.length), instruction);
            }
            else if (instruction.directive() != null) {
                int lineaddr = addr;
                pw.printf("%04x: %-10.10s  %s\n", lineaddr, bf.get(3), instruction);
                lineaddr+= 3;
                while (bf.hasMore()) {
                    pw.printf("%04x: %s\n", lineaddr, bf.get(8));
                    lineaddr += 8;
                }
            }
            else {
                pw.printf("%04x: %-10.10s  %s\n", addr, "", instruction);
            }
            //
            addr += instruction.size();
        }

        targetCodeListing.ifPresent(filename -> {
            try {
                Files.write(Path.of(filename), sw.toString().getBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        saveAsAppleSingle(out.toByteArray());
    }

    public void saveAsAppleSingle(byte[] code) throws IOException {
        String asInterpreter = debugFlag ? DEBUG_INTERPRETER : INTERPRETER;
        AppleSingle as = AppleSingle.read(getClass().getResourceAsStream(asInterpreter));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.writeBytes(as.getDataFork());  // Interpreter code
        baos.writeBytes(code);              // Byte code program

        AppleSingle.builder()
                .access(as.getProdosFileInfo().getAccess())
                .auxType(as.getProdosFileInfo().getAuxType())
                .fileType(as.getProdosFileInfo().getFileType())
                .dataFork(baos.toByteArray())
                .build()
                .save(outputFile);
    }

    private enum Language {
        BASIC,
        INTEGER_BASIC
    }

    public static class OptimizationFlags {
        @Option(names = { "--optimizations" }, negatable = true, defaultValue = "false",
            fallbackValue = "false", showDefaultValue = Visibility.NEVER,
            description = "disable all optimizations")
        private boolean noOptimizations;

        @Option(names = { "--bounds-checking" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "perform bounds checking on arrays")
        private boolean boundsChecking;

        @Option(names = { "--constant-reduction" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "constant reduction")
        private boolean constantReduction;

        @Option(names = { "--strength-reduction" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "enable strength reduction")
        private boolean strengthReduction;

        @Option(names = { "--dead-code-elimination" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "enable dead code elimination")
        private boolean deadCodeElimination;

        @Option(names = { "--peephole-optimizer" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "enable peephole optimizer")
        private boolean peepholeOptimizer;

        @Option(names = { "--label-optimizer" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "enable label optimizer")
        private boolean labelOptimizer;

        public void apply(ModelBuilder model) {
            model.setBoundsCheck(boundsChecking);
        }

        public void apply(Program program) {
            if (constantReduction) {
                ConstantReductionVisitor constantReductionVisitor = new ConstantReductionVisitor();
                constantReductionVisitor.visit(program);
            }
            if (noOptimizations) {
                // Constant reduction must be present at this time since ASC("A") doesn't exist in runtime.
                return;
            }
            if (strengthReduction) {
                StrengthReductionVisitor rewriteVisitor = new StrengthReductionVisitor();
                rewriteVisitor.visit(program);
            }
            if (deadCodeElimination) {
                DeadCodeEliminationVisitor deadCodeEliminationVisitor = new DeadCodeEliminationVisitor();
                deadCodeEliminationVisitor.visit(program);
            }
        }

        public void apply(List<Instruction> code) {
            if (noOptimizations) {
                return;
            }
            if (labelOptimizer) {
                LabelOptimizer.optimize(code);
            }
            if (peepholeOptimizer) {
                int loops = 5;      // arbitrarily picking maximum number of passes
                while (loops > 0 && PeepholeOptimizer.optimize(code) > 0) {
                    if (labelOptimizer) {
                        LabelOptimizer.optimize(code);  // This gets impacted by the peephole optimizer as well
                    }
                    loops--;
                }
            }
        }
    }
}
