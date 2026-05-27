package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.ChangeRoleRequest;
import fsegs.pfebackendemnagouuiaa.dto.CollaborateurSignatureDto;
import fsegs.pfebackendemnagouuiaa.dto.CreateUserRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateEmailRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateEmailResponse;
import fsegs.pfebackendemnagouuiaa.dto.UpdatePasswordRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateProfileRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateUserRequest;
import fsegs.pfebackendemnagouuiaa.dto.UserResponse;
import fsegs.pfebackendemnagouuiaa.services.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/users", "/api/utilisateurs"})
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return new ResponseEntity<>(utilisateurService.createUser(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(utilisateurService.updateUser(id, request));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/signature-collaborateur")
    public ResponseEntity<CollaborateurSignatureDto> getCollaborateurSignature(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.getCollaborateurSignature(id));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/profil")
    public ResponseEntity<UserResponse> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.getProfile(id));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/profile")
    public ResponseEntity<UserResponse> getCurrentProfile() {
        return ResponseEntity.ok(utilisateurService.getCurrentProfile());
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}/profil")
    public ResponseEntity<UserResponse> updateProfile(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(utilisateurService.updateProfile(id, request));
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/profile")
    public ResponseEntity<UserResponse> updateCurrentProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(utilisateurService.updateCurrentProfile(request));
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/email")
    public ResponseEntity<UpdateEmailResponse> updateCurrentEmail(@Valid @RequestBody UpdateEmailRequest request) {
        return ResponseEntity.ok(utilisateurService.updateCurrentEmail(request));
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}/mot-de-passe")
    public ResponseEntity<UserResponse> updatePassword(@PathVariable Long id,
                                                       @Valid @RequestBody UpdatePasswordRequest request) {
        return ResponseEntity.ok(utilisateurService.updatePassword(id, request));
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/mot-de-passe")
    public ResponseEntity<UserResponse> updateCurrentPassword(@Valid @RequestBody UpdatePasswordRequest request) {
        return ResponseEntity.ok(utilisateurService.updateCurrentPassword(request));
    }

    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.getUserById(id));
    }

    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(utilisateurService.getAllUsers());
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_ENTREPRISE')")
    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.deactivateUser(id));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_ENTREPRISE')")
    @PatchMapping("/{id}/activer")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.activateUser(id));
    }

    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        utilisateurService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeUserRole(@PathVariable Long id,
                                                       @Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(utilisateurService.changeUserRole(id, request));
    }
}
