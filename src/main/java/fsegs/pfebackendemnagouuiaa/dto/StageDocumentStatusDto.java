package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.entities.StatutDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Décrit l'état d'un document de stage (convention, fiche d'évaluation, cahier de stage).
 *
 * <p>{@code disponible} est {@code true} uniquement lorsque <strong>toutes</strong> les signatures
 * obligatoires sont présentes. Aucune validation manuelle du responsable des stages n'est requise
 * pour passer à {@code true} : la vérification est entièrement automatique.</p>
 *
 * <p>{@code statutDocument} expose le cycle de vie via l'enum {@link StatutDocument}.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageDocumentStatusDto {
    private String code;
    private String libelle;
    private Long documentId;

    /**
     * {@code true} si et seulement si toutes les signatures obligatoires sont présentes
     * et que le document peut être généré en PDF.
     * La valeur est calculée automatiquement — aucune action manuelle du responsable
     * des stages n'est nécessaire.
     */
    private Boolean disponible;
    private Boolean genere;
    private Boolean generationAutorisee;

    /**
     * Libellé de statut lisible (ex. "Disponible", "En attente de signatures").
     * Préférer {@link #statutDocument} pour les comparaisons programmatiques.
     */
    private String statut;

    /**
     * Message explicatif affiché dans l'interface lorsque {@code disponible = false}.
     * Liste les signatures manquantes séparées par des espaces.
     */
    private String raisonAbsence;

    private Boolean signeeParResponsableUniversitaire;
    private String dateSignatureResponsableUniversitaire;

    /**
     * Cycle de vie du document exprimé sous forme d'enum typé.
     * Calculé automatiquement à partir des signatures — sans intervention manuelle.
     */
    private StatutDocument statutDocument;
}
