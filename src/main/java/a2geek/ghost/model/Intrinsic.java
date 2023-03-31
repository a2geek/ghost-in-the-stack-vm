package a2geek.ghost.model;

import java.util.Set;

public interface Intrinsic {
    public static final String CPU_REGISTER_A = "cpu.register.a";
    public static final String CPU_REGISTER_X = "cpu.register.x";
    public static final String CPU_REGISTER_Y = "cpu.register.y";
    public static final Set<String> CPU_REGISTERS = Set.of(CPU_REGISTER_A, CPU_REGISTER_X, CPU_REGISTER_Y);
}
