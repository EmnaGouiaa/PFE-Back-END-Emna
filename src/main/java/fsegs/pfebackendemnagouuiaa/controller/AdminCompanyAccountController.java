package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.AdminCompanyAccountRequest;
import fsegs.pfebackendemnagouuiaa.dto.AdminCompanyAccountResponse;
import fsegs.pfebackendemnagouuiaa.services.AdminCompanyAccountService;
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
}
