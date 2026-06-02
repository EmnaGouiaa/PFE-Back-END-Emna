package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.ReunionDto;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantAcademique;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantProfessionnel;
import fsegs.pfebackendemnagouuiaa.entities.Reunion;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.ReunionHebdomadaire;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.ReunionMapper;
import fsegs.pfebackendemnagouuiaa.service.MeetingInvitationRules;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implémentation Spring de {@link ReunionMapper}.
 * <p>
 * Vue unifiée des réunions pour {@link fsegs.pfebackendemnagouuiaa.services.ReunionServiceImpl}.
 */
@Component
public class ReunionMapperImpl implements ReunionMapper {

    /**
     * {@inheritDoc}
     * <p>
     * Le type « FINALE » / « HEBDOMADAIRE » est inféré par {@code instanceof}, pas par une colonne discriminante seule.
     */
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
            applyStageSummary(dto, entity.getStage());
            applyCreatorFallback(dto, entity, entity.getStage());
        }

        if (entity.getParticipants() != null) {
            Set<Long> participantIds = entity.getParticipants()
                    .stream()
                    .filter(MeetingInvitationRules::isEligibleMeetingParticipant)
                    .map(Utilisateur::getId)
                    .collect(Collectors.toSet());
            dto.setParticipantIds(participantIds);

            Set<String> participantNoms = entity.getParticipants()
                    .stream()
                    .filter(MeetingInvitationRules::isEligibleMeetingParticipant)
                    .map(utilisateur -> ((utilisateur.getPrenom() != null ? utilisateur.getPrenom() : "") + " "
                            + (utilisateur.getNom() != null ? utilisateur.getNom() : "")).trim())
                    .collect(Collectors.toSet());
            dto.setParticipantNoms(participantNoms);
        }

        return dto;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Toujours une instance {@link ReunionHebdomadaire} : la création de réunion finale passe par
     * {@link ReunionFinaleMapperImpl}.
     */
    @Override
    public Reunion toEntity(ReunionDto dto) {
        if (dto == null) {
            return null;
        }

        Reunion entity = new ReunionHebdomadaire();
        entity.setId(dto.getId());
        entity.setNumReunion(dto.getNumReunion());
        entity.setDate(dto.getDate());
        entity.setHeure(dto.getHeure());
        entity.setObservation(dto.getObservation());
        entity.setCompteRendu(dto.getCompteRendu());

        return entity;
    }

    private void applyStageSummary(ReunionDto dto, Stage stage) {
        dto.setStageId(stage.getId());
        dto.setStageTitre(stage.getTitre());
        if (stage.getStagiaire() != null) {
            dto.setStagiaireNom(formatPersonName(stage.getStagiaire()));
        }
        if (stage.getEntreprise() != null) {
            dto.setEntrepriseNom(stage.getEntreprise().getNom());
        }
        if (stage.getTuteurEntreprise() != null) {
            dto.setNomTuteurEntreprise(formatPersonName(stage.getTuteurEntreprise()));
        }
    }

    private void applyCreatorFallback(ReunionDto dto, Reunion entity, Stage stage) {
        if (dto.getNomEncadrantCreateur() != null && !dto.getNomEncadrantCreateur().isBlank()) {
            return;
        }
        String type = entity.getTypeEncadrantCreateur();
        if (type != null && "ACADEMIQUE".equalsIgnoreCase(type) && stage.getEncadrantAcademique() != null) {
            dto.setNomEncadrantCreateur(formatPersonName(stage.getEncadrantAcademique()));
            if (dto.getEncadrantCreateurId() == null) {
                dto.setEncadrantCreateurId(stage.getEncadrantAcademique().getId());
            }
            return;
        }
        if (type != null && "PROFESSIONNEL".equalsIgnoreCase(type) && stage.getEncadrantProfessionnel() != null) {
            dto.setNomEncadrantCreateur(formatPersonName(stage.getEncadrantProfessionnel()));
            if (dto.getEncadrantCreateurId() == null) {
                dto.setEncadrantCreateurId(stage.getEncadrantProfessionnel().getId());
            }
            return;
        }
        if (entity instanceof ReunionFinale && stage.getEncadrantProfessionnel() != null) {
            dto.setNomEncadrantCreateur(formatPersonName(stage.getEncadrantProfessionnel()));
            dto.setTypeEncadrantCreateur("PROFESSIONNEL");
            if (dto.getEncadrantCreateurId() == null) {
                dto.setEncadrantCreateurId(stage.getEncadrantProfessionnel().getId());
            }
        }
    }

    private String formatPersonName(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return "";
        }
        return ((utilisateur.getPrenom() != null ? utilisateur.getPrenom() : "") + " "
                + (utilisateur.getNom() != null ? utilisateur.getNom() : "")).trim();
    }

    private String formatPersonName(EncadrantAcademique encadrant) {
        return encadrant == null ? "" : formatPersonName((Utilisateur) encadrant);
    }

    private String formatPersonName(EncadrantProfessionnel encadrant) {
        return encadrant == null ? "" : formatPersonName((Utilisateur) encadrant);
    }

    private String formatPersonName(ResponsableEntreprise responsable) {
        return responsable == null ? "" : formatPersonName((Utilisateur) responsable);
    }
}
