package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.entities.PartieEvaluation;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Catalogue des critères obligatoires par partie (aligné sur le frontend).
 */
public final class EvaluationCriteriaCatalog {

  public static final String SIGNATURE_INCOMPLETE_MESSAGE =
          "Veuillez compléter la fiche avant de signer.";

  private static final int MIN_REQUIRED_TEXT_LENGTH = 4;

  private static final List<String> ENCADRANT_PROFESSIONNEL_LABELS = List.of(
          "Qualité du travail",
          "Communication professionnelle",
          "Respect des consignes",
          "Autonomie"
  );

  private static final List<String> RESPONSABLE_ENTREPRISE_LABELS = List.of(
          "Ponctualité"
  );

  private EvaluationCriteriaCatalog() {
  }

  public static List<String> requiredLabelsFor(PartieEvaluation partie) {
    if (partie == PartieEvaluation.ENCADRANT_PROFESSIONNEL) {
      return ENCADRANT_PROFESSIONNEL_LABELS;
    }
    if (partie == PartieEvaluation.RESPONSABLE_ENTREPRISE) {
      return RESPONSABLE_ENTREPRISE_LABELS;
    }
    return List.of();
  }

  public static boolean isRequiredText(String value) {
    return value != null && value.trim().length() >= MIN_REQUIRED_TEXT_LENGTH;
  }

  public static String normalizeLabel(String value) {
    if (value == null) {
      return "";
    }
    return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT)
            .trim();
  }

  public static boolean matchesCriterionLabel(String actualLabel, String expectedLabel) {
    String actual = normalizeLabel(actualLabel);
    String expected = normalizeLabel(expectedLabel);
    if (actual.isEmpty() || expected.isEmpty()) {
      return false;
    }
    if (expected.contains("ponctual")) {
      return actual.contains("ponctual");
    }
    return actual.equals(expected);
  }
}
