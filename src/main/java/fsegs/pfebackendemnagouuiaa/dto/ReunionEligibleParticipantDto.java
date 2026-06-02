package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Participant pouvant être invité à une réunion de suivi hebdomadaire.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReunionEligibleParticipantDto {

    private Long id;
    private String fullName;
    private String email;
    /** Nom technique du rôle ({@code STAGIAIRE}, {@code ENCADRANT_ACADEMIQUE}, etc.). */
    private String role;
    /** Libellé affiché dans l'interface. */
    private String roleLabel;
}
