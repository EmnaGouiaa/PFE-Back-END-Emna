package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ResponsableEntreprise extends User {


    private String adresse;

    private String secteurActivite;

    private String telephone;

    @Column(updatable = false)
    private LocalDateTime dateCreation;

    private String poste;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true, name = "entreprise")
    private Entreprise entreprise;
}
