package a2geek.ghost.model;

import java.util.Set;

public interface Intrinsic {
    String CPU_REGISTER_A = "cpu.register.a";
    String CPU_REGISTER_X = "cpu.register.x";
    String CPU_REGISTER_Y = "cpu.register.y";
    Set<String> CPU_REGISTERS = Set.of(CPU_REGISTER_A, CPU_REGISTER_X, CPU_REGISTER_Y);
}
