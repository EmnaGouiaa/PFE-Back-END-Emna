package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@DiscriminatorValue("Stagiaire")
public class Stagiaire extends Utilisateur {
    private LocalDate dateNaiss;

    private Integer niveau;

    @OneToMany(mappedBy = "stagiaire")
    @JsonIgnore
    private List<Stage> stages = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encadrant_academique_id")
    @JsonIgnore
    private EncadrantAcademique encadrantAcademique;

    @ManyToOne
    @JoinColumn(name = "filiere_id")
    private Filiere filiere;

}
