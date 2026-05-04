package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.CreateStageRequest;
import fsegs.pfebackendemnagouuiaa.entities.Stage;

public class StageMapper {
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