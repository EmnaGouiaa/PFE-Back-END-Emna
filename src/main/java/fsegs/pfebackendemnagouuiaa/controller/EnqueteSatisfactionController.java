package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.EnqueteSatisfactionDto;
import fsegs.pfebackendemnagouuiaa.services.EnqueteSatisfactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enquetes-satisfaction")
@RequiredArgsConstructor
public class EnqueteSatisfactionController {

    private final EnqueteSatisfactionService enqueteSatisfactionService;

    @PreAuthorize("hasAnyRole('RESPONSABLE_UNIVERSITAIRE_STAGES','RESPONSABLE_SERVICE_STAGES','ADMINISTRATEUR')")
    @GetMapping("/configuration")
    public ResponseEntity<EnqueteSatisfactionDto> getConfiguration() {
        return ResponseEntity.ok(enqueteSatisfactionService.getConfiguration());
    }

    @PreAuthorize("hasAnyRole('RESPONSABLE_UNIVERSITAIRE_STAGES','RESPONSABLE_SERVICE_STAGES','ADMINISTRATEUR')")
    @PutMapping("/configuration")
    public ResponseEntity<EnqueteSatisfactionDto> saveConfiguration(@RequestBody EnqueteSatisfactionDto dto) {
        return ResponseEntity.ok(enqueteSatisfactionService.saveConfiguration(dto));
    }

    @PreAuthorize("hasAnyRole('STAGIAIRE','ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE','ENCADRANT_ACADEMIQUE')")
    @GetMapping("/disponible")
    public ResponseEntity<EnqueteSatisfactionDto> getDisponiblePourUtilisateurConnecte() {
        return ResponseEntity.ok(enqueteSatisfactionService.getDisponiblePourUtilisateurConnecte());
    }
}
