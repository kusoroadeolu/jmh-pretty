package io.github.kusoroadeolu.jmhpretty;

/**
 * A JMH Pretty Printer which uses default configs.
 * Mainly here to reduce the builder boilerplate when
 * constructing a default printer
 * */
public final class DefaultJmhPrettyPrinter {
    private DefaultJmhPrettyPrinter() {}

    public static void print(String filePath) {
        JmhPrettyPrinter.builder().build().print(filePath);
    }
}
