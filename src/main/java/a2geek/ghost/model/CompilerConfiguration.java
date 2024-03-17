package a2geek.ghost.model;

import java.util.function.Function;

public class CompilerConfiguration {
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private CompilerConfiguration config = new CompilerConfiguration();

        public Builder caseStrategy(Function<String,String> caseStrategy) {
            config.caseStrategy = caseStrategy;
            return this;
        }
        public Builder controlCharsFn(Function<String,String> controlCharsFn) {
            config.controlCharsFn = controlCharsFn;
            return this;
        }

        public CompilerConfiguration get() {
            return config;
        }
    }
}
