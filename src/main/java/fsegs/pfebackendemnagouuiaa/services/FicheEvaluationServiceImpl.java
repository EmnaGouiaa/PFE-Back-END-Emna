package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.FicheEvaluationDto;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.FicheEvaluationMapper;
import fsegs.pfebackendemnagouuiaa.repository.FicheEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionFinaleRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FicheEvaluationServiceImpl implements FicheEvaluationService {

    private final FicheEvaluationRepository ficheEvaluationRepository;
    private final StageRepository stageRepository;
    private final ReunionFinaleRepository reunionFinaleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final FicheEvaluationMapper ficheEvaluationMapper;

    @Override
    public FicheEvaluationDto create(FicheEvaluationDto dto) {
        if (dto.getStageId() == null) {
            throw new RuntimeException("Le stage est obligatoire");
        }

        if (dto.getReunionFinaleId() == null) {
            throw new RuntimeException("La réunion finale est obligatoire");
        }

        Stage stage = stageRepository.findById(dto.getStageId())
                .orElseThrow(() -> new RuntimeException("Stage introuvable avec l'id : " + dto.getStageId()));

        ReunionFinale reunionFinale = reunionFinaleRepository.findById(dto.getReunionFinaleId())
                .orElseThrow(() -> new RuntimeException("Réunion finale introuvable avec l'id : " + dto.getReunionFinaleId()));

        if (stage.getStatut() == null || !stage.getStatut().name().equals("TERMINE")) {
            throw new RuntimeException("Accès refusé : le stage n'est pas terminé");
        }

        if (reunionFinale.getStage() == null || !reunionFinale.getStage().getId().equals(stage.getId())) {
            throw new RuntimeException("La réunion finale ne correspond pas à ce stage");
        }

        if (ficheEvaluationRepository.existsByStageId(stage.getId())) {
            throw new RuntimeException("Une fiche d'évaluation existe déjà pour ce stage");
        }

        FicheEvaluation entity = ficheEvaluationMapper.toEntity(dto);
        entity.setStage(stage);
        entity.setReunionFinale(reunionFinale);
        entity.setNoteFinale(0.0);

        FicheEvaluation saved = ficheEvaluationRepository.save(entity);
        return ficheEvaluationMapper.toDto(saved);
    }

    @Override
    public FicheEvaluationDto getById(Long id) {
        FicheEvaluation entity = ficheEvaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("FicheEvaluation introuvable avec l'id : " + id));

        return ficheEvaluationMapper.toDto(entity);
    }

    @Override
    public List<FicheEvaluationDto> getAll() {
        return ficheEvaluationRepository.findAll()
                .stream()
                .map(ficheEvaluationMapper::toDto)
                .toList();
    }

    @Override
    public FicheEvaluationDto getByStageId(Long stageId) {
        FicheEvaluation entity = ficheEvaluationRepository.findFirstByStageId(stageId)
                .orElseThrow(() -> new RuntimeException("Aucune fiche d'évaluation trouvée pour le stage : " + stageId));

        return ficheEvaluationMapper.toDto(entity);
    }

    @Override
    public List<FicheEvaluationDto> getByReunionFinaleId(Long reunionFinaleId) {
        return ficheEvaluationRepository.findByReunionFinaleId(reunionFinaleId)
                .stream()
                .map(ficheEvaluationMapper::toDto)
                .toList();
    }

    @Override
    public FicheEvaluationDto update(Long id, FicheEvaluationDto dto) {
        FicheEvaluation entity = ficheEvaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("FicheEvaluation introuvable avec l'id : " + id));

        if (entity.estVerrouillee()) {
            throw new RuntimeException("Cette fiche est verrouillée car elle est déjà signée par les deux parties");
        }

        entity.setPointFortEncadrantPro(dto.getPointFortEncadrantPro());
        entity.setAxeAmeliorationEncadrantPro(dto.getAxeAmeliorationEncadrantPro());
        entity.setPointFortResponsableEntreprise(dto.getPointFortResponsableEntreprise());
        entity.setAxeAmeliorationResponsableEntreprise(dto.getAxeAmeliorationResponsableEntreprise());

        FicheEvaluation updated = ficheEvaluationRepository.save(entity);
        return ficheEvaluationMapper.toDto(updated);
    }

    @Override
    public FicheEvaluationDto signerFiche(Long ficheId, Long userId) {
        FicheEvaluation fiche = ficheEvaluationRepository.findById(ficheId)
                .orElseThrow(() -> new RuntimeException("FicheEvaluation introuvable avec l'id : " + ficheId));

        if (fiche.estVerrouillee()) {
            throw new RuntimeException("Cette fiche est déjà complètement signée et verrouillée");
        }

        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'id : " + userId));

        if (fiche.getStage() == null) {
            throw new RuntimeException("Aucun stage n'est associé à cette fiche");
        }

        Stage stage = fiche.getStage();

        if (user.getRole() == Role.ENCADRANT_PROFESSIONNEL) {

            if (stage.getEncadrantProfessionnel() == null ||
                    !stage.getEncadrantProfessionnel().getId().equals(user.getId())) {
                throw new RuntimeException("Cet encadrant professionnel n'est pas autorisé à signer cette fiche");
            }

            if (!fiche.partieEncadrantProfessionnelComplete()) {
                throw new RuntimeException("La partie de l'encadrant professionnel est incomplète");
            }

            if (fiche.getSignatureEncadrantProfessionnel() != null &&
                    !fiche.getSignatureEncadrantProfessionnel().isBlank()) {
                throw new RuntimeException("L'encadrant professionnel a déjà signé cette fiche");
            }

            // si le responsable a déjà signé, on impose que toutes les notes soient renseignées
            if (fiche.getSignatureRepresentantEntreprise() != null &&
                    !fiche.getSignatureRepresentantEntreprise().isBlank() &&
                    !fiche.toutesLesNotesSontRenseignees()) {
                throw new RuntimeException("La fiche n'est pas complète : toutes les notes doivent être renseignées avant la validation finale");
            }

            if (user.getNomFichierSignature() == null || user.getNomFichierSignature().isBlank()) {
                throw new RuntimeException("Please add your signature in your profile before signing this document.");
            }

            fiche.setSignatureEncadrantProfessionnel(user.getNomFichierSignature().trim());
            fiche.setDateSignatureEncadrantProfessionnel(LocalDateTime.now());
            fiche.setSignataireEncadrantProfessionnelId(user.getId());
            fiche.setRoleSignatureEncadrantProfessionnel(user.getRole().name());
            fiche.setNomSignataireEncadrantProfessionnel(buildFullName(user));

        } else if (user.getRole() == Role.RESPONSABLE_ENTREPRISE) {

            if (stage.getTuteurEntreprise() == null ||
                    !stage.getTuteurEntreprise().getId().equals(user.getId())) {
                throw new RuntimeException("Ce représentant de l'entreprise n'est pas autorisé à signer cette fiche");
            }

            if (!fiche.partieResponsableEntrepriseComplete()) {
                throw new RuntimeException("La partie du représentant de l'entreprise est incomplète");
            }

            if (fiche.getSignatureRepresentantEntreprise() != null &&
                    !fiche.getSignatureRepresentantEntreprise().isBlank()) {
                throw new RuntimeException("Le représentant de l'entreprise a déjà signé cette fiche");
            }

            // si l'encadrant a déjà signé, on impose que toutes les notes soient renseignées
            if (fiche.getSignatureEncadrantProfessionnel() != null &&
                    !fiche.getSignatureEncadrantProfessionnel().isBlank() &&
                    !fiche.toutesLesNotesSontRenseignees()) {
                throw new RuntimeException("La fiche n'est pas complète : toutes les notes doivent être renseignées avant la validation finale");
            }

            if (user.getNomFichierSignature() == null || user.getNomFichierSignature().isBlank()) {
                throw new RuntimeException("Please add your signature in your profile before signing this document.");
            }

            fiche.setSignatureRepresentantEntreprise(user.getNomFichierSignature().trim());
            fiche.setDateSignatureRepresentantEntreprise(LocalDateTime.now());
            fiche.setSignataireRepresentantEntrepriseId(user.getId());
            fiche.setRoleSignatureRepresentantEntreprise(user.getRole().name());
            fiche.setNomSignataireRepresentantEntreprise(buildFullName(user));

        } else {
            throw new RuntimeException("Seuls l'encadrant professionnel et le représentant de l'entreprise peuvent signer la fiche");
        }

        fiche.setNoteFinale(fiche.calculerNoteFinale());

        FicheEvaluation saved = ficheEvaluationRepository.save(fiche);
        return ficheEvaluationMapper.toDto(saved);
    }

    private String buildFullName(Utilisateur utilisateur) {
        String fullName = ((utilisateur.getPrenom() == null ? "" : utilisateur.getPrenom().trim()) + " "
                + (utilisateur.getNom() == null ? "" : utilisateur.getNom().trim())).trim();
        return fullName.isBlank() ? "Utilisateur" : fullName;
    }

    @Override
    public FicheEvaluationDto remplirPartieEncadrantProfessionnel(Long ficheId, Long userId, FicheEvaluationDto dto) {
        FicheEvaluation fiche = ficheEvaluationRepository.findById(ficheId)
                .orElseThrow(() -> new RuntimeException("FicheEvaluation introuvable avec l'id : " + ficheId));

        if (fiche.estVerrouillee()) {
            throw new RuntimeException("Cette fiche est verrouillée car elle est déjà signée par les deux parties");
        }

        if (fiche.getStage() == null) {
            throw new RuntimeException("Aucun stage n'est associé à cette fiche");
        }

        Stage stage = fiche.getStage();

        if (stage.getEncadrantProfessionnel() == null) {
            throw new RuntimeException("Le stage ne possède pas d'encadrant professionnel");
        }

        if (!stage.getEncadrantProfessionnel().getId().equals(userId)) {
            throw new RuntimeException("Cet encadrant professionnel n'est pas autorisé à remplir cette fiche");
        }

        fiche.setPointFortEncadrantPro(dto.getPointFortEncadrantPro());
        fiche.setAxeAmeliorationEncadrantPro(dto.getAxeAmeliorationEncadrantPro());

        fiche.setNoteFinale(fiche.calculerNoteFinale());

        FicheEvaluation updated = ficheEvaluationRepository.save(fiche);
        return ficheEvaluationMapper.toDto(updated);
    }
    @Override
    public FicheEvaluationDto remplirPartieResponsableEntreprise(Long ficheId, Long userId, FicheEvaluationDto dto) {
        FicheEvaluation fiche = ficheEvaluationRepository.findById(ficheId)
                .orElseThrow(() -> new RuntimeException("FicheEvaluation introuvable avec l'id : " + ficheId));

        if (fiche.estVerrouillee()) {
            throw new RuntimeException("Cette fiche est verrouillée car elle est déjà signée par les deux parties");
        }

        if (fiche.getStage() == null) {
            throw new RuntimeException("Aucun stage n'est associé à cette fiche");
        }

        Stage stage = fiche.getStage();

        if (stage.getTuteurEntreprise() == null) {
            throw new RuntimeException("Le stage ne possède pas de représentant de l'entreprise");
        }

        if (!stage.getTuteurEntreprise().getId().equals(userId)) {
            throw new RuntimeException("Ce représentant de l'entreprise n'est pas autorisé à remplir cette fiche");
        }

        fiche.setPointFortResponsableEntreprise(dto.getPointFortResponsableEntreprise());
        fiche.setAxeAmeliorationResponsableEntreprise(dto.getAxeAmeliorationResponsableEntreprise());

        fiche.setNoteFinale(fiche.calculerNoteFinale());

        FicheEvaluation updated = ficheEvaluationRepository.save(fiche);
        return ficheEvaluationMapper.toDto(updated);
    }
}
