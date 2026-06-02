package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Enregistrement d'une absence (ou d'un nombre d'heures/jours d'absence) pour un {@link Stage}.
 *
 * <h3>Mapping JPA</h3>
 * Table {@code absence}. Association obligatoire {@code @ManyToOne} vers {@link Stage}.
 *
 * <h3>Consommation applicative</h3>
 * <ul>
 *   <li>Service : {@code AbsenceServiceImpl} ; export PDF : {@code CahierStagePdfService}.</li>
 *   <li>Contrôleur : {@code AbsenceController}.</li>
 * </ul>
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateAbsence;

    private Integer nbAbsence; // nombre d'heures ou de jours

    @Column(length = 500)
    private String justification;

    @Column(length = 500)
    private String commentaire;

    @Column(length = 100)
    private String statut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;
}
