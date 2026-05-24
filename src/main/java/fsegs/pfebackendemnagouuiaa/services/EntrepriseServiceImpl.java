package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.EntrepriseDto;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import fsegs.pfebackendemnagouuiaa.mapper.EntrepriseMapper;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntrepriseServiceImpl implements EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;
    private final EntrepriseMapper entrepriseMapper;
    private final ContactUniquenessService contactUniquenessService;

    /**
     * EntityManager is injected via field injection (not constructor) because JPA requires
     * a transactional proxy wrapper that cannot be provided via constructor injection.
     */
    @PersistenceContext
    private EntityManager entityManager;

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

    /**
     * Deletes an entreprise and all its associated users (responsables + encadrants).
     *
     * Strategy: JPQL bulk-updates mark linked responsables and encadrants as supprimés/inactifs
     * and set their entreprise FK to null BEFORE the JPA cascade delete runs. This avoids:
     *  - Hard-deleting users who may have stage history
     *  - FK constraint violations (stages reference encadrants/tuteurs)
     *  - orphanRemoval conflicts (collections are empty when the entity is deleted)
     */
    @Override
    @Transactional
    public void delete(Long id) {
        if (!entrepriseRepository.existsById(id)) {
            throw new RuntimeException("Entreprise introuvable avec l'id : " + id);
        }

        // 1. Soft-delete and detach linked ResponsableEntreprise via JPQL
        //    (bypasses Hibernate collection tracking / orphanRemoval)
        int responsablesSupprime = entityManager.createQuery(
                        "UPDATE ResponsableEntreprise r "
                        + "SET r.supprime = true, r.actif = false, r.entreprise = null "
                        + "WHERE r.entreprise.id = :entrepriseId")
                .setParameter("entrepriseId", id)
                .executeUpdate();

        // 2. Soft-delete and detach linked EncadrantProfessionnel via JPQL
        int encadrantsSupprime = entityManager.createQuery(
                        "UPDATE EncadrantProfessionnel e "
                        + "SET e.supprime = true, e.actif = false, e.entreprise = null "
                        + "WHERE e.entreprise.id = :entrepriseId")
                .setParameter("entrepriseId", id)
                .executeUpdate();

        log.info("Suppression entreprise id={}: {} responsable(s) et {} encadrant(s) desactives.",
                id, responsablesSupprime, encadrantsSupprime);

        // 3. Flush to DB and evict stale first-level cache
        entityManager.flush();

        // 4. Re-fetch the entreprise from DB — collections are now empty (FK was set to null)
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable avec l'id : " + id));

        // 5. Unlink stage references to avoid FK errors during cascade
        entreprise.getStages().forEach(stage -> {
            stage.setTuteurEntreprise(null);
            stage.setEncadrantProfessionnel(null);
            stage.setOffreSource(null);
        });

        // 6. Delete the entreprise; orphanRemoval won't touch users (collections are empty)
        entrepriseRepository.delete(entreprise);
    }
}
