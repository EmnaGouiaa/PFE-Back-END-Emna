package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Trace d'une signature électronique sur un document de stage (convention, cahier ou fiche).
 *
 * <p>Sans FK JPA vers {@link Utilisateur} : {@link #signataireId} est résolu à la demande
 * (PDF, DTO) via {@code UtilisateurRepository}. Le document parent porte la FK
 * ({@code convention_stage_id}, {@code cahier_stage_id} ou {@code fiche_evaluation_id}).</p>
 *
 * <h3>Consommation applicative</h3>
 * Services de documents ({@code ConventionStageServiceImpl}, {@code CahierStageServiceImpl},
 * {@code FicheEvaluationServiceImpl}, {@code StageDocumentServiceImpl}) et services PDF associés.
 */
@Entity
@Table(name = "signatures")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Signature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Rôle de la partie qui signe (identifie de manière unique la signature dans le document). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleSignature roleSignature;

    /**
     * Identifiant de l'utilisateur signataire.
     * Pas de contrainte FK JPA : charger via {@code UtilisateurRepository} si nécessaire.
     */
    private Long signataireId;

    /** Horodatage de la signature. */
    @Column(nullable = false)
    private LocalDateTime dateSignature;

    /**
     * Image de la signature capturee au moment de l'apposition (data URL base64 inline ou
     * URL pointant vers le fichier stocke). Obligatoire pour toute NOUVELLE signature : la
     * validation est effectuee au niveau du service. Cote DB la colonne est nullable
     * uniquement pour preserver la compatibilite avec d'eventuelles lignes historiques
     * anterieures a cette regle ; toute insertion via le service exige une valeur.
     */
    @Lob
    @Column(name = "url_signature", columnDefinition = "TEXT")
    private String urlSignature;
}
