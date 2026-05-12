package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.ResponsableEntrepriseDto;
import fsegs.pfebackendemnagouuiaa.services.ResponsableEntrepriseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/responsables-entreprise")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ResponsableEntrepriseController {

    private final ResponsableEntrepriseService responsableEntrepriseService;

    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @PostMapping
    public ResponseEntity<ResponsableEntrepriseDto> create(@Valid @RequestBody ResponsableEntrepriseDto dto) {
        ResponsableEntrepriseDto created = responsableEntrepriseService.create(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @PutMapping("/{id}")
    public ResponseEntity<ResponsableEntrepriseDto> update(@PathVariable Long id,
                                                           @Valid @RequestBody ResponsableEntrepriseDto dto) {
        return ResponseEntity.ok(responsableEntrepriseService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponsableEntrepriseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(responsableEntrepriseService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ResponsableEntrepriseDto>> getAll() {
        return ResponseEntity.ok(responsableEntrepriseService.getAll());
    }

    @GetMapping("/entreprise/{entrepriseId}")
    public ResponseEntity<List<ResponsableEntrepriseDto>> getByEntrepriseId(@PathVariable Long entrepriseId) {
        return ResponseEntity.ok(responsableEntrepriseService.getByEntrepriseId(entrepriseId));
    }

    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        responsableEntrepriseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
