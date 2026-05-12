package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.EnqueteSatisfactionResponse;
import fsegs.pfebackendemnagouuiaa.dto.RemplirEnqueteSatisfactionRequest;
import fsegs.pfebackendemnagouuiaa.services.EnqueteSatisfactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/enquetes-satisfaction")
@RequiredArgsConstructor
public class EnqueteSatisfactionController {

    private final EnqueteSatisfactionService enqueteSatisfactionService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/stage/{stageId}")
    public ResponseEntity<List<EnqueteSatisfactionResponse>> getByStage(@PathVariable Long stageId) {
        return ResponseEntity.ok(enqueteSatisfactionService.getEnquetesByStage(stageId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/utilisateur/{utilisateurId}")
    public ResponseEntity<List<EnqueteSatisfactionResponse>> getByUtilisateur(@PathVariable Long utilisateurId) {
        return ResponseEntity.ok(enqueteSatisfactionService.getEnquetesByUtilisateur(utilisateurId));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}/remplir")
    public ResponseEntity<EnqueteSatisfactionResponse> remplir(@PathVariable Long id,
                                                               @RequestBody RemplirEnqueteSatisfactionRequest request) {
        return ResponseEntity.ok(enqueteSatisfactionService.remplirEnquete(id, request));
    }
}
