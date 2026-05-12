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

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRATEUR')")
public class AdminRepresentantEntrepriseController {

    private final AdminCompanyAccountService adminCompanyAccountService;

    @GetMapping("/entreprises")
    public ResponseEntity<List<EntrepriseDto>> getEntreprises() {
        return ResponseEntity.ok(adminCompanyAccountService.getAllEntreprisesForAdmin());
    }

    @PostMapping("/representants-entreprise")
    public ResponseEntity<RepresentantEntrepriseResponse> createRepresentantEntreprise(
            @Valid @RequestBody CreateRepresentantEntrepriseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminCompanyAccountService.createRepresentantEntreprise(request));
    }
}
