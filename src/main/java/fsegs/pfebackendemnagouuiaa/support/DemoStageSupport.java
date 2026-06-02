package fsegs.pfebackendemnagouuiaa.support;

import fsegs.pfebackendemnagouuiaa.entities.Stage;

/**
 * Regles transverses pour les stages de demonstration (titres {@code [DEMO-FIN...]}).
 */
public final class DemoStageSupport {

    /** Prefixe commun des titres de stages fin de demonstration. */
    public static final String DEMO_FIN_TITLE_PREFIX = "[DEMO-FIN";

    private DemoStageSupport() {
    }

    public static boolean isDemoFinTitre(String titre) {
        return titre != null && titre.startsWith(DEMO_FIN_TITLE_PREFIX);
    }

    /**
     * Les stages DEMO-FIN portent une date de fin explicite en base : ne pas la recalculer via {@code duree}.
     */
    public static boolean mustPreserveExplicitEndDate(Stage stage) {
        return isDemoFinTitre(stage != null ? stage.getTitre() : null);
    }
}
