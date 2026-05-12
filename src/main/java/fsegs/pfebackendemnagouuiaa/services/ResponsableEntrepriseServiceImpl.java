package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ResponsableEntrepriseDto;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.mapper.ResponsableEntrepriseMapper;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResponsableEntrepriseServiceImpl implements ResponsableEntrepriseService {

    private final ResponsableEntrepriseRepository responsableEntrepriseRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ResponsableEntrepriseMapper responsableEntrepriseMapper;
    private final ContactUniquenessService contactUniquenessService;
    private final CredentialPolicyService credentialPolicyService;
    private final AccountEmailService accountEmailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ResponsableEntrepriseDto create(ResponsableEntrepriseDto dto) {
        String email = contactUniquenessService.normalizeAndValidateRequiredEmail(dto.getEmail(), "emailResponsable");
        String telephone = contactUniquenessService.normalizeAndValidateOptionalPhone(dto.getTelephone(), "telephoneResponsable");
        contactUniquenessService.validateUserContactForCreate(email, telephone);
        Entreprise entreprise = loadRequiredEntreprise(dto.getEntrepriseId());

        ResponsableEntreprise entity = responsableEntrepriseMapper.toEntity(dto);
        entity.setEmail(email);
        entity.setTelephone(telephone);
        entity.setRole(Role.RESPONSABLE_ENTREPRISE);
        entity.setEntreprise(entreprise);

        String generatedPassword = credentialPolicyService.generateStrongPassword();
        credentialPolicyService.validatePasswordStrength(generatedPassword);
        entity.setMotDePasse(passwordEncoder.encode(generatedPassword));

        ResponsableEntreprise saved = responsableEntrepriseRepository.save(entity);
        accountEmailService.sendAccountCreatedEmail(saved.getPrenom(), saved.getEmail(), generatedPassword);
        return responsableEntrepriseMapper.toDto(saved);
    }

    @Override
    public ResponsableEntrepriseDto update(Long id, ResponsableEntrepriseDto dto) {
        ResponsableEntreprise entity = responsableEntrepriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Responsable entreprise introuvable avec l'id : " + id));

        String email = contactUniquenessService.normalizeAndValidateRequiredEmail(dto.getEmail(), "emailResponsable");
        String telephone = contactUniquenessService.normalizeAndValidateOptionalPhone(dto.getTelephone(), "telephoneResponsable");
        contactUniquenessService.validateUserContactForUpdate(id, email, telephone);
        Entreprise entreprise = loadRequiredEntreprise(dto.getEntrepriseId());

        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(email);
        entity.setTelephone(telephone);
        entity.setPoste(dto.getPoste());
        entity.setService(dto.getService());
        entity.setRole(Role.RESPONSABLE_ENTREPRISE);
        entity.setEntreprise(entreprise);

        ResponsableEntreprise updated = responsableEntrepriseRepository.save(entity);
        return responsableEntrepriseMapper.toDto(updated);
    }

    @Override
    public ResponsableEntrepriseDto getById(Long id) {
        ResponsableEntreprise entity = responsableEntrepriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Responsable entreprise introuvable avec l'id : " + id));

        return responsableEntrepriseMapper.toDto(entity);
    }

    @Override
    public List<ResponsableEntrepriseDto> getAll() {
        return responsableEntrepriseRepository.findAll()
                .stream()
                .map(responsableEntrepriseMapper::toDto)
                .toList();
    }

    @Override
    public List<ResponsableEntrepriseDto> getByEntrepriseId(Long entrepriseId) {
        return responsableEntrepriseRepository.findByEntrepriseId(entrepriseId)
                .stream()
                .map(responsableEntrepriseMapper::toDto)
                .toList();
    }

    @Override
    public void delete(Long id) {
        ResponsableEntreprise entity = responsableEntrepriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Responsable entreprise introuvable avec l'id : " + id));

        responsableEntrepriseRepository.delete(entity);
    }

    private Entreprise loadRequiredEntreprise(Long entrepriseId) {
        if (entrepriseId == null) {
            throw new IllegalArgumentException("Le responsable entreprise doit etre rattache a une entreprise.");
        }

        return entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable avec l'id : " + entrepriseId));
    }
}
