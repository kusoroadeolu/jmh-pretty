package io.github.kusoroadeolu.jmhpretty.model;
public enum Mode {
    THRPT,
    AVGT,
    SAMPLE,
    SS,
    ALL;

    public static Mode fromJmhCode(String code) {
        if (code == null) throw new IllegalArgumentException("Missing JMH mode field.");
        return switch (code.trim().toLowerCase()) {
            case "thrpt" -> THRPT;
            case "avgt" -> AVGT;
            case "sample" -> SAMPLE;
            case "ss" -> SS;
            case "all" -> ALL;
            default -> throw new IllegalArgumentException(
                    "Unrecognized JMH mode: '%s'".formatted(code));
        };
    }

    /** True when a higher score is the better result (throughput). */
    public boolean higherIsBetter() {
        return this == THRPT;
    }

    /** Percentile columns are only meaningful for sample/all — never shown for thrpt/avgt/ss. */
    public boolean showsPercentiles() {
        return this == SAMPLE || this == ALL;
    }
}
