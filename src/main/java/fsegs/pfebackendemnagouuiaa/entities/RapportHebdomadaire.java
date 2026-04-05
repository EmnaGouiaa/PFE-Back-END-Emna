package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RapportHebdomadaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer semaine;

    @Column(length = 3000)
    private String contenu;

    private String tachesRealisees;
    private String difficultes;
    private Boolean valideParEncadrant;
    private String commentaireEncadrant;

    @ManyToOne
    @JoinColumn(name = "cahier_stage_id")
    private CahierStage cahierStage;

    public void soumettre() {
        // Logique de soumission
    }

    public void valider() {
        this.valideParEncadrant = true;
    }
}
