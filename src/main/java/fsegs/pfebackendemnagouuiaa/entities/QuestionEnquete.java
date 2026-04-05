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
public class QuestionEnquete {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String libelle;

    private Boolean obligatoire;

    @Enumerated(EnumType.STRING)
    private CibleEnquete applicableA;

    @ManyToOne
    @JoinColumn(name = "enquete_id")
    private EnqueteSatisfaction enquete;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<ReponseEnquete> reponses = new ArrayList<>();

    public boolean estApplicableA(CibleEnquete cible) {
        return applicableA == cible || applicableA == CibleEnquete.LES_DEUX;
    }
}
