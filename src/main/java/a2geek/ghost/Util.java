package a2geek.ghost;

/**
 * Handy utilities.
 */
public class Util {
    private Util() {
        // prevent construction
    }

    /**
     * Create a RuntimeException with "printf" format specifiers.
     */
    public static RuntimeException errorf(String fmt, Object... args) {
        return new RuntimeException(String.format(fmt, args));
    }

}
