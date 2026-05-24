package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurerEnqueteRequest {
    private String titre;
    private String description;
    private String urlFormulaire;
}
