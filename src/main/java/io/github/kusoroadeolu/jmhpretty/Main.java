package io.github.kusoroadeolu.jmhpretty;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: jmhpretty <path-to-jmh-results.json> [--verbose]");
            System.exit(1);
        }

        boolean verbose = args.length > 1 && (args[1].equals("--verbose") || args[1].equals("-v"));

        String filePath = args[0];
        //get file path, if the filepath isn't a full path, take the current path and add it to the given path
        Path p = Path.of(filePath).normalize();
        if (!p.isAbsolute()) p = Path.of(".", filePath).toAbsolutePath().normalize();

        if (Files.isDirectory(p)) throw new IllegalArgumentException("Provided path is not a file path!");

        if (verbose) JmhPrettyPrinter.builder().verbose().build().print(p.toString());
        else JmhPrettyPrinter.builder().build().print(p.toString());
    }
}
