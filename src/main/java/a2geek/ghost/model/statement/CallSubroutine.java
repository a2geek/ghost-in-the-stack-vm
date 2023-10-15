package a2geek.ghost.model.statement;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

import java.util.*;
import java.util.stream.Collectors;

public class CallSubroutine implements Statement {
    private static final String INPUT_LIBRARY = "input";
    private static final String LORES_LIBRARY = "lores";
    private static final String MISC_LIBRARY = "misc";
    private static final String PRINT_LIBRARY = "print";
    private static final String RUNTIME_LIBRARY = "runtime";
    private static final String STRING_LIBRARY = "string";
    private static final String TEXT_LIBRARY = "text";
    private static final Map<String, Descriptor> SUBS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static {
        Arrays.asList(
            // input
            new Descriptor("readline", INPUT_LIBRARY),
            new Descriptor("scanstring", INPUT_LIBRARY, DataType.STRING),
            // lores
            new Descriptor("color", LORES_LIBRARY, DataType.INTEGER),
            new Descriptor("gr", LORES_LIBRARY),
            new Descriptor("hlin", LORES_LIBRARY, DataType.INTEGER, DataType.INTEGER, DataType.INTEGER),
            new Descriptor("plot", LORES_LIBRARY, DataType.INTEGER, DataType.INTEGER),
            new Descriptor("vlin", LORES_LIBRARY, DataType.INTEGER, DataType.INTEGER, DataType.INTEGER),
            // misc
            new Descriptor("innum", MISC_LIBRARY, DataType.INTEGER),
            new Descriptor("prnum", MISC_LIBRARY, DataType.INTEGER),
            // print
            new Descriptor("boolean", PRINT_LIBRARY, DataType.BOOLEAN),
            new Descriptor("comma", PRINT_LIBRARY),
            new Descriptor("integer", PRINT_LIBRARY, DataType.INTEGER),
            new Descriptor("newline", PRINT_LIBRARY),
            new Descriptor("string", PRINT_LIBRARY, DataType.STRING),
            new Descriptor("address", PRINT_LIBRARY, DataType.ADDRESS),
            // runtime
            new Descriptor("out_of_bounds", RUNTIME_LIBRARY, DataType.STRING, DataType.INTEGER),
            // string
            new Descriptor("strcpy", STRING_LIBRARY, DataType.STRING, DataType.INTEGER, DataType.STRING, DataType.INTEGER, DataType.INTEGER),
            // text
            new Descriptor("flash", TEXT_LIBRARY),
            new Descriptor("home", TEXT_LIBRARY),
            new Descriptor("htab", TEXT_LIBRARY, DataType.INTEGER),
            new Descriptor("inverse", TEXT_LIBRARY),
            new Descriptor("normal", TEXT_LIBRARY),
            new Descriptor("text", TEXT_LIBRARY),
            new Descriptor("vtab", TEXT_LIBRARY, DataType.INTEGER)
        ).forEach(d -> {
            SUBS.put(d.name(), d);
        });
    }
    public static Optional<Descriptor> getDescriptor(String name) {
        return Optional.ofNullable(SUBS.get(name));
    }

    private String name;
    private List<Expression> parameters;

    public CallSubroutine(String name, List<Expression> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public List<Expression> getParameters() {
        return parameters;
    }
    public void setParameters(List<Expression> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return String.format("CALL %s(%s)", name,
                parameters.stream().map(Expression::toString).collect(Collectors.joining(", ")));
    }

    public record Descriptor(
        String name,
        String library,
        DataType... parameterTypes
    ) {
        public String fullName() {
            return String.format("%s_%s", library(), name());
        }
    }
}
