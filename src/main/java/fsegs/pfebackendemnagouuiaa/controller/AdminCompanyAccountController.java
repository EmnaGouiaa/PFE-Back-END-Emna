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

@RestController
@RequestMapping("/api/admin/company-accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRATEUR')")
public class AdminCompanyAccountController {

    private final AdminCompanyAccountService adminCompanyAccountService;
    private final EncadrantProfessionnelService encadrantProfessionnelService;

    @GetMapping
    public ResponseEntity<List<AdminCompanyAccountResponse>> getAll() {
        return ResponseEntity.ok(adminCompanyAccountService.getAll());
    }

    @PostMapping
    public ResponseEntity<AdminCompanyAccountResponse> create(@Valid @RequestBody AdminCompanyAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCompanyAccountService.create(request));
    }

    @PutMapping("/{entrepriseId}")
    public ResponseEntity<AdminCompanyAccountResponse> update(
            @PathVariable Long entrepriseId,
            @Valid @RequestBody AdminCompanyAccountRequest request
    ) {
        return ResponseEntity.ok(adminCompanyAccountService.update(entrepriseId, request));
    }

    // ─── Encadrants professionnels liés à une entreprise ──────────────────────

    /**
     * Liste tous les encadrants professionnels d'une entreprise.
     */
    @GetMapping("/{entrepriseId}/encadrants")
    public ResponseEntity<List<EncadrantProfessionnelDto>> getEncadrants(@PathVariable Long entrepriseId) {
        return ResponseEntity.ok(encadrantProfessionnelService.getByEntrepriseId(entrepriseId));
    }

    /**
     * Crée un encadrant professionnel rattaché à cette entreprise.
     * L'entrepriseId du corps est forcé par le paramètre de chemin.
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
     * Met à jour un encadrant professionnel appartenant à l'entreprise donnée.
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
