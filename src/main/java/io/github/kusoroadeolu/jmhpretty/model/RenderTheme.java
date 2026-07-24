package io.github.kusoroadeolu.jmhpretty.model;

import io.github.kusoroadeolu.clique.Clique;
import io.github.kusoroadeolu.clique.style.Ink;

public record RenderTheme(
        Ink best, Ink worst, Ink relativeBest,
        Ink relativeWorst, Ink header, Ink title,
        Ink aggregate, Ink legend
) {
    private static final Ink BASE = new Ink();
    public static final Ink DIM = BASE.dim();

    public static final RenderTheme DEFAULT = defaultTheme();
    public static final RenderTheme NONE = noneTheme();

    private static RenderTheme noneTheme() {

        Ink base = BASE;

        return new RenderTheme(
                base,
                base,
                base,
                base,
                base,
                base,
                base,
                base
        );
    }


    private static RenderTheme defaultTheme() {
        Clique.registerTheme("nord");
        Ink base = BASE;
        Ink bold = base.bold();

        return new RenderTheme(
                bold.of("nord_green"),
                bold.of("nord_red"),
                base,
                base,
                bold.of("nord_frost1"),
                bold.of("nord_snow"),
                base.of("nord_polar3").italic(),
                base.of("nord_frost0")
        );
    }
}
