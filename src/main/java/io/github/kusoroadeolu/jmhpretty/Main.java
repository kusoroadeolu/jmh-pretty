package io.github.kusoroadeolu.jmhpretty;

import io.github.kusoroadeolu.jmhpretty.utils.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;


public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsageAndExit();
        }

        boolean verbose = args.length > 1 && (args[1].equals("--verbose") || args[1].equals("-v"));

        String filePath = args[0];

        if (filePath == null || filePath.isBlank()) {
            printUsageAndExit();
        }

        //get file path, if the filepath isn't a full path, take the current path and add it to the given path
        Path p = FileUtils.toPath(filePath);

        if (!Files.isRegularFile(p)) {
            System.err.println("Error: not a valid file path: " + p);
            System.exit(1);
        }

        if (verbose) JmhPrettyPrinter.builder().verbose().build().print(p.toString());
        else JmhPrettyPrinter.builder().build().print(p.toString());
    }

    static void printUsageAndExit() {
        System.err.println("Usage: jmhpretty <path-to-jmh-results.json> [--verbose]");
        System.exit(1);
    }
}
