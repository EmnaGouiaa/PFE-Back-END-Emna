package fsegs.pfebackendemnagouuiaa.services.pdf;

import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Formatage unifie des notes d'evaluation dans tous les PDF de stage (FSEGS).
 * Echelle unique : /5, moyenne arithmetique, sans score pondere.
 */
public final class InternshipPdfEvaluationFormat {

    public static final String FINAL_SCORE_SUBTITLE = "Moyenne arithmétique des notes sur 5";
    public static final String[] CRITERION_TABLE_HEADERS = {"Critère évalué", "Note / 5", "Commentaire"};

    private static final Locale LOCALE = Locale.US;

    private InternshipPdfEvaluationFormat() {
    }

    public static boolean hasDisplayableFinalScore(Double noteFinale) {
        return noteFinale != null && noteFinale > 0;
    }

    /** Valeur seule, ex. {@code 4.2 / 5} ou {@code — / 5}. */
    public static String formatFinalScoreValue(Double noteFinale) {
        if (!hasDisplayableFinalScore(noteFinale)) {
            return "— / 5";
        }
        return String.format(LOCALE, "%.1f / 5", noteFinale);
    }

    /** Libelle complet, ex. {@code Note finale : 4.2 / 5}. */
    public static String formatFinalScoreLabel(Double noteFinale) {
        if (!hasDisplayableFinalScore(noteFinale)) {
            return "Note finale : —";
        }
        return String.format(LOCALE, "Note finale : %.1f / 5", noteFinale);
    }

    /** Valeur compacte pour en-tete de document (sans suffixe /5). */
    public static String formatFinalScoreHeaderValue(Double noteFinale) {
        if (!hasDisplayableFinalScore(noteFinale)) {
            return "—";
        }
        return String.format(LOCALE, "%.1f", noteFinale);
    }

    public static String formatCriterionNote(Integer note) {
        return note != null ? String.valueOf(note) : "—";
    }

    public static String formatComment(String commentaire) {
        if (commentaire == null || commentaire.isBlank()) {
            return "—";
        }
        return commentaire.trim();
    }

    /** Lignes cle-valeur pour une section synthese d'evaluation (convention, cahier, etc.). */
    public static List<String[]> buildSummaryRows(FicheEvaluation fiche) {
        List<String[]> rows = new ArrayList<>();
        if (fiche == null) {
            return rows;
        }
        rows.add(new String[]{"Note finale", formatFinalScoreValue(fiche.getNoteFinale())});
        rows.add(new String[]{"Données complètes", fiche.donneesCompletes() ? "Oui" : "Non"});
        rows.add(new String[]{"Signatures", fiche.estCompletementSigne() ? "Complètes" : "En cours"});
        return rows;
    }

    /** Lignes du tableau des criteres (critere, note, commentaire). */
    public static List<String[]> buildCriterionRows(FicheEvaluation fiche) {
        List<String[]> rows = new ArrayList<>();
        if (fiche == null || fiche.getNotesAttribuees() == null) {
            return rows;
        }
        fiche.getNotesAttribuees().stream()
                .filter(n -> n.getCritereEvaluation() != null || n.getNote() != null)
                .sorted(Comparator.comparing(n -> n.getCritereEvaluation() == null
                        ? ""
                        : InternshipPdfTheme.safeText(n.getCritereEvaluation().getLibelle()),
                        String.CASE_INSENSITIVE_ORDER))
                .forEach(n -> rows.add(new String[]{
                        n.getCritereEvaluation() != null
                                ? InternshipPdfTheme.safeText(n.getCritereEvaluation().getLibelle())
                                : "—",
                        formatCriterionNote(n.getNote()),
                        formatComment(n.getCommentaire())
                }));
        return rows;
    }

    public static boolean hasEvaluationContent(FicheEvaluation fiche) {
        if (fiche == null) {
            return false;
        }
        if (hasDisplayableFinalScore(fiche.getNoteFinale())) {
            return true;
        }
        return fiche.getNotesAttribuees() != null
                && fiche.getNotesAttribuees().stream().anyMatch(NoteAttribuee::estEvalue);
    }
}
