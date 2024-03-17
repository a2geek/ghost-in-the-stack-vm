package a2geek.ghost.model;

import java.util.function.Consumer;
import java.util.function.Function;

public class CompilerConfiguration {
    private boolean trace;
    private boolean boundsCheck = true;
    private Function<String,String> caseStrategy = s -> s;
    private Function<String,String> controlCharsFn = s -> s;

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

        public CompilerConfiguration get() {
            return config;
        }
    }
}
