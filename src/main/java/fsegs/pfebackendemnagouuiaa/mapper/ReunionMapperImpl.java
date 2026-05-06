package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ReunionDto;
import fsegs.pfebackendemnagouuiaa.entities.Reunion;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.ReunionMapper;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ReunionMapperImpl implements ReunionMapper {

    @Override
    public ReunionDto toDto(Reunion entity) {
        if (entity == null) {
            return null;
        }

        ReunionDto dto = new ReunionDto();
        dto.setId(entity.getId());
        dto.setNumReunion(entity.getNumReunion());
        dto.setDate(entity.getDate());
        dto.setHeure(entity.getHeure());
        dto.setObservation(entity.getObservation());
        dto.setCompteRendu(entity.getCompteRendu());
        dto.setTypeReunion(entity instanceof ReunionFinale ? "FINALE" : "HEBDOMADAIRE");
        dto.setTypeEncadrantCreateur(entity.getTypeEncadrantCreateur());
        dto.setNomEncadrantCreateur(entity.getNomEncadrantCreateur());
        dto.setEncadrantCreateurId(entity.getEncadrantCreateurId());

        if (entity.getStage() != null) {
            dto.setStageId(entity.getStage().getId());
            dto.setStageTitre(entity.getStage().getTitre());
            if (entity.getStage().getStagiaire() != null) {
                dto.setStagiaireNom((entity.getStage().getStagiaire().getPrenom() + " " + entity.getStage().getStagiaire().getNom()).trim());
            }
            if (entity.getStage().getEntreprise() != null) {
                dto.setEntrepriseNom(entity.getStage().getEntreprise().getNom());
            }
        }

        if (entity.getParticipants() != null) {
            Set<Long> participantIds = entity.getParticipants()
                    .stream()
                    .map(Utilisateur::getId)
                    .collect(Collectors.toSet());
            dto.setParticipantIds(participantIds);
        }

        return dto;
    }

    @Override
    public Reunion toEntity(ReunionDto dto) {
        if (dto == null) {
            return null;
        }

        Reunion entity = new Reunion();
        entity.setId(dto.getId());
        entity.setNumReunion(dto.getNumReunion());
        entity.setDate(dto.getDate());
        entity.setHeure(dto.getHeure());
        entity.setObservation(dto.getObservation());
        entity.setCompteRendu(dto.getCompteRendu());

        return entity;
    }
}
