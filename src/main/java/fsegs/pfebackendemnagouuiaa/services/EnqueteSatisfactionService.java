package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.EnqueteSatisfactionDto;

public interface EnqueteSatisfactionService {

    EnqueteSatisfactionDto getConfiguration();

    EnqueteSatisfactionDto saveConfiguration(EnqueteSatisfactionDto dto);

    EnqueteSatisfactionDto getDisponiblePourUtilisateurConnecte();
}
