package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Réunion de soutenance / clôture de stage ({@code type_reunion = FINALE}).
 * Hérite de tous les champs de {@link Reunion} sans attribut supplémentaire.
 *
 * <p>Point d'ancrage pour la {@link FicheEvaluation} ({@code reunionFinale_id}).</p>
 *
 * @see ReunionFinaleServiceImpl
 * @see ReunionFinaleController
 */
@Entity
@DiscriminatorValue("FINALE")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ReunionFinale extends Reunion {
}
