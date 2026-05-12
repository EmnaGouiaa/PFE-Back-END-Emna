package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.StatutEnqueteSatisfaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnqueteSatisfactionResponse {

    private Long id;
    private LocalDateTime dateCreation;
    private LocalDateTime dateSoumission;
    private StatutEnqueteSatisfaction statutEnquete;
    private String titre;
    private String description;
    private String urlFormulaire;
    private String reponses;
    private String commentaireGlobal;
    private Role roleRepondant;
    private Long stageId;
    private String stageTitre;
    private Long utilisateurId;
    private String utilisateurNomComplet;
    private boolean proprietaire;
    private boolean actionDisponible;
    private boolean sectionEnqueteOuverte;
}
