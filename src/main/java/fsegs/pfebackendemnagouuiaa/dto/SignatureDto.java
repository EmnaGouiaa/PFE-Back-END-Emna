package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.entities.RoleSignature;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Représentation DTO d'une signature sur un document de stage.
 * Les champs {@code nomSignataire} et {@code urlSignature} sont optionnellement enrichis
 * par le mapper à partir du signataire chargé via {@code signataireId}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignatureDto {

    private Long id;

    private RoleSignature roleSignature;

    /** Identifiant de l'utilisateur signataire. */
    private Long signataireId;

    /** Nom complet du signataire (enrichi par le mapper, peut être null). */
    private String nomSignataire;

    /** URL/nom de fichier de la signature (enrichi par le mapper, peut être null). */
    private String urlSignature;

    private LocalDateTime dateSignature;
}
