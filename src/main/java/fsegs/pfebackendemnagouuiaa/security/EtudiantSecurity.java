package fsegs.pfebackendemnagouuiaa.security;

import fsegs.pfebackendemnagouuiaa.entities.Etudiant;
import fsegs.pfebackendemnagouuiaa.entities.User;
import org.springframework.security.core.Authentication;

public class EtudiantSecurity {
    public static boolean isOwnerOrAdmin(User user, Long etudiantId) {
        if (user == null || etudiantId == null) return false;
        
        // Admin can access everything
        if (user.getRole() == fsegs.pfebackendemnagouuiaa.entities.Role.ADMIN) {
            return true;
        }
        
        // Student can only access their own data
        if (user instanceof Etudiant) {
            Etudiant etudiant = (Etudiant) user;
            return etudiant.getId().equals(etudiantId);
        }
        
        return false;
    }
    
    public static boolean canAccessEtudiant(Authentication authentication, Long etudiantId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        User user = (User) authentication.getPrincipal();
        return isOwnerOrAdmin(user, etudiantId);
    }
}
