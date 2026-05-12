package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RapportEnqueteSatisfactionResponse {

    private Long id;
    private String nomFichier;
    private LocalDateTime dateUpload;
    private Long stageId;
    private Long uploadedById;
    private String uploadedByNomComplet;
}
