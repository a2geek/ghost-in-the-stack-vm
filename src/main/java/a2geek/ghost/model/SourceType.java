package a2geek.ghost.model;

/**
 * This enum is used to track where parts of a program originated.
 * This is primarily used to track source code from compiler generated
 * code. Compiler generated code is more subject to deletion (such as
 * duplicate array bound checks).
 * Note that the usage is not exhaustive and will really only be added
 * as the need arises.
 */
public enum SourceType {
    CODE,
    BOUNDS_CHECK
}
