package fsegs.pfebackendemnagouuiaa.services.pdf;

import java.time.LocalDateTime;

/** Donnees d'une case signature pour les PDF de stage. */
public record InternshipPdfSignatureSlot(
        String roleLabel,
        String fullName,
        String signatureImageSource,
        boolean signed,
        LocalDateTime signedAt
) {
}
