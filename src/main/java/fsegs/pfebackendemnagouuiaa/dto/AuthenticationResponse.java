package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class AuthenticationResponse {
    private String token;
    private Long userId;
    private String nom;
    private String prenom;
    private String email;
    private Role role;
    private Boolean doitChangerMotDePasse;
}
