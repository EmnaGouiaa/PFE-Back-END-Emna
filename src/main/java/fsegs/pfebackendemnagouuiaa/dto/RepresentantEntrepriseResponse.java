package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepresentantEntrepriseResponse {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private Long entrepriseId;
    private String nomEntreprise;
}
