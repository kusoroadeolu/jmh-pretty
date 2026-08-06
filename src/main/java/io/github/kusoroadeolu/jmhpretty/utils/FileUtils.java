package io.github.kusoroadeolu.jmhpretty.utils;

import io.github.kusoroadeolu.jmhpretty.mapper.RawResultMapper;
import io.github.kusoroadeolu.jmhpretty.model.ParsedRun;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;


public final class FileUtils {

    public static Path toPath(String path) {
        return Path.of(path).toAbsolutePath().normalize();
    }

    public static ParsedRun readJmhJson(String filePath) {
        try (Reader reader = new FileReader(filePath)) {
            return RawResultMapper.parse(reader);
        } catch (IOException e) {
            throw new ParseFailedException("Failed to read file: %s. Error: %s%n".formatted(filePath, e.getMessage()));
        } catch (IllegalArgumentException e) {
            throw new ParseFailedException("Failed to parse JMH JSON: " + e.getMessage());
        }
    }
}
