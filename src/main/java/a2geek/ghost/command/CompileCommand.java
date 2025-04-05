package a2geek.ghost.command;

import a2geek.ghost.TrackingLogger;
import a2geek.ghost.antlr.ParseUtil;
import a2geek.ghost.command.util.IntegerTypeConverter;
import a2geek.ghost.command.util.PrettyPrintVisitor;
import a2geek.ghost.model.*;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.visitor.*;
import a2geek.ghost.target.TargetBackend;
import a2geek.ghost.target.ghost.GhostBackend;
import io.github.applecommander.applesingle.AppleSingle;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static a2geek.ghost.TrackingLogger.LOGGER;
import static a2geek.ghost.model.Symbol.in;
import static picocli.CommandLine.ArgGroup;

@Command(name = "compile", mixinStandardHelpOptions = true, usageHelpAutoWidth = true,
    description = "Compile Ghost BASIC program.")
public class CompileCommand implements Callable<Integer> {
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

    @Option(names = { "--level" }, description = "set logging level",
            defaultValue = "WARNING", showDefaultValue = Visibility.ALWAYS)
    private void setLogLevel(TrackingLogger.Level level) {
        LOGGER.setLevel(level);
    }

    @Option(names = { "-D", "--define" }, mapFallbackValue = "true",
            description = "define variables (default: ${MAP-FALLBACK-VALUE})")
    private Map<String,Expression> definedValues = new HashMap<>();

    @Option(names = { "--case-sensitive" },
            defaultValue = "false", showDefaultValue = Visibility.ALWAYS,
            description = "allow identifiers to be case sensitive (A is different from a)")
    private boolean caseSensitive;

    @Option(names = { "--fix-control-chars" },
            defaultValue = "false",
            description = "replace '<CONTROL-?>' with the actual control character")
    private boolean fixControlChars;

    @ArgGroup(exclusive = false, heading = "Memory Configuration:%n")
    private MemoryConfig memoryConfig = new MemoryConfig();

    @ArgGroup(exclusive = false, heading = "Optimizations:%n")
    private OptimizationFlags optimizations = new OptimizationFlags();

    @Option(names = { "-il", "--intermediate-code-listing" }, description = "create intermediate code listing file")
    private Optional<String> intermediateCodeListing;

    @Option(names = { "-tl", "--target-code-listing" }, description = "create listing file")
    private Optional<String> targetCodeListing;

    @Option(names = { "--symbols" }, description = "dump symbol table to file")
    private Optional<String> symbolTableFile;

    @Option(names = { "--stage" }, description = "run compiler to specified stage", defaultValue = "OUTPUT")
    private Stage stage;

    @Option(names = { "-t", "--target" }, description = "target (virtual) processor")
    private Target target = Target.GHOSTVM;

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
        CompilerConfiguration config = CompilerConfiguration.builder()
                .caseStrategy(caseSensitive ? s -> s : String::toUpperCase)
                .controlCharsFn(fixControlChars ? CompileCommand::convertControlCharacterMarkers : s -> s)
                .apply(optimizations::configure)
                .apply(memoryConfig::configure)
                .defines(definedValues)
                .get();
        ModelBuilder model = new ModelBuilder(config);
        if (memoryConfig.heapAllocationFlag) {
            model.useMemoryForHeap(memoryConfig.heapStartAddress);
        }
        Program program = switch (language) {
            case INTEGER_BASIC -> ParseUtil.integerToModel(stream, model);
            case BASIC -> ParseUtil.basicToModel(stream, model);
        };

        if (!quietFlag) {
            System.out.println("=== MODEL ===");
            System.out.println(program);
            System.out.println("=== VARIABLES ===");
            var allScopes = new ArrayList<Scope>();
            program.findAllLocalScope(in(SymbolType.FUNCTION,SymbolType.SUBROUTINE)).forEach(symbol -> {
                allScopes.add(symbol.scope());
            });
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
        if (stage == Stage.INTERMEDIATE) {
            System.out.println("Intermediate code file written; stopping compilation.");
            return;
        }

        // From this point, working with target code!
        // Examples: GhostVM, BasicVM, Plasma, 6502, 65C02, 65816, etc.

        var backend = target.backend();
        var code = backend.generate(program);
        if (stage == Stage.TARGET) {
            targetCodeListing.map(Path::of).ifPresent(code::writeSource);
            System.out.println("Target code file written; stopping compilation.");
            System.exit(0);
        }
        var optimized = backend.optimize(code, optimizations.targetOptimizations());
        if (stage == Stage.OPTIMIZATION) {
            targetCodeListing.map(Path::of).ifPresent(optimized::writeSource);
            System.out.println("Optimized target code file written; stopping compilation.");
            System.exit(0);
        }
        var binout = backend.assemble(optimized);
        targetCodeListing.map(Path::of).ifPresent(binout::writeSource);
        symbolTableFile.map(Path::of).ifPresent(binout::writeSymbols);

        AppleSingle.builder()
                .access(0xc3)
                .auxType(0x803)
                .fileType(0x06) // BIN
                .dataFork(binout.getBytes())
                .build()
                .save(outputFile);
    }

    private enum Language {
        BASIC,
        INTEGER_BASIC
    }

    public static class MemoryConfig {
        @Option(names = { "--heap" },
                defaultValue = "false",
                description = "allocate memory on heap")
        private boolean heapAllocationFlag;

        @Option(names = { "--heap-start", "--lomem" }, defaultValue = "0x8000",
                converter = IntegerTypeConverter.class,
                description = "heap start address (default: ${DEFAULT-VALUE})")
        private int heapStartAddress;

        public void configure(CompilerConfiguration.Builder builder) {
            builder.memoryConfig(heapAllocationFlag, heapStartAddress);
        }
    }

    public static class OptimizationFlags {
        @Option(names = { "--optimizations" }, negatable = true, defaultValue = "false",
            fallbackValue = "false", showDefaultValue = Visibility.NEVER,
            description = "disable all optimizations (enabled: ${DEFAULT-VALUE})")
        private boolean noOptimizations;

        @Option(names = { "--bounds-checking" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "perform bounds checking on arrays (enabled: ${DEFAULT-VALUE})")
        private boolean boundsChecking;

        @Option(names = { "--constant-reduction" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "constant reduction (enabled: ${DEFAULT-VALUE})")
        private boolean constantReduction;

        @Option(names = { "--strength-reduction" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "enable strength reduction (enabled: ${DEFAULT-VALUE})")
        private boolean strengthReduction;

        @Option(names = { "--dead-code-elimination" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "enable dead code elimination (enabled: ${DEFAULT-VALUE})")
        private boolean deadCodeElimination;

        @Option(names = { "--peephole-optimizer" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "enable peephole optimizer (enabled: ${DEFAULT-VALUE})")
        private boolean peepholeOptimizer;

        @Option(names = { "--label-optimizer" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "enable label optimizer (enabled: ${DEFAULT-VALUE})")
        private boolean labelOptimizer;

        @Option(names = { "--code-inlining" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "enable code inlining (enabled: ${DEFAULT-VALUE})")
        private boolean codeInlining;

        @Option(names = { "--expression-rewrite" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "enable expression rewriting (enabled: ${DEFAULT-VALUE})")
        private boolean expressionRewriting;

        @Option(names = { "--subexpression-elimination" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "enable expression reduction (enabled: ${DEFAULT-VALUE})")
        private boolean subexpressionElimination;

        @Option(names = { "--temp-variable-consolidation" }, negatable = true, defaultValue = "true",
                fallbackValue = "true", showDefaultValue = Visibility.NEVER,
                description = "consolidate/reduce temporary variables (enabled: ${DEFAULT-VALUE})")
        private boolean tempVariableConsolidation;

        public void configure(CompilerConfiguration.Builder builder) {
            if (noOptimizations) {
                return;
            }
            builder.boundsCheckEnabled(boundsChecking);
        }

        public void apply(Program program) {
            if (noOptimizations) {
                return;
            }

            var optimizations = new ArrayList<Supplier<ProgramVisitor>>();
            if (constantReduction) {
                optimizations.add(ConstantReductionVisitor::new);
            }
            if (codeInlining) {
                optimizations.add(InliningVisitor::new);
                optimizations.add(InliningCleanupVisitor::new);
            }
            if (strengthReduction) {
                optimizations.add(StrengthReductionVisitor::new);
            }
            if (deadCodeElimination) {
                optimizations.add(DeadCodeEliminationVisitor::new);
            }
            if (expressionRewriting) {
                optimizations.add(ExpressionRewriteVisitor::new);
            }
            if (subexpressionElimination) {
                optimizations.add(SubexpressionEliminationVisitor::new);
            }
            if (tempVariableConsolidation) {
                optimizations.add(TempVariableConsolidationVisitor::new);
            }

            int counter = 0;
            int oldCounter = 0;
            do {
                oldCounter = counter;
                for (var optimizer : optimizations) {
                    var visitor = optimizer.get();
                    visitor.visit(program);
                    if (visitor instanceof RepeatingVisitor repeating) {
                        counter += repeating.getCounter();
                    }
                }
            } while (counter != oldCounter);
        }

        public TargetBackend.OptimizationFlags targetOptimizations() {
            return new TargetBackend.OptimizationFlags(this.peepholeOptimizer, this.labelOptimizer);
        }
    }

    enum Stage {
        INTERMEDIATE,
        TARGET,
        OPTIMIZATION,
        OUTPUT
    }

    enum Target {
        GHOSTVM(new GhostBackend());

        TargetBackend backend;

        Target(TargetBackend backend) {
            this.backend = backend;
        }

        TargetBackend backend() {
            return backend;
        }
    }
}
