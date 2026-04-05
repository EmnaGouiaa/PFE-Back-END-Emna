package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor

public class ResponsableServiceStages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String service;

    @OneToMany(mappedBy = "valideePar")
    private List<OffreStage> offresValidees = new ArrayList<>();

    public void validerOffre(OffreStage offre) {
        offre.valider(this);
    }

    public void rejeterOffre(OffreStage offre, String motif) {
        offre.rejeter(motif);
    }

    public void attribuerEncadrant(Stage stage, EncadrantAcademique encadrant) {
        stage.setEncadrantAcademique(encadrant);

    }
}
