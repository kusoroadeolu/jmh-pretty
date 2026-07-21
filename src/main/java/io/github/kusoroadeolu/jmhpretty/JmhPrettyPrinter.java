package io.github.kusoroadeolu.jmhpretty;

import io.github.kusoroadeolu.jmhpretty.engine.TableRenderer;
import io.github.kusoroadeolu.jmhpretty.mapper.RawResultMapper;
import io.github.kusoroadeolu.jmhpretty.model.ParsedRun;
import io.github.kusoroadeolu.jmhpretty.model.RenderTheme;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

public class JmhPrettyPrinter {
    private final TableRenderer renderer;

    private JmhPrettyPrinter(TableRenderer renderer) {
        this.renderer = renderer;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void print(String filePath) {
        Objects.requireNonNull(filePath);
        try (Reader reader = new FileReader(filePath)) {
            ParsedRun run = RawResultMapper.parse(reader);
            clearScreen();
            renderer.render(run);
        } catch (IOException e) {
            System.err.println("Failed to read file: %s. Error: %s".formatted(filePath, e.getMessage()));
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to parse JMH JSON: " + e.getMessage());
            System.exit(1);
        }
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
            return new JmhPrettyPrinter(new TableRenderer(verbose, theme));
        }


    }

    private static void clearScreen() {
        System.out.print("\033[2J\033[H");
        System.out.flush();
    }
}
