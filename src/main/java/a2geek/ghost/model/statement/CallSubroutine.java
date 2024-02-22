package a2geek.ghost.model.statement;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;
import a2geek.ghost.model.scope.Subroutine;

import java.util.*;
import java.util.stream.Collectors;

public class CallSubroutine implements Statement {
    private static final String LORES_LIBRARY = "lores";
    private static final String MISC_LIBRARY = "misc";
    private static final String RUNTIME_LIBRARY = "runtime";
    private static final String STRINGS_LIBRARY = "strings";
    private static final String TEXT_LIBRARY = "text";
    private static final Map<String, Descriptor> SUBS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static {
        Arrays.asList(
            // input
            new Descriptor("input_readline", RUNTIME_LIBRARY),
            new Descriptor("input_scanstring", RUNTIME_LIBRARY, DataType.STRING),
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
            new Descriptor("print_boolean", RUNTIME_LIBRARY, DataType.BOOLEAN),
            new Descriptor("print_comma", RUNTIME_LIBRARY),
            new Descriptor("print_integer", RUNTIME_LIBRARY, DataType.INTEGER),
            new Descriptor("print_byte", RUNTIME_LIBRARY, DataType.INTEGER),    // FIXME?
            new Descriptor("print_newline", RUNTIME_LIBRARY),
            new Descriptor("print_string", RUNTIME_LIBRARY, DataType.STRING),
            new Descriptor("print_address", RUNTIME_LIBRARY, DataType.ADDRESS),
            // runtime
            new Descriptor("out_of_bounds", RUNTIME_LIBRARY, DataType.STRING, DataType.INTEGER),
            // string
            new Descriptor("strcpy", STRINGS_LIBRARY, DataType.STRING, DataType.INTEGER, DataType.STRING, DataType.INTEGER, DataType.INTEGER),
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

    private final Subroutine subroutine;
    private List<Expression> parameters;

    public CallSubroutine(Subroutine subroutine, List<Expression> parameters) {
        this.subroutine = subroutine;
        this.parameters = parameters;
    }

    public Subroutine getSubroutine() {
        return subroutine;
    }

    public List<Expression> getParameters() {
        return parameters;
    }
    public void setParameters(List<Expression> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return String.format("CALL %s(%s)", subroutine.getFullPathName(),
                parameters.stream().map(Expression::toString).collect(Collectors.joining(", ")));
    }

    public record Descriptor(
        String name,
        String library,
        DataType... parameterTypes
    ) {
        public String fullName() {
            return String.format("%s.%s", library, name);
        }
    }
}
