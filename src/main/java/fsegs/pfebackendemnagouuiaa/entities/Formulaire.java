package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.boot.internal.Abstract;

import java.time.LocalDate;
import java.util.Date;
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Formulaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private LocalDate dateCreation;

    @Enumerated(EnumType.STRING)
    private StatutFormulaire statut;
    public abstract String getType();
    public abstract boolean estComplet();

    public void soumettre() { this.statut = StatutFormulaire.SOUMIS; }
    public void valider() { this.statut = StatutFormulaire.VALIDE; }
    public void rejeter(String motif) { this.statut = StatutFormulaire.REJETE; }

}
