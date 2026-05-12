package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEnqueteSatisfactionRequest {

    private Long stageId;
    private Long utilisateurId;
    private Role roleRepondant;
    private String reponses;
    private String commentaireGlobal;
}
