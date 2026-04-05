package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"stage", "dossierStage"})
public class ConventionStage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;


    private LocalDate dateCreation;

    private LocalDate dateSignature;

    private Boolean signeeEntreprise;
    private Boolean signeeEtablissement;
    private Boolean signeeStagiaire;

    @Enumerated(EnumType.STRING)
    private StatutConvention statut;

    @OneToOne
    @JoinColumn(name = "stage_id")
    @JsonIgnore
    private Stage stage;

    @OneToOne
    @JoinColumn(name = "dossier_stage_id")
    @JsonIgnore
    private DossierStage dossierStage;

    public boolean estSigneeParTous() {
        return signeeEntreprise && signeeEtablissement && signeeStagiaire;
    }

    public void activerStage() {
        if (estSigneeParTous()) {
            this.statut = StatutConvention.ACTIVEE;
            this.stage.demarrer();
        }
    }
}
