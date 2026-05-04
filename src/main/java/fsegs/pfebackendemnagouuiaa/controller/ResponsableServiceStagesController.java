package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.ResponsableServiceStagesRequestDTO;
import fsegs.pfebackendemnagouuiaa.dto.ResponsableServiceStagesResponseDTO;
import fsegs.pfebackendemnagouuiaa.services.ResponsableServiceStagesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/responsables-service-stages")
@RequiredArgsConstructor
public class ResponsableServiceStagesController {

    private final ResponsableServiceStagesService service;

    @PostMapping
    public ResponseEntity<ResponsableServiceStagesResponseDTO> create(
            @RequestBody ResponsableServiceStagesRequestDTO dto) {
        return new ResponseEntity<>(service.create(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponsableServiceStagesResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ResponsableServiceStagesResponseDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponsableServiceStagesResponseDTO> update(
            @PathVariable Long id,
            @RequestBody ResponsableServiceStagesRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok("ResponsableServiceStages supprimé avec succès");
    }
}