package io.github.kusoroadeolu.jmhpretty;

import io.github.kusoroadeolu.clique.configuration.TableType;
import io.github.kusoroadeolu.jmhpretty.engine.OutputBuilder;
import io.github.kusoroadeolu.jmhpretty.model.ParsedRun;
import io.github.kusoroadeolu.jmhpretty.model.RenderTheme;
import io.github.kusoroadeolu.jmhpretty.utils.FileUtils;

import java.util.Objects;

public class JmhPrettyPrinter {
    private final OutputBuilder builder;

    private JmhPrettyPrinter(OutputBuilder builder) {
        this.builder = builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void print(String filePath) {
        Objects.requireNonNull(filePath);
        ParsedRun run = FileUtils.readJmhJson(filePath);
        clearScreen();
        System.out.print(builder.buildOutput(run, TableType.COMPACT, true));
    }

    public static class Builder {
        private boolean verbose = false;
        private RenderTheme renderTheme;

        private Builder() {}

        public Builder verbose() {
            this.verbose = true;
            return this;
        }

        public Builder renderTheme(RenderTheme renderTheme) {
            this.renderTheme = renderTheme;
            return this;
        }

        public JmhPrettyPrinter build() {
            RenderTheme theme = renderTheme == null ? RenderTheme.DEFAULT : renderTheme;
            return new JmhPrettyPrinter(new OutputBuilder(verbose, theme));
        }


    }

    private static void clearScreen() {
        System.out.print("\033[2J\033[H");
        System.out.flush();
    }
}
