package a2geek.ghost.model;

public interface Statement {
    /** Indicates how this code originated. Assuming from source code. */
    default SourceType getSource() {
        return SourceType.CODE;
    }
}
