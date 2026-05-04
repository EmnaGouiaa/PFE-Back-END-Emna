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

@RestController
@RequestMapping("/api/entreprise-validations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESPONSABLE_ENTREPRISE')")
public class CompanyValidationController {

    private final CompanyValidationService companyValidationService;

    @GetMapping
    public ResponseEntity<List<CompanyValidationItemDto>> getValidationItems() {
        return ResponseEntity.ok(companyValidationService.getValidationItemsForAuthenticatedCompany());
    }

    @PatchMapping("/{itemType}/{itemId}/valider")
    public ResponseEntity<CompanyValidationItemDto> approve(
            @PathVariable String itemType,
            @PathVariable Long itemId
    ) {
        return ResponseEntity.ok(companyValidationService.approve(itemType, itemId));
    }

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
