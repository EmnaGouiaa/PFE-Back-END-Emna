package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.EntrepriseDto;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import org.springframework.stereotype.Component;

/**
 * Implémentation Spring de {@link EntrepriseMapper}.
 * <p>
 * Conversion symétrique {@link Entreprise} ↔ {@link EntrepriseDto}, utilisée par
 * {@link fsegs.pfebackendemnagouuiaa.services.EntrepriseServiceImpl}.
 */
@Component
public class EntrepriseMapperImpl implements EntrepriseMapper {

    /** {@inheritDoc} */
    @Override
    public EntrepriseDto toDto(Entreprise entreprise) {
        if (entreprise == null) {
            return null;
        }

        return EntrepriseDto.builder()
                .id(entreprise.getId())
                .nom(entreprise.getNom())
                .adresse(entreprise.getAdresse())
                .email(entreprise.getEmail())
                .telephone(entreprise.getTelephone())
                .secteurActivite(entreprise.getSecteurActivite())
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public Entreprise toEntity(EntrepriseDto dto) {
        if (dto == null) {
            return null;
        }

        return Entreprise.builder()
                .id(dto.getId())
                .nom(dto.getNom())
                .adresse(dto.getAdresse())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .secteurActivite(dto.getSecteurActivite())
                .build();
    }
}
