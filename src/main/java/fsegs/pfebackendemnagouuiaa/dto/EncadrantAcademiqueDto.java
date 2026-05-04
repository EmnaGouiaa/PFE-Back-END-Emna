package fsegs.pfebackendemnagouuiaa.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncadrantAcademiqueDto {

    private Long id;

    private String nom;
    private String prenom;
    private String email;
    private String telephone;

    private String grade;
    private String matricule;
    private String specialite;
}