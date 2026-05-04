package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CritereEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String libelle;
    private String description;
    private String categorie;
    private Integer bareme;
    private String commentaireGeneral;
    @Enumerated(EnumType.STRING)
    private PartieEvaluation partie;
    @OneToMany(mappedBy = "critereEvaluation")
    @JsonIgnore
    private List<NoteAttribuee> notes = new ArrayList<>();
    @ManyToOne
    private FicheEvaluation fiche;

}
