package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.CreateRepresentantEntrepriseRequest;
import fsegs.pfebackendemnagouuiaa.dto.EntrepriseDto;
import fsegs.pfebackendemnagouuiaa.dto.RepresentantEntrepriseResponse;
import fsegs.pfebackendemnagouuiaa.services.AdminCompanyAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contrôleur REST d'administration pour les entreprises et représentants entreprise.
 * <p>
 * <strong>Domaine exposé :</strong> catalogue entreprises (vue admin), création de représentants.
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/admin}
 * <p>
 * <strong>Sécurité :</strong> {@code @PreAuthorize("hasRole('ADMINISTRATEUR')")} au niveau classe.
 * <p>
 * <strong>Services injectés :</strong> {@link AdminCompanyAccountService}
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRATEUR')")
public class AdminRepresentantEntrepriseController {

    private final AdminCompanyAccountService adminCompanyAccountService;

    /**
     * Liste toutes les entreprises pour le tableau de bord administrateur.
     *
     * @return {@link ResponseEntity} 200 avec la liste des {@link EntrepriseDto}
     */
    @GetMapping("/entreprises")
    public ResponseEntity<List<EntrepriseDto>> getEntreprises() {
        return ResponseEntity.ok(adminCompanyAccountService.getAllEntreprisesForAdmin());
    }

    /**
     * Crée un compte représentant entreprise rattaché à une entreprise existante.
     *
     * @param request données du représentant et lien entreprise
     * @return {@link ResponseEntity} 201 avec {@link RepresentantEntrepriseResponse}
     * @throws jakarta.validation.ConstraintViolationException si la requête est invalide
     */
    @PostMapping("/representants-entreprise")
    public ResponseEntity<RepresentantEntrepriseResponse> createRepresentantEntreprise(
            @Valid @RequestBody CreateRepresentantEntrepriseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminCompanyAccountService.createRepresentantEntreprise(request));
    }
}
