package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.FicheEvaluationDto;
import fsegs.pfebackendemnagouuiaa.dto.SignatureDto;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.RoleSignature;
import fsegs.pfebackendemnagouuiaa.entities.Signature;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.NoteAttribueeRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FicheEvaluationMapperImpl implements FicheEvaluationMapper {

    private final NoteAttribueeRepository noteAttribueeRepository;
    private final NoteAttribueeMapper noteAttribueeMapper;
    private final UtilisateurRepository utilisateurRepository;

    @Override
    public FicheEvaluationDto toDto(FicheEvaluation entity) {
        if (entity == null) return null;

        FicheEvaluationDto dto = new FicheEvaluationDto();
        dto.setId(entity.getId());

        dto.setPointFortEncadrantPro(entity.getPointFortEncadrantPro());
        dto.setAxeAmeliorationEncadrantPro(entity.getAxeAmeliorationEncadrantPro());

        // Enriched EP signature fields — backward-compatible
        entity.getSignaturePour(RoleSignature.ENCADRANT_PROFESSIONNEL).ifPresent(sig -> {
            dto.setDateSignatureEncadrantProfessionnel(sig.getDateSignature());
            dto.setSignataireEncadrantProfessionnelId(sig.getSignataireId());
            dto.setRoleSignatureEncadrantProfessionnel(RoleSignature.ENCADRANT_PROFESSIONNEL.name());
            if (sig.getSignataireId() != null) {
                utilisateurRepository.findById(sig.getSignataireId()).ifPresent(u -> {
                    dto.setSignatureEncadrantProfessionnel(u.getUrlSignature());
                    dto.setNomSignataireEncadrantProfessionnel(buildFullName(u));
                });
            }
        });

        dto.setPointFortResponsableEntreprise(entity.getPointFortResponsableEntreprise());
        dto.setAxeAmeliorationResponsableEntreprise(entity.getAxeAmeliorationResponsableEntreprise());

        // Enriched RE signature fields — backward-compatible
        entity.getSignaturePour(RoleSignature.RESPONSABLE_ENTREPRISE).ifPresent(sig -> {
            dto.setDateSignatureRepresentantEntreprise(sig.getDateSignature());
            dto.setSignataireRepresentantEntrepriseId(sig.getSignataireId());
            dto.setRoleSignatureRepresentantEntreprise(RoleSignature.RESPONSABLE_ENTREPRISE.name());
            if (sig.getSignataireId() != null) {
                utilisateurRepository.findById(sig.getSignataireId()).ifPresent(u -> {
                    dto.setSignatureRepresentantEntreprise(u.getUrlSignature());
                    dto.setNomSignataireRepresentantEntreprise(buildFullName(u));
                });
            }
        });

        dto.setNoteFinale(entity.getNoteFinale());

        // Full signatures list
        dto.setSignatures(mapSignatures(entity.getSignatures()));

        // Computed status flags
        dto.setDonneesCompletes(entity.donneesCompletes());
        dto.setComplete(entity.donneesCompletes());
        dto.setSignaturesCompletes(entity.estCompletementSigne());
        dto.setVerrouillee(entity.estVerrouillee());

        if (entity.getStage() != null) {
            Stage stage = entity.getStage();
            dto.setStageId(stage.getId());
            dto.setStageTitre(stage.getTitre());
            dto.setStageSujet(stage.getSujet());
            dto.setStageDateDebut(stage.getDateDebut());
            dto.setStageDateFin(stage.getDateFin());
            dto.setStagiaireNomComplet(stage.getStagiaire() == null
                    ? ""
                    : ((stage.getStagiaire().getPrenom() == null ? "" : stage.getStagiaire().getPrenom().trim()) + " "
                    + (stage.getStagiaire().getNom() == null ? "" : stage.getStagiaire().getNom().trim())).trim());
            dto.setSectionStagiaire(stage.getStagiaire() == null || stage.getStagiaire().getFiliere() == null
                    ? ""
                    : stage.getStagiaire().getFiliere().getNom());
            dto.setEntrepriseNom(stage.getEntreprise() == null ? "" : stage.getEntreprise().getNom());
            dto.setEntrepriseLieuStage(stage.getEntreprise() == null ? "" : stage.getEntreprise().getAdresse());
        }

        if (entity.getReunionFinale() != null) {
            dto.setReunionFinaleId(entity.getReunionFinale().getId());
            dto.setReunionFinaleNumero(entity.getReunionFinale().getNumReunion());
            dto.setReunionFinaleDate(entity.getReunionFinale().getDate());
            dto.setReunionFinaleHeure(entity.getReunionFinale().getHeure());
        }

        dto.setNotesAttribuees(loadNotes(entity.getId()));

        return dto;
    }

    @Override
    public FicheEvaluation toEntity(FicheEvaluationDto dto) {
        if (dto == null) return null;

        FicheEvaluation entity = new FicheEvaluation();
        entity.setId(dto.getId());

        entity.setPointFortEncadrantPro(dto.getPointFortEncadrantPro());
        entity.setAxeAmeliorationEncadrantPro(dto.getAxeAmeliorationEncadrantPro());
        entity.setPointFortResponsableEntreprise(dto.getPointFortResponsableEntreprise());
        entity.setAxeAmeliorationResponsableEntreprise(dto.getAxeAmeliorationResponsableEntreprise());
        entity.setNoteFinale(dto.getNoteFinale());

        if (dto.getStageId() != null) {
            Stage stage = new Stage();
            stage.setId(dto.getStageId());
            entity.setStage(stage);
        }

        if (dto.getReunionFinaleId() != null) {
            ReunionFinale reunionFinale = new ReunionFinale();
            reunionFinale.setId(dto.getReunionFinaleId());
            entity.setReunionFinale(reunionFinale);
        }

        return entity;
    }

    private List<SignatureDto> mapSignatures(List<Signature> signatures) {
        if (signatures == null) return List.of();
        return signatures.stream().map(this::toSignatureDto).toList();
    }

    private SignatureDto toSignatureDto(Signature sig) {
        SignatureDto dto = new SignatureDto();
        dto.setId(sig.getId());
        dto.setRoleSignature(sig.getRoleSignature());
        dto.setSignataireId(sig.getSignataireId());
        dto.setDateSignature(sig.getDateSignature());

        if (sig.getSignataireId() != null) {
            utilisateurRepository.findById(sig.getSignataireId()).ifPresent(u -> {
                dto.setNomSignataire(buildFullName(u));
                dto.setUrlSignature(u.getUrlSignature());
            });
        }

        return dto;
    }

    private List<fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto> loadNotes(Long ficheId) {
        if (ficheId == null) return List.of();
        return noteAttribueeRepository.findByFicheEvaluationId(ficheId).stream()
                .sorted(Comparator.comparing(note -> note.getCritereEvaluation() == null
                        ? ""
                        : String.valueOf(note.getCritereEvaluation().getLibelle()), String.CASE_INSENSITIVE_ORDER))
                .map(noteAttribueeMapper::toDto)
                .toList();
    }

    private String buildFullName(Utilisateur u) {
        String full = ((u.getPrenom() == null ? "" : u.getPrenom().trim()) + " "
                + (u.getNom() == null ? "" : u.getNom().trim())).trim();
        return full.isBlank() ? "Utilisateur" : full;
    }
}
