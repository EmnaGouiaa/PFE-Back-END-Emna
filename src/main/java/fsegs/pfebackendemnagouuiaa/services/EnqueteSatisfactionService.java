package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ConfigurerEnqueteRequest;
import fsegs.pfebackendemnagouuiaa.dto.EnqueteSatisfactionDto;

public interface EnqueteSatisfactionService {

    EnqueteSatisfactionDto getConfiguration();

    EnqueteSatisfactionDto saveConfiguration(ConfigurerEnqueteRequest request);

    /** Inverse l'état actif de l'enquête et retourne la configuration mise à jour. */
    EnqueteSatisfactionDto toggleActive();

    /**
     * Retourne la configuration de l'enquête pour un stage donné.
     * La section s'ouvre automatiquement quand la date du jour >= date de la réunion finale
     * ET que l'enquête est active ET que l'URL est configurée.
     */
    EnqueteSatisfactionDto getForStage(Long stageId);
}
