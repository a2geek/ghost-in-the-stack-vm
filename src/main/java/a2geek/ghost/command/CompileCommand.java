package a2geek.ghost.command;

import a2geek.ghost.antlr.ParseUtil;
import a2geek.ghost.model.basic.ModelBuilder;
import a2geek.ghost.model.basic.Scope;
import a2geek.ghost.model.basic.scope.Program;
import a2geek.ghost.model.basic.visitor.ConstantReductionVisitor;
import a2geek.ghost.model.basic.visitor.DeadCodeEliminationVisitor;
import a2geek.ghost.model.basic.visitor.StrengthReductionVisitor;
import a2geek.ghost.target.ghost.CodeGenerationVisitor;
import a2geek.ghost.target.ghost.Instruction;
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

@Command(name = "compile", mixinStandardHelpOptions = true, 
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

    @Option(names = { "--bounds-checking" }, negatable = true, defaultValue = "true",
            fallbackValue = "true", showDefaultValue = Visibility.ALWAYS,
            description = "perform bounds checking on arrays")
    private boolean boundsChecking;

    @Option(names = { "-l", "--listing" }, description = "create listing file")
    private Optional<String> programListing;

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
        model.setTrace(traceFlag);
        model.setBoundsCheck(boundsChecking);
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

        ConstantReductionVisitor constantReductionVisitor = new ConstantReductionVisitor();
        constantReductionVisitor.visit(program);
        StrengthReductionVisitor rewriteVisitor = new StrengthReductionVisitor();
        rewriteVisitor.visit(program);
        DeadCodeEliminationVisitor deadCodeEliminationVisitor = new DeadCodeEliminationVisitor();
        deadCodeEliminationVisitor.visit(program);

        if (!quietFlag) {
            System.out.println("=== REWRITTEN ===");
            System.out.println(program);
        }

        CodeGenerationVisitor codeGenerationVisitor = new CodeGenerationVisitor();
        codeGenerationVisitor.visit(program);

        // Fixme: Ugly code.
        int loops = 5;
        List<Instruction> code = codeGenerationVisitor.getInstructions();
        while (loops > 0 && PeepholeOptimizer.optimize(code) > 0) {
            loops--;
        }

        // Assembly first pass: Figure out label values
        Map<String,Integer> addrs = new HashMap<>();
        int addr = 0;
        for (Instruction instruction : code) {
            if (instruction.isLabelOnly()) {
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

        programListing.ifPresent(filename -> {
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

    public static class ByteFormatter {
        private ByteArrayInputStream data;
        public static ByteFormatter from(byte[] bytes) {
            ByteFormatter bf = new ByteFormatter();
            bf.data = new ByteArrayInputStream(bytes);
            return bf;
        }

        public boolean hasMore() {
            return data.available() > 0;
        }

        public String get(int nBytes) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            for (int i=0; i<nBytes; i++) {
                int b = data.read();
                if (b == -1) break;
                pw.printf("%02x ", b);
            }
            return sw.toString();
        }
    }
}
