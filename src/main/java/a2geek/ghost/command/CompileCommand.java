package a2geek.ghost.command;

import a2geek.ghost.antlr.GhostBasicVisitor;
import a2geek.ghost.antlr.generated.BasicLexer;
import a2geek.ghost.antlr.generated.BasicParser;
import a2geek.ghost.model.Scope;
import a2geek.ghost.model.scope.Program;
import a2geek.ghost.model.visitor.RewriteVisitor;
import a2geek.ghost.target.ghost.CodeGenerationVisitor;
import a2geek.ghost.target.ghost.Instruction;
import io.github.applecommander.applesingle.AppleSingle;
import org.antlr.v4.runtime.*;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(name = "compile", mixinStandardHelpOptions = true, 
    description = "Compile Ghost BASIC program.")
public class CompileCommand implements Callable<Integer> {
    public static final String INTERPRETER = "/interp-base.as";
    public static final String DEBUG_INTERPRETER = "/interp-debug-base.as";
    @Parameters(index = "0", description = "program to compile")
    private Path sourceCode;

    @Option(names = { "-o", "--output" }, description = "output file name",
            defaultValue = "a.out", showDefaultValue = Visibility.ALWAYS)
    private Path outputFile;

    @Option(names = { "--debug" }, description = "use the debugging interpreter")
    private boolean debugFlag;

    @Option(names = { "--case-sensitive" },
            defaultValue = "false", showDefaultValue = Visibility.ALWAYS,
            description = "allow identifiers to be case sensitive (A is different from a)")
    private boolean caseSensitive;

    @Option(names = { "-l", "--listing" }, description = "create listing file")
    private Optional<String> programListing;

    @Override
    public Integer call() throws Exception {
        CharStream stream = CharStreams.fromPath(sourceCode);
        BasicLexer lexer = new BasicLexer(stream);
        lexer.addErrorListener(ConsoleErrorListener.INSTANCE);
        TokenStream tokens = new CommonTokenStream(lexer);
        BasicParser parser = new BasicParser(tokens);
        parser.addErrorListener(ConsoleErrorListener.INSTANCE);

        BasicParser.ProgramContext context = parser.program();
        GhostBasicVisitor visitor = new GhostBasicVisitor(caseSensitive ?
                s -> s : String::toUpperCase);
        visitor.visit(context);

        Program program = visitor.getProgram();

        System.out.println("=== MODEL ===");
        System.out.println(program);
        System.out.println("=== VARIABLES ===");
        var allScopes = new ArrayList<>(program.getScopes());
        allScopes.add(program);
        for (Scope scope : allScopes) {
            System.out.printf("%s - %s\n", scope.getName(), scope.getLocalVariables());
        }

        RewriteVisitor rewriteVisitor = new RewriteVisitor();
        rewriteVisitor.visit(program);

        System.out.println("=== REWRITTEN ===");
        System.out.println(program);

        CodeGenerationVisitor codeGenerationVisitor = new CodeGenerationVisitor();
        codeGenerationVisitor.visit(program);

        // Assembly first pass: Figure out label values
        Map<String,Integer> addrs = new HashMap<>();
        int addr = 0;
        for (Instruction instruction : codeGenerationVisitor.getInstructions()) {
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
        for (Instruction instruction : codeGenerationVisitor.getInstructions()) {
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

        return 0;
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
