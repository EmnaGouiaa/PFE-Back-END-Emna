package fsegs.pfebackendemnagouuiaa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compte utilisateur racine du système, intégré à Spring Security via {@link UserDetails}.
 *
 * <h3>Rôle métier</h3>
 * Représente toute personne pouvant se connecter : stagiaire, encadrants, responsables
 * entreprise ou universitaires, administrateur. Le discriminant JPA {@code dtype} distingue
 * les sous-types ({@link Stagiaire}, {@link EncadrantAcademique}, etc.).
 *
 * <h3>Mapping JPA</h3>
 * Stratégie {@link InheritanceType#JOINED} : table {@code utilisateur} + tables filles par sous-type.
 * Colonne {@code dtype} comme discriminant. Identifiant technique {@link #id}.
 *
 * <h3>Champs clés</h3>
 * <ul>
 *   <li>{@link #email} — identifiant de connexion (unique, obligatoire).</li>
 *   <li>{@link #role} — {@link Role}, source des autorités Spring ({@code ROLE_*}).</li>
 *   <li>{@link #actif}, {@link #supprime}, {@link #doitChangerMotDePasse} — états du compte.</li>
 *   <li>{@link #urlSignature} — image de signature (Base64 ou URL) pour les documents PDF.</li>
 *   <li>{@link #reunions} — participation N-N aux {@link Reunion} via table de jointure.</li>
 * </ul>
 *
 * <h3>Consommation applicative</h3>
 * <ul>
 *   <li>Services : {@code UtilisateurServiceImpl}, {@code AuthenticationServiceImpl},
 *       {@code ServiceAuthentification}, et la plupart des services métier (résolution du connecté).</li>
 *   <li>Contrôleurs : {@code UtilisateurController}, {@code AuthenticationController},
 *       {@code ControleurAuthentification}, {@code ProfileController}.</li>
 * </ul>
 *
 * @see Role
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED) // ou SINGLE_TABLE
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@DiscriminatorColumn(name = "dtype")
//userDetail représentant l'utilisateur connecté , elle est from spring security
public class Utilisateur implements UserDetails {


    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String telephone;

    @Column(unique = true)
    private String matricule;

    private String adresse;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;
    @JsonIgnore
    @Column(nullable = false)
    private String motDePasse;


    @Builder.Default
    private Boolean actif = true;

    @Builder.Default
    private Boolean supprime = false;

    @Builder.Default
    private Boolean doitChangerMotDePasse = false;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** URL, chemin ou donnee Base-64 de la signature manuscrite de l'utilisateur.
     *  Stocke en LONGTEXT pour accepter les images encodees en Base-64 (~100-500 Ko). */
    @Column(columnDefinition = "LONGTEXT")
    private String urlSignature;

    // ── Contrat Spring Security (UserDetails) ───────────────────────────────────

    /**
     * Construit l'autorité unique {@code ROLE_<nomRole>} à partir de {@link #role}.
     * Utilisé par les filtres de sécurité pour l'annotation {@code @PreAuthorize}.
     */
    @Override 
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role != null ? List.of(new SimpleGrantedAuthority("ROLE_" + role.name())) : List.of();
    }
    
    @Override 
    @JsonIgnore

    public String getPassword() { return motDePasse; }

    @Override 
    @JsonIgnore
    public String getUsername() { return email; }

    @Override 
    @JsonIgnore
    public boolean isAccountNonExpired() { return true; }
    
    @Override 
    @JsonIgnore
    public boolean isAccountNonLocked() { return true; }
    
    @Override 
    @JsonIgnore
    public boolean isCredentialsNonExpired() { return true; }
    
    /**
     * Compte utilisable si non désactivé explicitement et non marqué supprimé (soft delete).
     * Les valeurs {@code null} en base (anciens comptes) sont traitées comme actives.
     */
    @Override 
    @JsonIgnore
    public boolean isEnabled() { 
        // Si la valeur est null en base (pour les vieux comptes), on considère qu'il est actif
        return !Boolean.FALSE.equals(this.actif) && !Boolean.TRUE.equals(this.supprime);
    }

    @ManyToMany(mappedBy = "participants")
    @JsonIgnore
    private Set<Reunion> reunions = new HashSet<>();

}
