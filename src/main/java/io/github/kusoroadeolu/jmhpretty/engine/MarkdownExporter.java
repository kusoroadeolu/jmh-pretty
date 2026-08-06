package io.github.kusoroadeolu.jmhpretty.engine;

import io.github.kusoroadeolu.clique.Clique;
import io.github.kusoroadeolu.clique.configuration.TableType;
import io.github.kusoroadeolu.jmhpretty.model.ParsedRun;
import io.github.kusoroadeolu.jmhpretty.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

//TODO: Still experimental, working on making the api more cohesive
public final class MarkdownExporter {

    public static void exportToFile(String jsonPath, String mdPath, ResultRenderer builder) {
        ParsedRun run = FileUtils.readJmhJson(Objects.requireNonNull(jsonPath));
        Path p = FileUtils.toPath(mdPath);

        if (!Files.isRegularFile(p)) throw new RuntimeException("Provided path %s is not a file path!".formatted(mdPath));

        String output = builder.buildOutput(run, TableType.MARKDOWN, false);
        String cleaned = System.lineSeparator() + System.lineSeparator() +
                Clique.parser().getOriginalString(output);//remove reset ansi used in building frames

        try {
            Files.writeString(p, cleaned, StandardOpenOption.APPEND);
        }catch (IOException e) {
            throw new RuntimeException("Failed to write to path: " + p);
        }
    }
}
