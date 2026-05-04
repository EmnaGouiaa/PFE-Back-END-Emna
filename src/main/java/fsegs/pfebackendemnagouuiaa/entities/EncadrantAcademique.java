package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("EncadrantAcademique") // si SINGLE_TABLE
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder

public class EncadrantAcademique extends Utilisateur {

    private String grade;

    private String specialite;

    @OneToMany(mappedBy = "encadrantAcademique")
    @JsonIgnore
    private List<Stage> stages = new ArrayList<>();

    @OneToMany(mappedBy = "encadrantAcademique")
    @JsonIgnore
    private List<Stagiaire> stagiaires = new ArrayList<>();
}
