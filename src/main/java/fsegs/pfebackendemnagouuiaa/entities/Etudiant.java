package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(exclude = {"dossiersStage", "stages", "reponses"})
public class Etudiant extends User {
    @Column(nullable = false, unique = true)
    private String matricule;

    private String filiere;

    private String niveau;

    private String niveauStage;
    @OneToMany(mappedBy = "stagiaire")
    @JsonIgnore
    private List<DossierStage> dossiersStage = new ArrayList<>();

    @OneToMany(mappedBy = "stagiaire")
    @JsonIgnore
    private List<Stage> stages = new ArrayList<>();

    @OneToMany(mappedBy = "auteur")
    @JsonIgnore
    private List<ReponseEnquete> reponses = new ArrayList<>();

    public List<OffreStage> consulterOffres() {
        return new ArrayList<>();
    }

    public void repondreEnquete(EnqueteSatisfaction enquete) {
        // Logique de réponse
    }

}
