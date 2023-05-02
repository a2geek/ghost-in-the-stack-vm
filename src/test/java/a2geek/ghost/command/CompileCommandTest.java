package a2geek.ghost.command;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CompileCommandTest {
    @Test
    public void testConvertControlCharacterMarkers() {
        assertEquals(new String(new char[] {7}), CompileCommand.convertControlCharacterMarkers("<CTRL-G>"));
    }
}
