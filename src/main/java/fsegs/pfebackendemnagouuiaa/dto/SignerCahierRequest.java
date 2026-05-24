package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Data;

/**
 * Payload optionnel pour la signature d'un cahier de stage.
 *
 * <p>Le champ {@code signatureImage} est facultatif :</p>
 * <ul>
 *   <li>S'il est fourni (data URL base64 ou URL HTTP), il est utilise tel quel.</li>
 *   <li>S'il est absent ou vide, le backend utilise automatiquement l'image de
 *       signature stockee sur le profil de l'utilisateur authentifie.</li>
 *   <li>Si aucune source n'est disponible, l'endpoint renvoie 400 avec un message
 *       explicite invitant a renseigner sa signature de profil.</li>
 * </ul>
 *
 * Ce comportement evite a l'utilisateur de devoir uploader son image a chaque
 * signature : il configure sa signature une fois dans son profil, puis le systeme
 * la reutilise automatiquement.
 */
@Data
public class SignerCahierRequest {

    /** Image de signature optionnelle (data URL base64 ou URL). Null = fallback profil. */
    private String signatureImage;
}
