package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CreateEnqueteSatisfactionRequest;
import fsegs.pfebackendemnagouuiaa.dto.EnqueteSatisfactionResponse;
import fsegs.pfebackendemnagouuiaa.dto.RemplirEnqueteSatisfactionRequest;
import fsegs.pfebackendemnagouuiaa.entities.Stage;

import java.util.List;

public interface EnqueteSatisfactionService {

    List<EnqueteSatisfactionResponse> getEnquetesByStage(Long stageId);

    List<EnqueteSatisfactionResponse> getEnquetesByUtilisateur(Long utilisateurId);

    EnqueteSatisfactionResponse remplirEnquete(Long enqueteId, RemplirEnqueteSatisfactionRequest request);

    List<EnqueteSatisfactionResponse> creerEnquetesPourStageSiNecessaire(Stage stage);

    EnqueteSatisfactionResponse createPendingSurvey(CreateEnqueteSatisfactionRequest request);
}
