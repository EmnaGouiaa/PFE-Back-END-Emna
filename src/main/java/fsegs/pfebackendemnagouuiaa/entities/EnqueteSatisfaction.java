package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"questions", "reponses"})
public class EnqueteSatisfaction extends Formulaire {

    @Enumerated(EnumType.STRING)
    private CibleEnquete cible;


    private LocalDate dateCreation;


    private LocalDate dateLimitReponse;

    private Boolean completee;

    @ManyToOne
    @JoinColumn(name = "stage_id")
    private Stage stage;
    // In EnqueteSatisfaction.java
    @ManyToOne
    @JoinColumn(name = "dossier_stage_id")
    private DossierStage dossierStage;

    @ManyToOne
    @JoinColumn(name = "auteur_id")
    private User auteur;

    @OneToMany(mappedBy = "enquete", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionEnquete> questions = new ArrayList<>();

    @OneToMany(mappedBy = "enquete", cascade = CascadeType.ALL , orphanRemoval= true)
    @JsonIgnore

    private List<ReponseEnquete> reponses = new ArrayList<>();

    @Override
    public String getType() { return "ENQUETE_" + cible; }

    @Override
    public boolean estComplet() {
        return completee != null && completee;
    }
    public void addReponse(ReponseEnquete p) {
        this.reponses.add(p);
        p.setEnquete(this);
    }
    public void removePiece(ReponseEnquete p) {
        this.reponses.remove(p);
        p.setEnquete(null);
    }
}
