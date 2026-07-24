# jmhpretty

A CLI tool that turns raw [JMH](https://openjdk.org/projects/code-tools/jmh/) `-rf json` output into readable, color-coded terminal tables instead of squinting at a giant JSON blob.

## Features

- Renders normal benchmarks and `@Group` benchmarks with different, purpose-built layouts.
- Relative color-coding per benchmark group: best result, worst result, and "close to best / close to worst" shades, computed independently for each param combination.
- Mode-aware: percentile columns (`p99`, `p99.9`, `p99.99`, `max`) are only shown for `SAMPLE` / `ALL` modes, since they're meaningless for `THRPT`, `AVGT`, and `SS`.
- `--verbose` mode expands percentiles to the full set: `p00, p50, p90, p95, p99, p99.9, p99.99, p99.999, p99.9999, max`.
- For `@Group` benchmarks, each role gets its own row (colored relative to other variants of that same role), followed by a dimmed/italicized aggregate row summing the group.
- Clears the screen and prints a legend before rendering, so color meaning is always visible.

## Requirements

- **JDK 21+** (the JSON mapper uses `SequencedMap`).
- A JMH results file produced with `-rf json`, e.g.:

  ```bash
  java -jar your-benchmarks.jar -rf json -rff results.json
  ```

## Installation

> Maven is required

```bash
git clone https://github.com/kusoroadeolu/jmhpretty.git
cd jmhpretty
mvn clean package
```

This produces a runnable jar.

## Usage

```bash
jmhpretty <path-to-jmh-results.json> [--verbose | -v]
```

Or, running the jar directly:

```bash
java -jar jmhpretty.jar results.json
java -jar jmhpretty.jar results.json --verbose
```

- `<path-to-jmh-results.json>`: required. Can be relative or absolute; relative paths are resolved against the current working directory.
- `--verbose` / `-v`: optional. Expands the percentile columns shown for `SAMPLE`/`ALL` mode benchmarks.

If the path points to a directory instead of a file, `jmhpretty` exits with an error rather than guessing.

## Output

Each benchmark is rendered as its own framed table, titled with the benchmark name. Columns, in order:

1. One column per `@Param` key (union of all param keys seen for that benchmark, in first-encountered order).
2. `Role`: **only for `@Group` benchmarks**, naming the group role (e.g. `readerEmpty`, `deleteMin_4_4`).
3. `Score`
4. `Error` (`±`, or `NaN` if JMH didn't report one).
5. Percentile columns: **only for `SAMPLE`/`ALL` mode**.
6. `Unit` (raw JMH score unit, e.g. `ops/s`, `ns/op`).

A legend line above the tables shows what the "best" and "worst" colors mean (fastest/best vs. slowest/worst), themed via [Nord](https://www.nordtheme.com/).

### Color coding

For any benchmark with 2+ variants, `jmhpretty` computes a best/worst index per group of scores (scoped per `@Group` role where applicable):

| Verdict | Meaning |
|---|---|
| **Best** | The single best score in its comparison set |
| **Worst** | The single worst score in its comparison set |
| **Relative best** | Within a tolerance band of the best score |
| **Relative worst** | Within a tolerance band of the worst score |
| Neutral | Everything else (unstyled) |

Direction (higher-is-better vs. lower-is-better) is derived from the score unit (`ops/*` = throughput = higher is better; `*/op` = latency = lower is better), so mixed-mode comparisons stay correct.

### `@Group` benchmarks

For benchmarks using JMH's `@Group`, each variant produces one row per role (from `secondaryMetrics`), colored relative to that role across all param combinations, plus a trailing **aggregate** row (dimmed, italicized, never colored) summing the whole group for that param combination.

## What gets parsed

From each JMH JSON entry, `jmhpretty` reads: `benchmark`, `mode`, `params`, and `primaryMetric` (`score`, `scoreError`, `scoreUnit`, `scorePercentiles`), plus `secondaryMetrics` for `@Group` roles.

It deliberately ignores: run metadata (JVM, JDK/VM version, JMH version, warmup/measurement iteration counts, JVM args), `rawData`, `rawDataHistogram`, and `scoreConfidence`, none of which affect the rendered comparison.

## License

MIT