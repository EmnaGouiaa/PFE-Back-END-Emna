package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;
import fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee;

/**
 * Contrat de conversion entre l'entité {@link NoteAttribuee} (clé composite) et le DTO
 * {@link NoteAttribueeDto}.
 * <p>
 * Implémenté par {@link NoteAttribueeMapperImpl}. Utilisé par
 * {@link fsegs.pfebackendemnagouuiaa.services.NoteAttribueeServiceImpl} et injecté dans
 * {@link FicheEvaluationMapperImpl} pour charger les notes d'une fiche.
 */
public interface NoteAttribueeMapper {

    /**
     * Transforme une note persistée en DTO, avec champs calculés ({@code evaluee}, {@code scorePondere}).
     *
     * @param entity entité source ; {@code null} renvoie {@code null}
     * @return DTO pour l'API
     */
    NoteAttribueeDto toDto(NoteAttribuee entity);

    /**
     * Construit une entité à partir du DTO ; reconstruit la clé composite si les deux identifiants sont fournis.
     *
     * @param dto DTO entrant ; {@code null} renvoie {@code null}
     * @return entité non persistée
     */
    NoteAttribuee toEntity(NoteAttribueeDto dto);
}
