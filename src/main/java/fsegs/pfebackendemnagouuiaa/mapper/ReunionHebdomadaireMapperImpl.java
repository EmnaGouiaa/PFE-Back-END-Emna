package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ReunionHebdomadaireDto;
import fsegs.pfebackendemnagouuiaa.entities.ReunionHebdomadaire;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.ReunionHebdomadaireMapper;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ReunionHebdomadaireMapperImpl implements ReunionHebdomadaireMapper {

    @Override
    public ReunionHebdomadaireDto toDto(ReunionHebdomadaire entity) {
        if (entity == null) {
            return null;
        }

        ReunionHebdomadaireDto dto = new ReunionHebdomadaireDto();
        dto.setId(entity.getId());
        dto.setNumReunion(entity.getNumReunion());
        dto.setDate(entity.getDate());
        dto.setHeure(entity.getHeure());
        dto.setObservation(entity.getObservation());
        dto.setCompteRendu(entity.getCompteRendu());
        dto.setNumSemaine(entity.getNumSemaine());
        dto.setObjectifsSemaineProchaine(entity.getObjectifsSemaineProchaine());

        if (entity.getStage() != null) {
            dto.setStageId(entity.getStage().getId());
            dto.setStageTitre(entity.getStage().getTitre());
        }

        if (entity.getParticipants() != null) {
            Set<Long> participantIds = entity.getParticipants()
                    .stream()
                    .map(Utilisateur::getId)
                    .collect(Collectors.toSet());
            dto.setParticipantIds(participantIds);
        }

        if (entity.getCahierStage() != null) {
            dto.setCahierStageId(entity.getCahierStage().getId());
        }

        return dto;
    }

    @Override
    public ReunionHebdomadaire toEntity(ReunionHebdomadaireDto dto) {
        if (dto == null) {
            return null;
        }

        ReunionHebdomadaire entity = new ReunionHebdomadaire();
        entity.setId(dto.getId());
        entity.setNumReunion(dto.getNumReunion());
        entity.setDate(dto.getDate());
        entity.setHeure(dto.getHeure());
        entity.setObservation(dto.getObservation());
        entity.setCompteRendu(dto.getCompteRendu());
        entity.setNumSemaine(dto.getNumSemaine());
        entity.setObjectifsSemaineProchaine(dto.getObjectifsSemaineProchaine());

        return entity;
    }
}