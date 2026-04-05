package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    private String prenom;
    private String nom;
    private String email;
    private Role role;
    private Boolean compteValide;
    private String matricule;
    
    // Type-specific fields (nullable for non-applicable user types)
    private String filiere;
    private String niveau;
    private String niveauStage;
    private String grade;
    private String specialite;
    private String departement;
    private String poste;
    private String service;
    private String adresse;
    private String secteurActivite;
    private String telephone;
}
