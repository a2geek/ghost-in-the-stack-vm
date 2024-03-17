package a2geek.ghost.model;

import a2geek.ghost.model.expression.BooleanConstant;
import a2geek.ghost.model.expression.IntegerConstant;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class CompilerConfiguration {
    public static final String OPTION_HEAP = "option.heap";
    public static final String OPTION_HEAP_LOMEM = "option.heap.lomem";
    private boolean trace;
    private boolean boundsCheck = true;
    private Function<String,String> caseStrategy = s -> s;
    private Function<String,String> controlCharsFn = s -> s;
    private Map<String,Expression> defines = new HashMap<>();

    private CompilerConfiguration() {
        // prevent construction
    }

    public String applyCaseStrategy(String string) {
        return caseStrategy.apply(string);
    }
    public String applyControlCharsFn(String string) {
        return controlCharsFn.apply(string);
    }
    public void trace(String fmt, Object... args) {
        if (trace) {
            System.out.printf(fmt, args);
            System.out.println();
        }
    }
    public boolean isBoundsCheckEnabled() {
        return boundsCheck;
    }
    public Map<String,Expression> getDefines() {
        return defines;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private CompilerConfiguration config = new CompilerConfiguration();

        public Builder apply(Consumer<CompilerConfiguration.Builder> configurer) {
            configurer.accept(this);
            return this;
        }

        public Builder trace(boolean traceFlag) {
            config.trace = traceFlag;
            return this;
        }
        public Builder caseStrategy(Function<String,String> caseStrategy) {
            config.caseStrategy = caseStrategy;
            return this;
        }
        public Builder controlCharsFn(Function<String,String> controlCharsFn) {
            config.controlCharsFn = controlCharsFn;
            return this;
        }
        public Builder boundsCheckEnabled(boolean boundsCheckFlag) {
            config.boundsCheck = boundsCheckFlag;
            return this;
        }
        public Builder memoryConfig(boolean heapAllocationFlag, int heapStartAddress) {
            if (heapAllocationFlag) {
                config.defines.put(OPTION_HEAP, new BooleanConstant(true));
                config.defines.put(OPTION_HEAP_LOMEM, new IntegerConstant(heapStartAddress));
            }
            return this;
        }
        public Builder defines(Map<String,Expression> defines) {
            config.defines.putAll(defines);
            return this;
        }

        public CompilerConfiguration get() {
            return config;
        }
    }
}
