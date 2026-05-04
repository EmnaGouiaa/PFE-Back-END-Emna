package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.CahierStageDto;
import fsegs.pfebackendemnagouuiaa.services.CahierStageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cahiers-stage")
@RequiredArgsConstructor
@CrossOrigin("*")
public class CahierStageController {

    private final CahierStageService cahierStageService;

    @PostMapping
    public ResponseEntity<CahierStageDto> create(@RequestBody CahierStageDto dto) {
        return new ResponseEntity<>(cahierStageService.create(dto), HttpStatus.CREATED);
    }

    @PostMapping("/stage/{stageId}")
    public ResponseEntity<CahierStageDto> createByStage(@PathVariable Long stageId,
                                                        @RequestBody CahierStageDto dto) {
        return new ResponseEntity<>(cahierStageService.createByStage(stageId, dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CahierStageDto> update(@PathVariable Long id,
                                                 @RequestBody CahierStageDto dto) {
        return ResponseEntity.ok(cahierStageService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CahierStageDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cahierStageService.getById(id));
    }

    @GetMapping("/stage/{stageId}")
    public ResponseEntity<CahierStageDto> getByStageId(@PathVariable Long stageId) {
        return ResponseEntity.ok(cahierStageService.getByStageId(stageId));
    }

    @GetMapping
    public ResponseEntity<List<CahierStageDto>> getAll() {
        return ResponseEntity.ok(cahierStageService.getAll());
    }

    @PutMapping("/{id}/signer-stagiaire")
    public ResponseEntity<CahierStageDto> signerParStagiaire(@PathVariable Long id) {
        return ResponseEntity.ok(cahierStageService.signerParStagiaire(id));
    }

    @PutMapping("/{id}/signer-encadrant-academique")
    public ResponseEntity<CahierStageDto> signerParEncadrantAcademique(@PathVariable Long id) {
        return ResponseEntity.ok(cahierStageService.signerParEncadrantAcademique(id));
    }

    @PutMapping("/{id}/signer-encadrant-professionnel")
    public ResponseEntity<CahierStageDto> signerParEncadrantProfessionnel(@PathVariable Long id) {
        return ResponseEntity.ok(cahierStageService.signerParEncadrantProfessionnel(id));
    }

    @PutMapping("/{id}/signer-responsable-entreprise")
    public ResponseEntity<CahierStageDto> signerParResponsableEntreprise(@PathVariable Long id) {
        return ResponseEntity.ok(cahierStageService.signerParResponsableEntreprise(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cahierStageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}