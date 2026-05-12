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

@Entity
@Inheritance(strategy = InheritanceType.JOINED) // ou SINGLE_TABLE
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@DiscriminatorColumn(name = "dtype")
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

    private String nomFichierSignature ;

    // Méthodes de sécurité Spring
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
