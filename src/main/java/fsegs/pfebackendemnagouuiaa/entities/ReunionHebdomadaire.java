package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Réunion de suivi périodique ({@code type_reunion = HEBDOMADAIRE}), rattachée au {@link CahierStage}.
 *
 * @see ReunionServiceImpl
 * @see ReunionHebdomadaireController
 */
@Entity
@DiscriminatorValue("HEBDOMADAIRE")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReunionHebdomadaire extends Reunion {
}
