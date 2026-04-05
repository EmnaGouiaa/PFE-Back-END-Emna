package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CreateUserRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateUserRequest;
import fsegs.pfebackendemnagouuiaa.dto.UserResponse;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.User;
import fsegs.pfebackendemnagouuiaa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final int PASSWORD_LENGTH = 12;

    /**
     * Create a new user with admin-provided password
     */
    public UserResponse createUser(CreateUserRequest request) {
        // Check if email already exists
        if (userRepository.findByEmailIgnoreCase(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .prenom(request.getPrenom())
                .nom(request.getNom())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .compteValide(true)
                .matricule("N/A")
                .build();

        User savedUser = userRepository.save(user);
        return convertToResponse(savedUser);
    }

    /**
     * Generate a random secure password
     */
    public String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return password.toString();
    }

    /**
     * Get all users
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get user by ID
     */
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return convertToResponse(user);
    }

    /**
     * Update user (with role-based authorization)
     */
    public UserResponse updateUser(Long id, UpdateUserRequest request, User currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Authorization check: only ADMIN or the user themselves can update
        if (!currentUser.getId().equals(id) && currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied!");
        }

        // Update fields if provided
        if (request.getPrenom() != null) {
            user.setPrenom(request.getPrenom());
        }
        if (request.getNom() != null) {
            user.setNom(request.getNom());
        }
        if (request.getEmail() != null) {
            String normalizedEmail = request.getEmail().trim().toLowerCase();
            // Check if new email is already taken by another user
            userRepository.findByEmailIgnoreCase(normalizedEmail)
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(id)) {
                            throw new RuntimeException("Email already in use: " + normalizedEmail);
                        }
                    });
            user.setEmail(normalizedEmail);
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null && currentUser.getRole() == Role.ADMIN) {
            user.setRole(request.getRole());
        }
        if (request.getCompteValide() != null) {
            user.setCompteValide(request.getCompteValide());
        }

        User updatedUser = userRepository.save(user);
        return convertToResponse(updatedUser);
    }

    /**
     * Delete user (ADMIN only)
     */
    public void deleteUser(Long id, User currentUser) {
        // Only ADMIN can delete users
        if (currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only ADMIN can delete users!");
        }

        // Prevent self-deletion
        if (currentUser.getId().equals(id)) {
            throw new RuntimeException("Cannot delete your own account!");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        userRepository.delete(user);
    }

    /**
     * Convert User entity to UserResponse DTO
     */
    private UserResponse convertToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .prenom(user.getPrenom())
                .nom(user.getNom())
                .email(user.getEmail())
                .role(user.getRole())
                .compteValide(user.getCompteValide())
                .matricule(user.getMatricule())
                .filiere(user.getFiliere())
                .niveau(user.getNiveau())
                .niveauStage(user.getNiveauStage())
                .grade(user.getGrade())
                .specialite(user.getSpecialite())
                .departement(user.getDepartement())
                .poste(user.getPoste())
                .service(user.getService())
                .adresse(user.getAdresse())
                .secteurActivite(user.getSecteurActivite())
                .telephone(user.getTelephone())
                .build();
    }
}
