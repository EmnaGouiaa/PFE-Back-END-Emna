package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.CompanyValidationActionRequest;
import fsegs.pfebackendemnagouuiaa.dto.CompanyValidationItemDto;
import fsegs.pfebackendemnagouuiaa.services.CompanyValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contrôleur REST des validations administratives côté entreprise.
 * <p>
 * <strong>Domaine exposé :</strong> file d'attente des éléments à valider/refuser par le responsable entreprise
 * (stages, documents, etc. selon le type {@code itemType}).
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/entreprise-validations}
 * <p>
 * <strong>Sécurité :</strong> {@code @PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")} au niveau classe ;
 * le service filtre les éléments selon l'entreprise de l'utilisateur connecté.
 * <p>
 * <strong>Services injectés :</strong> {@link CompanyValidationService}
 */
@RestController
@RequestMapping("/api/entreprise-validations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")
public class CompanyValidationController {

    private final CompanyValidationService companyValidationService;

    /**
     * Liste les éléments en attente de validation pour l'entreprise de l'utilisateur connecté.
     *
     * @return {@link ResponseEntity} 200 avec la liste des {@link CompanyValidationItemDto}
     */
    @GetMapping
    public ResponseEntity<List<CompanyValidationItemDto>> getValidationItems() {
        // Le service résout l'entreprise à partir du contexte de sécurité JWT
        return ResponseEntity.ok(companyValidationService.getValidationItemsForAuthenticatedCompany());
    }

    /**
     * Approuve un élément de validation.
     *
     * @param itemType type métier de l'élément (ex. convention, stage)
     * @param itemId   identifiant de l'élément
     * @return {@link ResponseEntity} 200 avec l'élément mis à jour
     * @throws IllegalArgumentException si le type est inconnu
     */
    @PatchMapping("/{itemType}/{itemId}/valider")
    public ResponseEntity<CompanyValidationItemDto> approve(
            @PathVariable String itemType,
            @PathVariable Long itemId
    ) {
        return ResponseEntity.ok(companyValidationService.approve(itemType, itemId));
    }

    /**
     * Refuse un élément de validation avec commentaire optionnel.
     *
     * @param itemType type métier de l'élément
     * @param itemId   identifiant de l'élément
     * @param request  motif de refus (optionnel)
     * @return {@link ResponseEntity} 200 avec l'élément mis à jour
     */
    @PatchMapping("/{itemType}/{itemId}/refuser")
    public ResponseEntity<CompanyValidationItemDto> reject(
            @PathVariable String itemType,
            @PathVariable Long itemId,
            @RequestBody(required = false) CompanyValidationActionRequest request
    ) {
        return ResponseEntity.ok(companyValidationService.reject(
                itemType,
                itemId,
                request != null ? request.getCommentaire() : null
        ));
    }
}
