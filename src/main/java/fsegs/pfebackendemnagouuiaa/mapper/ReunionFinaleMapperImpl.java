package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ReunionFinaleDto;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.ReunionFinaleMapper;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ReunionFinaleMapperImpl implements ReunionFinaleMapper {

    @Override
    public ReunionFinaleDto toDto(ReunionFinale entity) {
        if (entity == null) {
            return null;
        }

        ReunionFinaleDto dto = new ReunionFinaleDto();
        dto.setId(entity.getId());
        dto.setTypeReunion("FINALE");
        dto.setNumReunion(entity.getNumReunion());
        dto.setDate(entity.getDate());
        dto.setHeure(entity.getHeure());
        dto.setObservation(entity.getObservation());
        dto.setCompteRendu(entity.getCompteRendu());
        dto.setNote(entity.getNote());
        dto.setUrlFormEvaluation(entity.getUrlFormEvaluation());
        dto.setUrlFormSatisfaction(entity.getUrlFormSatisfaction());
        dto.setTitreEnqueteSatisfaction(entity.getTitreEnqueteSatisfaction());
        dto.setDescriptionEnqueteSatisfaction(entity.getDescriptionEnqueteSatisfaction());

        if (entity.getStage() != null) {
            dto.setStageId(entity.getStage().getId());
            dto.setStageTitre(entity.getStage().getTitre());
            if (entity.getStage().getStagiaire() != null) {
        dto.setStagiaireNom((entity.getStage().getStagiaire().getPrenom() + " " + entity.getStage().getStagiaire().getNom()).trim());
            }
            if (entity.getStage().getEntreprise() != null) {
                dto.setEntrepriseNom(entity.getStage().getEntreprise().getNom());
            }
            if (entity.getNomEncadrantCreateur() == null && entity.getStage().getEncadrantProfessionnel() != null) {
                dto.setNomEncadrantCreateur(
                        (entity.getStage().getEncadrantProfessionnel().getPrenom() + " "
                                + entity.getStage().getEncadrantProfessionnel().getNom()).trim()
                );
                dto.setTypeEncadrantCreateur("PROFESSIONNEL");
                dto.setEncadrantCreateurId(entity.getStage().getEncadrantProfessionnel().getId());
            }
        }

        if (dto.getTypeEncadrantCreateur() == null) {
            dto.setTypeEncadrantCreateur(entity.getTypeEncadrantCreateur());
        }
        if (dto.getNomEncadrantCreateur() == null) {
            dto.setNomEncadrantCreateur(entity.getNomEncadrantCreateur());
        }
        if (dto.getEncadrantCreateurId() == null) {
            dto.setEncadrantCreateurId(entity.getEncadrantCreateurId());
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
    public ReunionFinale toEntity(ReunionFinaleDto dto) {
        if (dto == null) {
            return null;
        }

        ReunionFinale entity = new ReunionFinale();
        entity.setId(dto.getId());
        entity.setNumReunion(dto.getNumReunion());
        entity.setDate(dto.getDate());
        entity.setHeure(dto.getHeure());
        entity.setObservation(dto.getObservation());
        entity.setCompteRendu(dto.getCompteRendu());
        entity.setNote(dto.getNote());
        entity.setUrlFormEvaluation(dto.getUrlFormEvaluation());
        entity.setUrlFormSatisfaction(dto.getUrlFormSatisfaction());
        entity.setTitreEnqueteSatisfaction(dto.getTitreEnqueteSatisfaction());
        entity.setDescriptionEnqueteSatisfaction(dto.getDescriptionEnqueteSatisfaction());

        return entity;
    }
}
