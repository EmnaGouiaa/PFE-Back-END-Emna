package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;

public interface StagiaireResolutionService {
    Stagiaire findByEmail(String email);
}
