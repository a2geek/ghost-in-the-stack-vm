package a2geek.ghost.model.statement;

import a2geek.ghost.model.DataType;
import a2geek.ghost.model.Expression;
import a2geek.ghost.model.Statement;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class CallSubroutine implements Statement {
    private static final String LORES_LIBRARY = "lores";
    private static final String MISC_LIBRARY = "misc";
    private static final String PRINT_LIBRARY = "print";
    private static final String TEXT_LIBRARY = "text";
    private static final Map<String, Descriptor> SUBS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static {
        SUBS.putAll(Map.of(
            "color", new Descriptor(LORES_LIBRARY, DataType.INTEGER),
            "gr", new Descriptor(LORES_LIBRARY),
            "hlin", new Descriptor(LORES_LIBRARY, DataType.INTEGER, DataType.INTEGER, DataType.INTEGER),
            "plot", new Descriptor(LORES_LIBRARY, DataType.INTEGER, DataType.INTEGER),
            "vlin", new Descriptor(LORES_LIBRARY, DataType.INTEGER, DataType.INTEGER, DataType.INTEGER)
        ));
        SUBS.putAll(Map.of(
            "innum", new Descriptor(MISC_LIBRARY, DataType.INTEGER),
            "prnum", new Descriptor(MISC_LIBRARY, DataType.INTEGER)
        ));
        SUBS.putAll(Map.of(
            "boolean", new Descriptor(PRINT_LIBRARY, DataType.BOOLEAN),
            "comma", new Descriptor(PRINT_LIBRARY),
            "integer", new Descriptor(PRINT_LIBRARY, DataType.INTEGER),
            "newline", new Descriptor(PRINT_LIBRARY),
            "string", new Descriptor(PRINT_LIBRARY, DataType.STRING)
        ));
        SUBS.putAll(Map.of(
            "flash", new Descriptor(TEXT_LIBRARY),
            "home", new Descriptor(TEXT_LIBRARY),
            "htab", new Descriptor(TEXT_LIBRARY, DataType.INTEGER),
            "inverse", new Descriptor(TEXT_LIBRARY),
            "normal", new Descriptor(TEXT_LIBRARY),
            "text", new Descriptor(TEXT_LIBRARY),
            "vtab", new Descriptor(TEXT_LIBRARY, DataType.INTEGER)
        ));
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
            String library,
            DataType... parameterTypes
    ) {

    }
}
