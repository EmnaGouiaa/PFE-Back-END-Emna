package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.CreateStageRequest;
import fsegs.pfebackendemnagouuiaa.entities.Stage;

/**
 * Mapper utilitaire statique : {@link CreateStageRequest} → {@link Stage}.
 * <p>
 * Utilisé par {@link fsegs.pfebackendemnagouuiaa.services.StageServiceImpl} à la création d'un stage.
 * Les associations (stagiaire, entreprise, encadrants) sont résolues dans le service, pas ici.
 */
public class StageMapper {

    private StageMapper() {
    }

    /**
     * Initialise une entité {@link Stage} avec les champs métier de la requête de création.
     *
     * @param request requête API ; ne doit pas être {@code null} (appelant responsable)
     * @return entité non persistée, sans relations JPA
     */
    public static Stage toEntity(CreateStageRequest request) {
        Stage stage = new Stage();

        stage.setTitre(request.getTitre());
        stage.setSujet(request.getSujet());
        stage.setDateDebut(request.getDateDebut());
        stage.setDateFin(request.getDateFin());
        stage.setDuree(request.getDuree());
        stage.setNbSemaine(request.getNbSemaine());
        stage.setNiveauSouhaite(request.getNiveauSouhaite());

        return stage;
    }
}
