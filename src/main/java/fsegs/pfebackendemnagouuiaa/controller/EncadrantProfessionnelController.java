package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantProfessionnelDto;
import fsegs.pfebackendemnagouuiaa.services.EncadrantProfessionnelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/encadrants-professionnels")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EncadrantProfessionnelController {

    private final EncadrantProfessionnelService encadrantProfessionnelService;

    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @PostMapping
    public ResponseEntity<EncadrantProfessionnelDto> create(@Valid @RequestBody EncadrantProfessionnelDto dto) {
        EncadrantProfessionnelDto created = encadrantProfessionnelService.create(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_ENTREPRISE')")
    @PostMapping("/responsable/{responsableId}")
    public ResponseEntity<EncadrantProfessionnelDto> createByResponsable(
            @PathVariable Long responsableId,
            @Valid @RequestBody EncadrantProfessionnelDto dto) {
        EncadrantProfessionnelDto created =
                encadrantProfessionnelService.createByResponsableEntreprise(responsableId, dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_ENTREPRISE')")
    @PutMapping("/{id}")
    public ResponseEntity<EncadrantProfessionnelDto> update(@PathVariable Long id,
                                                            @Valid @RequestBody EncadrantProfessionnelDto dto) {
        return ResponseEntity.ok(encadrantProfessionnelService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EncadrantProfessionnelDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(encadrantProfessionnelService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<EncadrantProfessionnelDto>> getAll() {
        return ResponseEntity.ok(encadrantProfessionnelService.getAll());
    }

    @GetMapping("/entreprise/{entrepriseId}")
    public ResponseEntity<List<EncadrantProfessionnelDto>> getByEntrepriseId(@PathVariable Long entrepriseId) {
        return ResponseEntity.ok(encadrantProfessionnelService.getByEntrepriseId(entrepriseId));
    }

    @GetMapping("/by-entreprise/{entrepriseId}")
    public ResponseEntity<List<EncadrantProfessionnelDto>> getByEntrepriseAlias(@PathVariable Long entrepriseId) {
        return ResponseEntity.ok(encadrantProfessionnelService.getByEntrepriseId(entrepriseId));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_ENTREPRISE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        encadrantProfessionnelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
