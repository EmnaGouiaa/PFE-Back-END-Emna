package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Signataire concerné par un document de stage (uniquement les rôles autorisés à signer ce document).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSignatoryStatusDto {
    private String role;
    private String libelle;
    private Boolean signe;
    /** Horodatage de la signature lorsque {@code signe} est vrai. */
    private LocalDateTime dateSignature;

    public DocumentSignatoryStatusDto(String role, String libelle, Boolean signe) {
        this(role, libelle, signe, null);
    }
}
