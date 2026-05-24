package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReunionFinaleFormulairesUpdateRequest {

    private String urlFormSatisfaction;
    private String titreEnqueteSatisfaction;
    private String descriptionEnqueteSatisfaction;
}
