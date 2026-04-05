package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.entities.Role;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    
    private String prenom;
    
    private String nom;
    
    @Email(message = "Email should be valid")
    private String email;
    
    private String password; // Optional - only update if provided
    
    private Role role;
    
    private Boolean compteValide;
}
