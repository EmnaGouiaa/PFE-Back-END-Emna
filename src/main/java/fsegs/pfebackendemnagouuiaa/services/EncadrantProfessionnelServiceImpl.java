package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.EncadrantProfessionnelDto;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantProfessionnel;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.mapper.EncadrantProfessionnelMapper;
import fsegs.pfebackendemnagouuiaa.repository.EncadrantProfessionnelRepository;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EncadrantProfessionnelServiceImpl implements EncadrantProfessionnelService {

    private static final String RESPONSABLE_ENTREPRISE_ONLY_CREATE_MESSAGE =
            "Le responsable entreprise peut seulement creer un encadrant professionnel.";

    private final EncadrantProfessionnelRepository encadrantProfessionnelRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ResponsableEntrepriseRepository responsableEntrepriseRepository;
    private final EncadrantProfessionnelMapper encadrantProfessionnelMapper;
    private final PasswordEncoder passwordEncoder;
    private final ContactUniquenessService contactUniquenessService;
    private final CredentialPolicyService credentialPolicyService;
    private final AccountEmailService accountEmailService;
    private final JwtService jwtService;

    @Override
    public EncadrantProfessionnelDto create(EncadrantProfessionnelDto dto) {
        String normalizedEmail = contactUniquenessService.normalizeAndValidateRequiredEmail(dto.getEmail(), "email");
        String normalizedPhone = contactUniquenessService.normalizeAndValidateRequiredPhone(dto.getTelephone(), "telephone");
        contactUniquenessService.validateUserContactForCreate(normalizedEmail, normalizedPhone);
        Entreprise entreprise = loadRequiredEntreprise(dto.getEntrepriseId());

        EncadrantProfessionnel entity = encadrantProfessionnelMapper.toEntity(dto);
        String generatedPassword = generateTemporaryPassword();
        entity.setEmail(normalizedEmail);
        entity.setTelephone(normalizedPhone);
        entity.setMotDePasse(passwordEncoder.encode(generatedPassword));
        entity.setDoitChangerMotDePasse(true);
        entity.setRole(Role.ENCADRANT_PROFESSIONNEL);
        entity.setActif(true);
        entity.setEntreprise(entreprise);

        EncadrantProfessionnel saved = encadrantProfessionnelRepository.save(entity);
        accountEmailService.sendProfessionalSupervisorAccountCreatedEmail(saved.getPrenom(), saved.getEmail(), generatedPassword);
        return encadrantProfessionnelMapper.toDto(saved);
    }

    @Override
    public EncadrantProfessionnelDto update(Long id, EncadrantProfessionnelDto dto) {
        ensureResponsableEntrepriseCanOnlyCreate();

        EncadrantProfessionnel entity = encadrantProfessionnelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Encadrant professionnel introuvable avec l'id : " + id));

        String normalizedEmail = contactUniquenessService.normalizeAndValidateRequiredEmail(dto.getEmail(), "email");
        String normalizedPhone = contactUniquenessService.normalizeAndValidateRequiredPhone(dto.getTelephone(), "telephone");
        contactUniquenessService.validateUserContactForUpdate(entity.getId(), normalizedEmail, normalizedPhone);
        Entreprise entreprise = loadRequiredEntreprise(dto.getEntrepriseId());

        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(normalizedEmail);
        entity.setTelephone(normalizedPhone);
        entity.setPoste(dto.getPoste());
        entity.setService(dto.getService());
        entity.setEntreprise(entreprise);

        EncadrantProfessionnel updated = encadrantProfessionnelRepository.save(entity);
        return encadrantProfessionnelMapper.toDto(updated);
    }

    @Override
    public EncadrantProfessionnelDto getById(Long id) {
        EncadrantProfessionnel entity = encadrantProfessionnelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Encadrant professionnel introuvable avec l'id : " + id));

        return encadrantProfessionnelMapper.toDto(entity);
    }

    @Override
    public List<EncadrantProfessionnelDto> getAll() {
        return encadrantProfessionnelRepository.findAll()
                .stream()
                .map(encadrantProfessionnelMapper::toDto)
                .toList();
    }

    @Override
    public List<EncadrantProfessionnelDto> getByEntrepriseId(Long entrepriseId) {
        return encadrantProfessionnelRepository.findByEntrepriseId(entrepriseId)
                .stream()
                .map(encadrantProfessionnelMapper::toDto)
                .toList();
    }

    @Override
    public EncadrantProfessionnelDto createByResponsableEntreprise(Long responsableId, EncadrantProfessionnelDto dto) {
        ensureAuthenticatedResponsableEntrepriseOwnsRequest(responsableId);

        ResponsableEntreprise responsable = responsableEntrepriseRepository.findById(responsableId)
                .orElseThrow(() -> new EntityNotFoundException("Responsable entreprise introuvable avec l'id : " + responsableId));

        if (responsable.getEntreprise() == null) {
            throw new IllegalStateException("Ce responsable n'est rattache a aucune entreprise.");
        }

        String normalizedEmail = contactUniquenessService.normalizeAndValidateRequiredEmail(dto.getEmail(), "email");
        String normalizedPhone = contactUniquenessService.normalizeAndValidateRequiredPhone(dto.getTelephone(), "telephone");
        contactUniquenessService.validateUserContactForCreate(normalizedEmail, normalizedPhone);

        EncadrantProfessionnel entity = encadrantProfessionnelMapper.toEntity(dto);
        String generatedPassword = generateTemporaryPassword();
        entity.setEmail(normalizedEmail);
        entity.setTelephone(normalizedPhone);
        entity.setEntreprise(responsable.getEntreprise());
        entity.setMotDePasse(passwordEncoder.encode(generatedPassword));
        entity.setDoitChangerMotDePasse(true);
        entity.setRole(Role.ENCADRANT_PROFESSIONNEL);
        entity.setActif(true);

        EncadrantProfessionnel saved = encadrantProfessionnelRepository.save(entity);
        accountEmailService.sendProfessionalSupervisorAccountCreatedEmail(saved.getPrenom(), saved.getEmail(), generatedPassword);
        return encadrantProfessionnelMapper.toDto(saved);
    }

    @Override
    public void delete(Long id) {
        ensureResponsableEntrepriseCanOnlyCreate();

        EncadrantProfessionnel entity = encadrantProfessionnelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Encadrant professionnel introuvable avec l'id : " + id));

        encadrantProfessionnelRepository.delete(entity);
    }

    private void ensureResponsableEntrepriseCanOnlyCreate() {
        jwtService.getAuthenticatedUtilisateur().ifPresent(utilisateur -> {
            if (utilisateur.getRole() == Role.RESPONSABLE_ENTREPRISE) {
                throw new AccessDeniedException(RESPONSABLE_ENTREPRISE_ONLY_CREATE_MESSAGE);
            }
        });
    }

    private void ensureAuthenticatedResponsableEntrepriseOwnsRequest(Long responsableId) {
        jwtService.getAuthenticatedUtilisateur().ifPresent(utilisateur -> {
            if (utilisateur.getRole() != Role.RESPONSABLE_ENTREPRISE) {
                return;
            }

            if (utilisateur.getId() == null || !utilisateur.getId().equals(responsableId)) {
                throw new AccessDeniedException(RESPONSABLE_ENTREPRISE_ONLY_CREATE_MESSAGE);
            }
        });
    }

    private Entreprise loadEntreprise(Long entrepriseId) {
        return entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new EntityNotFoundException("Entreprise introuvable avec l'id : " + entrepriseId));
    }

    private Entreprise loadRequiredEntreprise(Long entrepriseId) {
        if (entrepriseId == null) {
            throw new IllegalArgumentException("L'encadrant professionnel doit etre rattache a une entreprise.");
        }

        return loadEntreprise(entrepriseId);
    }

    private String generateTemporaryPassword() {
        String generatedPassword = credentialPolicyService.generateStrongPassword();
        credentialPolicyService.validatePasswordStrength(generatedPassword);
        return generatedPassword;
    }
}
