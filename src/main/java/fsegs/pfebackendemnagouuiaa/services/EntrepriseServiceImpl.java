package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.EntrepriseDto;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import fsegs.pfebackendemnagouuiaa.mapper.EntrepriseMapper;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EntrepriseServiceImpl implements EntrepriseService {
    private final EntrepriseRepository entrepriseRepository;
    private final EntrepriseMapper entrepriseMapper;
    private final ContactUniquenessService contactUniquenessService;

    @Override
    public EntrepriseDto create(EntrepriseDto dto) {
        String email = contactUniquenessService.normalizeAndValidateOptionalEmail(dto.getEmail(), "emailEntreprise");
        String telephone = contactUniquenessService.normalizeAndValidateOptionalPhone(dto.getTelephone(), "telephoneEntreprise");
        contactUniquenessService.validateEntrepriseContactForCreate(email, telephone);

        Entreprise entreprise = entrepriseMapper.toEntity(dto);
        entreprise.setEmail(email);
        entreprise.setTelephone(telephone);

        Entreprise saved = entrepriseRepository.save(entreprise);
        return entrepriseMapper.toDto(saved);
    }

    @Override
    public EntrepriseDto update(Long id, EntrepriseDto dto) {
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable avec l'id : " + id));

        String email = contactUniquenessService.normalizeAndValidateOptionalEmail(dto.getEmail(), "emailEntreprise");
        String telephone = contactUniquenessService.normalizeAndValidateOptionalPhone(dto.getTelephone(), "telephoneEntreprise");
        contactUniquenessService.validateEntrepriseContactForUpdate(id, email, telephone);

        entreprise.setNom(dto.getNom());
        entreprise.setAdresse(dto.getAdresse());
        entreprise.setEmail(email);
        entreprise.setTelephone(telephone);
        entreprise.setSecteurActivite(dto.getSecteurActivite());

        Entreprise updated = entrepriseRepository.save(entreprise);
        return entrepriseMapper.toDto(updated);
    }

    @Override
    public EntrepriseDto getById(Long id) {
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable avec l'id : " + id));

        return entrepriseMapper.toDto(entreprise);
    }

    @Override
    public List<EntrepriseDto> getAll() {
        return entrepriseRepository.findAll()
                .stream()
                .map(entrepriseMapper::toDto)
                .toList();
    }

    @Override
    public void delete(Long id) {
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable avec l'id : " + id));

        entrepriseRepository.delete(entreprise);
    }
}
