package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CahierStage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.DATE)
    private Date dateOuverture;

    @OneToOne
    @JoinColumn(name = "dossier_stage_id")
    private DossierStage dossierStage;

    @OneToMany(mappedBy = "cahierStage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeuillePresence> feuillesPresence = new ArrayList<>();

    @OneToMany(mappedBy = "cahierStage", cascade = CascadeType.ALL)
    private List<RapportHebdomadaire> rapportsHebdomadaires = new ArrayList<>();

    public void initialiserFeuilles(Integer dureeSemaines) {
        for (int i = 1; i <= dureeSemaines; i++) {
            FeuillePresence fp = new FeuillePresence();
            fp.setSemaine(i);
            fp.setCahierStage(this);
            this.feuillesPresence.add(fp);
        }
    }

}
