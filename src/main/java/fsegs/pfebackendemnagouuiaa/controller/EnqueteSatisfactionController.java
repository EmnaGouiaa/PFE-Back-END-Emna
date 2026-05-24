package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.ConfigurerEnqueteRequest;
import fsegs.pfebackendemnagouuiaa.dto.EnqueteSatisfactionDto;
import fsegs.pfebackendemnagouuiaa.services.EnqueteSatisfactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion de l'enquête de satisfaction.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ RESPONSABLE_STAGE                                                       │
 * │   PUT   /api/enquete           → configurer titre, description, URL     │
 * │   PATCH /api/enquete/toggle    → activer / désactiver                   │
 * │   GET   /api/enquete           → lire la configuration globale          │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │ ACTEURS (stagiaire, encadrants, représentant entreprise)                │
 * │   GET   /api/enquete/stage/{id} → état de l'enquête pour un stage       │
 * │                                   (URL visible seulement si ouverte)    │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * Règle d'ouverture :
 *   sectionOuverte = (today >= dateFin_stage OU today >= date_réunionFinale)
 *                    ET active ET URL configurée
 */
@RestController
@RequestMapping("/api/enquete")
@RequiredArgsConstructor
public class EnqueteSatisfactionController {

    private final EnqueteSatisfactionService enqueteSatisfactionService;

    // ─── Responsable des stages ──────────────────────────────────────────────

    /**
     * Lecture de la configuration globale.
     * Réservé au RESPONSABLE_STAGE et à l'ADMINISTRATEUR.
     */
    @PreAuthorize("hasAnyRole('RESPONSABLE_STAGE', 'ADMINISTRATEUR')")
    @GetMapping
    public ResponseEntity<EnqueteSatisfactionDto> getConfiguration() {
        return ResponseEntity.ok(enqueteSatisfactionService.getConfiguration());
    }

    /**
     * Configurer / modifier l'enquête (titre, description, URL du formulaire externe).
     * URL obligatoire et doit commencer par http:// ou https://.
     * Réservé au RESPONSABLE_STAGE.
     */
    @PreAuthorize("hasRole('RESPONSABLE_STAGE')")
    @PutMapping
    public ResponseEntity<EnqueteSatisfactionDto> saveConfiguration(
            @RequestBody ConfigurerEnqueteRequest request) {
        return ResponseEntity.ok(enqueteSatisfactionService.saveConfiguration(request));
    }

    /**
     * Activer ou désactiver l'enquête.
     * Réservé au RESPONSABLE_STAGE.
     */
    @PreAuthorize("hasRole('RESPONSABLE_STAGE')")
    @PatchMapping("/toggle")
    public ResponseEntity<EnqueteSatisfactionDto> toggleActive() {
        return ResponseEntity.ok(enqueteSatisfactionService.toggleActive());
    }

    // ─── Acteurs — configuration globale lisible ─────────────────────────────

    /**
     * Lecture de la configuration globale visible par tous les acteurs authentifiés.
     * Endpoint dédié acteurs : évite le 403 / 401 que provoquait l'appel à GET /api/enquete
     * (réservé RESPONSABLE_STAGE) depuis les tableaux de bord EP, EA, STAGIAIRE, RE.
     *
     * L'URL du formulaire est incluse dans la réponse ; c'est le client (Angular)
     * qui décide de l'afficher uniquement si {@code active = true}.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/acteur")
    public ResponseEntity<EnqueteSatisfactionDto> getConfigurationActeur() {
        return ResponseEntity.ok(enqueteSatisfactionService.getConfiguration());
    }

    // ─── Acteurs (stagiaire, encadrants, représentant entreprise) ────────────

    /**
     * Retourne l'état de l'enquête pour un stage précis.
     *
     * - Si la condition d'ouverture n'est pas satisfaite : {@code sectionEnqueteOuverte = false},
     *   {@code urlFormulaire = ""} (URL masquée).
     * - Si la condition est satisfaite : {@code sectionEnqueteOuverte = true},
     *   {@code urlFormulaire} contient le lien Google Form / Microsoft Form à ouvrir.
     *
     * Accessible à : STAGIAIRE, ENCADRANT_ACADEMIQUE, ENCADRANT_PROFESSIONNEL,
     *                RESPONSABLE_ENTREPRISE, RESPONSABLE_STAGE, ADMINISTRATEUR.
     */
    @PreAuthorize("hasAnyRole('STAGIAIRE', 'ENCADRANT_ACADEMIQUE', 'ENCADRANT_PROFESSIONNEL',"
                + "'RESPONSABLE_ENTREPRISE', 'RESPONSABLE_STAGE', 'ADMINISTRATEUR')")
    @GetMapping("/stage/{stageId}")
    public ResponseEntity<EnqueteSatisfactionDto> getEnquetePourStage(@PathVariable Long stageId) {
        return ResponseEntity.ok(enqueteSatisfactionService.getForStage(stageId));
    }
}
