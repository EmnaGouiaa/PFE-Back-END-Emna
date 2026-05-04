package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.entities.Role;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String adresse;
    private Boolean actif;
    private String nomFichierSignature;
    private Role role;
    private String matricule;
    private String grade;
    private String specialite;
    private String poste;
    private String service;
    private Long entrepriseId;
    private String entrepriseNom;
    private Long filiereId;
    private String filiereNom;
    private LocalDate dateNaiss;
    private Integer niveau;
    private Boolean emailSent;
    private String message;
}
