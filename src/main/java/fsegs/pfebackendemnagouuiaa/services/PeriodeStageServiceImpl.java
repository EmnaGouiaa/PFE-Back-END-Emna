package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.PeriodeStageDto;
import fsegs.pfebackendemnagouuiaa.entities.PeriodeStage;
import fsegs.pfebackendemnagouuiaa.repository.PeriodeStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PeriodeStageServiceImpl implements PeriodeStageService {

    private final PeriodeStageRepository periodeStageRepository;

    /**
     * Vérifie si la date fournie est couverte par au moins une période de stage active.
     *
     * Règle métier :
     *   - Une affectation est autorisée uniquement si {@code today} appartient à la
     *     fenêtre [dateDebut, dateFin] d'au moins une {@link PeriodeStage} active.
     *   - Si aucune période n'est définie dans la base, l'affectation est bloquée.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isDateInActivePeriode(LocalDate date) {
        return !periodeStageRepository.findActivePeriodesCouvrant(date).isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeriodeStageDto> getAll() {
        return periodeStageRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private PeriodeStageDto toDto(PeriodeStage p) {
        return new PeriodeStageDto(p.getId(), p.getLibelle(), p.getDateDebut(), p.getDateFin(), p.isActive());
    }
}
