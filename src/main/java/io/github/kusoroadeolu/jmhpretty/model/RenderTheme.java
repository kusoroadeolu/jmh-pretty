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

    private static RenderTheme defaultTheme() {
        Clique.registerTheme("catppuccin-mocha");
        Ink base = BASE;
        Ink bold = base.bold();
        return new RenderTheme(
                base.of("ctp_green"), base.of("ctp_red"), base.of("ctp_teal"),
                base.of("ctp_maroon"), bold.of("ctp_mauve"), bold.of("ctp_blue"),
                base.of("ctp_subtext1").italic(), base.of("ctp_sky")
        );

    }
}
