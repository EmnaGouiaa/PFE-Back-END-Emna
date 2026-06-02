package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.CritereEvaluationDto;
import fsegs.pfebackendemnagouuiaa.entities.CritereEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import org.springframework.stereotype.Component;

/**
 * Implémentation Spring de {@link CritereEvaluationMapper}.
 * <p>
 * Conversion {@link CritereEvaluation} ↔ {@link CritereEvaluationDto}, utilisée par
 * {@link fsegs.pfebackendemnagouuiaa.services.CritereEvaluationServiceImpl}.
 */
@Component
public class CritereEvaluationMapperImpl implements CritereEvaluationMapper {

    /**
     * {@inheritDoc}
     * <p>
     * Le champ DTO {@code commentaireGeneral} correspond à la consigne métier {@code consigne} en base.
     */
    @Override
    public CritereEvaluationDto toDto(CritereEvaluation entity) {
        if (entity == null) {
            return null;
        }

        CritereEvaluationDto dto = new CritereEvaluationDto();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setDescription(entity.getDescription());
        dto.setCategorie(entity.getCategorie());
        dto.setBareme(entity.getBareme());
        // Alias API : commentaireGeneral ↔ consigne (entité)
        dto.setCommentaireGeneral(entity.getConsigne());
        dto.setPartie(entity.getPartie());

        if (entity.getFiche() != null) {
            dto.setFicheId(entity.getFiche().getId());
        }

        return dto;
    }

    /**
     * {@inheritDoc}
     * <p>
     * La fiche parente est matérialisée par une référence légère (identifiant seul) pour la persistance JPA.
     */
    @Override
    public CritereEvaluation toEntity(CritereEvaluationDto dto) {
        if (dto == null) {
            return null;
        }

        CritereEvaluation entity = new CritereEvaluation();
        entity.setId(dto.getId());
        entity.setLibelle(dto.getLibelle());
        entity.setDescription(dto.getDescription());
        entity.setCategorie(dto.getCategorie());
        entity.setBareme(dto.getBareme());
        entity.setConsigne(dto.getCommentaireGeneral());
        entity.setPartie(dto.getPartie());

        if (dto.getFicheId() != null) {
            FicheEvaluation fiche = new FicheEvaluation();
            fiche.setId(dto.getFicheId());
            entity.setFiche(fiche);
        }

        return entity;
    }
}
