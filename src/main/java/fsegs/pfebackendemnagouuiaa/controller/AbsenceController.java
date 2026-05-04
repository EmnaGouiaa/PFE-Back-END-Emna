package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.AbsenceDto;
import fsegs.pfebackendemnagouuiaa.services.AbsenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/absences")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AbsenceController {

    private final AbsenceService absenceService;

    @PostMapping
    public ResponseEntity<AbsenceDto> create(@RequestBody AbsenceDto dto) {
        return new ResponseEntity<>(absenceService.create(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AbsenceDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(absenceService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<AbsenceDto>> getAll() {
        return ResponseEntity.ok(absenceService.getAll());
    }

    @GetMapping("/stage/{stageId}")
    public ResponseEntity<List<AbsenceDto>> getByStageId(@PathVariable Long stageId) {
        return ResponseEntity.ok(absenceService.getByStageId(stageId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AbsenceDto> update(@PathVariable Long id,
                                             @RequestBody AbsenceDto dto) {
        return ResponseEntity.ok(absenceService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        absenceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}