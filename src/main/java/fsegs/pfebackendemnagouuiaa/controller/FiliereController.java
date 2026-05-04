package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.entities.Filiere;
import fsegs.pfebackendemnagouuiaa.repository.FiliereRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/filieres")
@RequiredArgsConstructor
public class FiliereController {

    private final FiliereRepository filiereRepository;

    @PostMapping
    public Filiere create(@RequestBody Filiere filiere) {
        return filiereRepository.save(filiere);
    }

    @GetMapping
    public List<Filiere> getAll() {
        return filiereRepository.findAll();
    }
}