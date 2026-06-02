package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.entities.StatutDocument;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Décrit l'état d'un document de stage (convention, fiche d'évaluation, cahier de stage).
 *
 * <p>{@code disponible} dépend du type de document (politique centralisée côté serveur) :
 * convention (sujet validé, signatures complètes ; indépendamment de la date de début) ;
 * cahier / fiche (signatures complètes et date &gt;= fin de stage).
 * Aucun rôle ne peut contourner ces règles.</p>
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
     * {@code true} si le PDF peut être consulté ou téléchargé selon la politique du document
     * ({@code CONVENTION}, {@code FICHE_EVALUATION}, {@code CAHIER_STAGE}).
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
     * Signataires autorisés pour ce document uniquement (aucun rôle hors workflow).
     */
    private List<DocumentSignatoryStatusDto> signataires = new ArrayList<>();

    /**
     * Cycle de vie du document exprimé sous forme d'enum typé.
     * Calculé automatiquement à partir des signatures — sans intervention manuelle.
     */
    private StatutDocument statutDocument;
}
