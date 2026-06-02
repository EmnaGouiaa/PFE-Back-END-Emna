package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.entities.Filiere;
import fsegs.pfebackendemnagouuiaa.repository.FiliereRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST de référence pour les filières universitaires.
 * <p>
 * <strong>Domaine exposé :</strong> filières (catalogue académique).
 * <p>
 * <strong>Chemin de base :</strong> {@code /api/filieres}
 * <p>
 * <strong>Sécurité :</strong> aucune annotation {@code @PreAuthorize} au niveau classe ou méthode ;
 * l'accès dépend de la configuration globale Spring Security.
 * <p>
 * <strong>Services injectés :</strong> {@link FiliereRepository} (accès direct JPA, sans couche service).
 */
@RestController
@RequestMapping("/api/filieres")
@RequiredArgsConstructor
public class FiliereController {

    private final FiliereRepository filiereRepository;

    /**
     * Crée une nouvelle filière.
     *
     * @param filiere entité {@link Filiere} à persister
     * @return {@link Filiere} créée (corps de réponse direct, sans enveloppe {@code ResponseEntity})
     */
    @PostMapping
    public Filiere create(@RequestBody Filiere filiere) {
        // Persistance directe via le repository
        return filiereRepository.save(filiere);
    }

    /**
     * Liste toutes les filières enregistrées.
     *
     * @return liste de {@link Filiere}
     */
    @GetMapping
    public List<Filiere> getAll() {
        return filiereRepository.findAll();
    }
}
