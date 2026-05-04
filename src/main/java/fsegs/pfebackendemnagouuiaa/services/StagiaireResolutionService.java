package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;

public interface StagiaireResolutionService {
    Stagiaire resolveByEmail(String email);
}
