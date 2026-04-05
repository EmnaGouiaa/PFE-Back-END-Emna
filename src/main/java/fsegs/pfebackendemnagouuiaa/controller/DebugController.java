package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.entities.User;
import fsegs.pfebackendemnagouuiaa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/debug")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class DebugController {

    private final UserRepository userRepository;

    /**
     * Check current authentication and authorities
     */
    @GetMapping("/auth-check")
    public ResponseEntity<Map<String, Object>> checkAuth(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("authenticated", false);
            response.put("message", "Not authenticated");
            return ResponseEntity.ok(response);
        }
        
        User user = (User) authentication.getPrincipal();
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        response.put("authenticated", true);
        response.put("username", authentication.getName());
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("role", user.getRole() != null ? user.getRole().name() : "NULL");
        response.put("authorities", authorities);
        response.put("compteValide", user.getCompteValide());
        response.put("isEnabled", user.isEnabled());
        response.put("isAccountNonExpired", user.isAccountNonExpired());
        response.put("isAccountNonLocked", user.isAccountNonLocked());
        response.put("isCredentialsNonExpired", user.isCredentialsNonExpired());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check ADMIN user status (public endpoint for debugging)
     */
    @GetMapping("/admin-status")
    public ResponseEntity<Map<String, Object>> checkAdminStatus() {
        Map<String, Object> response = new HashMap<>();
        
        var adminUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && u.getRole().name().equals("ADMIN"))
                .toList();
        
        if (adminUsers.isEmpty()) {
            response.put("error", "No ADMIN user found");
            return ResponseEntity.ok(response);
        }
        
        User admin = adminUsers.get(0);
        String password = admin.getPassword();
        boolean isBcryptEncoded = password != null && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
        
        Map<String, Object> adminInfo = new HashMap<>();
        adminInfo.put("id", admin.getId());
        adminInfo.put("email", admin.getEmail());
        adminInfo.put("nom", admin.getNom());
        adminInfo.put("prenom", admin.getPrenom());
        adminInfo.put("role", admin.getRole().name());
        adminInfo.put("compteValide", admin.getCompteValide());
        adminInfo.put("isEnabled", admin.isEnabled());
        adminInfo.put("passwordEncoded", isBcryptEncoded);
        adminInfo.put("passwordPrefix", password != null ? password.substring(0, Math.min(30, password.length())) : "NULL");
        adminInfo.put("authorities", admin.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        
        response.put("adminCount", adminUsers.size());
        response.put("admin", adminInfo);
        response.put("issues", getIssues(admin, isBcryptEncoded));
        
        return ResponseEntity.ok(response);
    }
    
    private List<String> getIssues(User admin, boolean isBcryptEncoded) {
        List<String> issues = new java.util.ArrayList<>();
        
        if (!isBcryptEncoded) {
            issues.add("Password is NOT BCrypt encoded! Admin login will fail.");
        }
        if (Boolean.FALSE.equals(admin.getCompteValide())) {
            issues.add("compteValide is false! Account is disabled.");
        }
        if (admin.getRole() == null) {
            issues.add("Role is NULL!");
        }
        if (!admin.isEnabled()) {
            issues.add("isEnabled() returns false! Account is disabled.");
        }
        
        return issues.isEmpty() ? List.of("No issues detected ✅") : issues;
    }
}
