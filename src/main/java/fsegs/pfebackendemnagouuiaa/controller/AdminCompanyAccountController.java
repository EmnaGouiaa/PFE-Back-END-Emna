package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.AdminCompanyAccountRequest;
import fsegs.pfebackendemnagouuiaa.dto.AdminCompanyAccountResponse;
import fsegs.pfebackendemnagouuiaa.dto.EncadrantProfessionnelDto;
import fsegs.pfebackendemnagouuiaa.services.AdminCompanyAccountService;
import fsegs.pfebackendemnagouuiaa.services.EncadrantProfessionnelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contrôleur REST d'administration des comptes entreprise et de leurs encadrants professionnels.
 * <p>
 * <strong>Domaine exposé :</strong> provisioning comptes entreprise, gestion des encadrants pro par entreprise.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/admin/company-accounts}
 * <p>
 * <strong>Sécurité :</strong> {@code @PreAuthorize("hasRole('ADMINISTRATEUR')")} au niveau classe.
 * <p>
 * <strong>Services injectés :</strong>
 * <ul>
 *   <li>{@link AdminCompanyAccountService} — comptes entreprise</li>
 *   <li>{@link EncadrantProfessionnelService} — encadrants professionnels</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/company-accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRATEUR')")
public class AdminCompanyAccountController {

    private final AdminCompanyAccountService adminCompanyAccountService;
    private final EncadrantProfessionnelService encadrantProfessionnelService;

    /**
     * Liste tous les comptes entreprise gérés par l'administration.
     *
     * @return {@link ResponseEntity} 200 avec la liste des {@link AdminCompanyAccountResponse}
     */
    @GetMapping
    public ResponseEntity<List<AdminCompanyAccountResponse>> getAll() {
        return ResponseEntity.ok(adminCompanyAccountService.getAll());
    }

    /**
     * Crée un compte entreprise complet (entreprise + responsable).
     *
     * @param request données du compte
     * @return {@link ResponseEntity} 201 avec {@link AdminCompanyAccountResponse}
     */
    @PostMapping
    public ResponseEntity<AdminCompanyAccountResponse> create(@Valid @RequestBody AdminCompanyAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCompanyAccountService.create(request));
    }

    /**
     * Met à jour un compte entreprise existant.
     *
     * @param entrepriseId identifiant de l'entreprise
     * @param request      champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link AdminCompanyAccountResponse}
     */
    @PutMapping("/{entrepriseId}")
    public ResponseEntity<AdminCompanyAccountResponse> update(
            @PathVariable Long entrepriseId,
            @Valid @RequestBody AdminCompanyAccountRequest request
    ) {
        return ResponseEntity.ok(adminCompanyAccountService.update(entrepriseId, request));
    }

    /**
     * Liste les encadrants professionnels rattachés à une entreprise.
     *
     * @param entrepriseId identifiant de l'entreprise
     * @return {@link ResponseEntity} 200 avec la liste des {@link EncadrantProfessionnelDto}
     */
    @GetMapping("/{entrepriseId}/encadrants")
    public ResponseEntity<List<EncadrantProfessionnelDto>> getEncadrants(@PathVariable Long entrepriseId) {
        return ResponseEntity.ok(encadrantProfessionnelService.getByEntrepriseId(entrepriseId));
    }

    /**
     * Crée un encadrant professionnel pour l'entreprise indiquée.
     * <p>Règle : l'{@code entrepriseId} du corps est écrasé par le paramètre de chemin.
     *
     * @param entrepriseId identifiant de l'entreprise
     * @param dto          données de l'encadrant
     * @return {@link ResponseEntity} 201 avec {@link EncadrantProfessionnelDto}
     */
    @PostMapping("/{entrepriseId}/encadrants")
    public ResponseEntity<EncadrantProfessionnelDto> createEncadrant(
            @PathVariable Long entrepriseId,
            @Valid @RequestBody EncadrantProfessionnelDto dto
    ) {
        dto.setEntrepriseId(entrepriseId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(encadrantProfessionnelService.create(dto));
    }

    /**
     * Met à jour un encadrant professionnel d'une entreprise.
     *
     * @param entrepriseId identifiant de l'entreprise
     * @param encadrantId  identifiant de l'encadrant
     * @param dto          champs modifiés
     * @return {@link ResponseEntity} 200 avec {@link EncadrantProfessionnelDto}
     */
    @PutMapping("/{entrepriseId}/encadrants/{encadrantId}")
    public ResponseEntity<EncadrantProfessionnelDto> updateEncadrant(
            @PathVariable Long entrepriseId,
            @PathVariable Long encadrantId,
            @Valid @RequestBody EncadrantProfessionnelDto dto
    ) {
        dto.setEntrepriseId(entrepriseId);
        return ResponseEntity.ok(encadrantProfessionnelService.update(encadrantId, dto));
    }
}
