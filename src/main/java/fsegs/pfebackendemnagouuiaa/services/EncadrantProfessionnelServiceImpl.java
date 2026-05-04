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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EncadrantProfessionnelServiceImpl implements EncadrantProfessionnelService {

    private final EncadrantProfessionnelRepository encadrantProfessionnelRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ResponsableEntrepriseRepository responsableEntrepriseRepository;
    private final EncadrantProfessionnelMapper encadrantProfessionnelMapper;
    private final PasswordEncoder passwordEncoder;
    private final ContactUniquenessService contactUniquenessService;
    private final CredentialPolicyService credentialPolicyService;
    private final AccountEmailService accountEmailService;

    @Override
    public EncadrantProfessionnelDto create(EncadrantProfessionnelDto dto) {
        String normalizedEmail = contactUniquenessService.normalizeAndValidateRequiredEmail(dto.getEmail(), "email");
        String normalizedPhone = contactUniquenessService.normalizeAndValidateRequiredPhone(dto.getTelephone(), "telephone");
        contactUniquenessService.validateUserContactForCreate(normalizedEmail, normalizedPhone);

        EncadrantProfessionnel entity = encadrantProfessionnelMapper.toEntity(dto);
        String generatedPassword = generateTemporaryPassword();
        entity.setEmail(normalizedEmail);
        entity.setTelephone(normalizedPhone);
        entity.setMotDePasse(passwordEncoder.encode(generatedPassword));
        entity.setRole(Role.ENCADRANT_PROFESSIONNEL);
        entity.setActif(true);

        if (dto.getEntrepriseId() != null) {
            entity.setEntreprise(loadEntreprise(dto.getEntrepriseId()));
        }

        EncadrantProfessionnel saved = encadrantProfessionnelRepository.save(entity);
        accountEmailService.sendProfessionalSupervisorAccountCreatedEmail(dto.getPrenom(), saved.getEmail(), generatedPassword);
        return encadrantProfessionnelMapper.toDto(saved);
    }

    @Override
    public EncadrantProfessionnelDto update(Long id, EncadrantProfessionnelDto dto) {
        EncadrantProfessionnel entity = encadrantProfessionnelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Encadrant professionnel introuvable avec l'id : " + id));

        String normalizedEmail = contactUniquenessService.normalizeAndValidateRequiredEmail(dto.getEmail(), "email");
        String normalizedPhone = contactUniquenessService.normalizeAndValidateRequiredPhone(dto.getTelephone(), "telephone");
        contactUniquenessService.validateUserContactForUpdate(entity.getId(), normalizedEmail, normalizedPhone);

        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(normalizedEmail);
        entity.setTelephone(normalizedPhone);
        entity.setPoste(dto.getPoste());
        entity.setService(dto.getService());

        if (dto.getEntrepriseId() != null) {
            entity.setEntreprise(loadEntreprise(dto.getEntrepriseId()));
        } else {
            entity.setEntreprise(null);
        }

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
        entity.setRole(Role.ENCADRANT_PROFESSIONNEL);
        entity.setActif(true);

        EncadrantProfessionnel saved = encadrantProfessionnelRepository.save(entity);
        accountEmailService.sendProfessionalSupervisorAccountCreatedEmail(dto.getPrenom(), saved.getEmail(), generatedPassword);
        return encadrantProfessionnelMapper.toDto(saved);
    }

    @Override
    public void delete(Long id) {
        EncadrantProfessionnel entity = encadrantProfessionnelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Encadrant professionnel introuvable avec l'id : " + id));

        encadrantProfessionnelRepository.delete(entity);
    }

    private Entreprise loadEntreprise(Long entrepriseId) {
        return entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new EntityNotFoundException("Entreprise introuvable avec l'id : " + entrepriseId));
    }

    private String generateTemporaryPassword() {
        String generatedPassword = credentialPolicyService.generateStrongPassword();
        credentialPolicyService.validatePasswordStrength(generatedPassword);
        return generatedPassword;
    }
}
