package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StagiaireRequestDTO {
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String telephone;
    private Boolean actif;
    private String urlSignature;

    private String matricule;
    private LocalDate dateNaiss;
    private Long filiereId;
    private Integer niveau;
}
