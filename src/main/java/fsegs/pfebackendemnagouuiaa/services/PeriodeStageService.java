package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.PeriodeStageDto;

import java.time.LocalDate;
import java.util.List;

public interface PeriodeStageService {

    /**
     * Indique si la date fournie est couverte par au moins une période de stage active
     * définie dans le système.
     *
     * @param date date à vérifier (en pratique, la date du jour)
     * @return {@code true} si une période active couvre cette date
     */
    boolean isDateInActivePeriode(LocalDate date);

    /** Retourne toutes les périodes (actives et inactives) pour l'administration. */
    List<PeriodeStageDto> getAll();
}
