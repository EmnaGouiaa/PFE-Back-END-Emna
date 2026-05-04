package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantProfessionnelDto;
import fsegs.pfebackendemnagouuiaa.services.EncadrantProfessionnelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/encadrants-professionnels")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EncadrantProfessionnelController {

    private final EncadrantProfessionnelService encadrantProfessionnelService;

    @PostMapping
    public ResponseEntity<EncadrantProfessionnelDto> create(@Valid @RequestBody EncadrantProfessionnelDto dto) {
        EncadrantProfessionnelDto created = encadrantProfessionnelService.create(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/responsable/{responsableId}")
    public ResponseEntity<EncadrantProfessionnelDto> createByResponsable(
            @PathVariable Long responsableId,
            @Valid @RequestBody EncadrantProfessionnelDto dto) {
        EncadrantProfessionnelDto created =
                encadrantProfessionnelService.createByResponsableEntreprise(responsableId, dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        encadrantProfessionnelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
