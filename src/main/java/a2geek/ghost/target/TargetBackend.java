package a2geek.ghost.target;

import a2geek.ghost.model.scope.Program;

import java.nio.file.Path;

/**
 * TargetBackend is a <em>stateless</em> code generator.
 * Any state is managed in the <code>Code</code> or
 * <code>Binary</code> interfaces.
 *
 * @see Code
 * @see Binary
 */
public interface TargetBackend {
    Code generate(Program program);

    /**
     * Manages target code.
     */
    interface Code {
        Code optimize(OptimizationFlags optimizations);
        Binary assemble();
        void writeSource(Path path);
    }

    /**
     * Manages binary code (real instructions or virtual, depending on target).
     */
    interface Binary {
        void writeSource(Path path);
        byte[] getBytes();
    }

    record OptimizationFlags(boolean peephole, boolean labels) {}
}
