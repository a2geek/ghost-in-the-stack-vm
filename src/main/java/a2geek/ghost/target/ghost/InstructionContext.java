package a2geek.ghost.target.ghost;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class InstructionContext implements Iterator<Instruction> {
        private List<Instruction> code;
        private int pos;
        private int origSize;

        public InstructionContext(List<Instruction> code) {
            this.code = code;
        }

        public Optional<List<Instruction>> slice(int size) {
            origSize = code.size();
            if (size > 0 && pos + size <= code.size()) {
                return Optional.of(code.subList(pos, pos+size));
            }
            return Optional.empty();
        }

        @Override
        public boolean hasNext() {
            return pos < code.size();
        }
        @Override
        public Instruction next() {
            if (pos >= code.size()) {
                throw new NoSuchElementException();
            }
            var inst = code.get(pos);
            if (origSize == code.size()) {
                // Only advance if we did NOT modify the list; otherwise we "magically" skip elements
                pos+= 1;
            }
            return inst;
        }
    }