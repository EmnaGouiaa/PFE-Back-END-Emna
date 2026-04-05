package fsegs.pfebackendemnagouuiaa.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users") // Changed from "user" to avoid SQL reserved keyword
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("USER")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;
    
    @Builder.Default
    private Boolean compteValide = true;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Common fields for all user types (required for single table inheritance)
    // These are nullable as they're only used by specific user subtypes
    @Builder.Default
    private String matricule = "N/A";
    
    private String filiere;
    private String niveau;
    private String niveauStage;
    private String grade;
    private String specialite;
    private String departement;
    private String poste;
    private String service;
    private String adresse;
    private String secteurActivite;
    private String telephone;

    @Override 
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Null-safe guard for role
        if (role == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override 
    @JsonIgnore
    public String getUsername() { 
        return email; 
    }
    
    // Removed duplicate getPassword() - @Data generates it automatically
    // The @Override annotation was causing conflict
    
    @Override 
    @JsonIgnore
    public boolean isAccountNonExpired() { 
        return true; 
    }
    
    @Override 
    @JsonIgnore
    public boolean isAccountNonLocked() { 
        return true; 
    }
    
    @Override 
    @JsonIgnore
    public boolean isCredentialsNonExpired() { 
        return true; 
    }
    
    @Override 
    @JsonIgnore
    public boolean isEnabled() { 
        // Safe unboxing of Boolean to boolean - defaults to true for null
        return !Boolean.FALSE.equals(this.compteValide);
    }
}

