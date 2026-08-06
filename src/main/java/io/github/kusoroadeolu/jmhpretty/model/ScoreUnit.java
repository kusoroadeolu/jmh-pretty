package io.github.kusoroadeolu.jmhpretty.model;

import java.util.Locale;

public record ScoreUnit(Kind kind, String raw) {

    public enum Kind {
        LATENCY,
        THROUGHPUT
    }

    public static ScoreUnit parse(String jmhUnit) {
        if (jmhUnit == null || jmhUnit.isBlank()) {
            throw new IllegalArgumentException("Missing scoreUnit");
        }

        String u = jmhUnit.trim().toLowerCase(Locale.ROOT);
        if (u.endsWith("/op")) return new ScoreUnit(Kind.LATENCY, jmhUnit);

        if (u.startsWith("ops/")) return new ScoreUnit(Kind.THROUGHPUT, jmhUnit);

        throw new IllegalArgumentException("Unrecognized JMH scoreUnit shape: '" + jmhUnit + "'");
    }


}