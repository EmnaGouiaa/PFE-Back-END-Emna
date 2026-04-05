package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.AuthenticationRequest;
import fsegs.pfebackendemnagouuiaa.dto.AuthenticationResponse;
import fsegs.pfebackendemnagouuiaa.dto.RegisterRequest;
import fsegs.pfebackendemnagouuiaa.dto.UserDTO;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.User;
import fsegs.pfebackendemnagouuiaa.repository.UserRepository;
import fsegs.pfebackendemnagouuiaa.services.JwtService;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@RequiredArgsConstructor
@Service
public class AuthenticationService {
 private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
 private final UserRepository userRepository;
 private final PasswordEncoder passwordEncoder;
 private final JwtService jwtService;
 private final AuthenticationManager authenticationManager;

    public  AuthenticationResponse register(RegisterRequest request) {
        var user = User.builder()
                .prenom(request.getFirstname())
                .nom(request.getLastname())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .compteValide(true)
                .role(Role.ADMIN).build();
        userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        
        // Return response with user DTO
        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .role(user.getRole().name())
                .compteValide(user.getCompteValide())
                .build();
                
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .user(userDTO)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        String normalizedEmail = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        
        logger.info("🔐 Authentication attempt for email: {}", normalizedEmail);
        
        // First, find the user to check if they exist and are validated
        var user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    logger.error("❌ User not found with email: {}", normalizedEmail);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
                });
        
        logger.info("✅ User found - ID: {}, Role: {}, Validated: {}", user.getId(), user.getRole(), user.getCompteValide());
        
        // Check if account is validated - only reject if explicitly set to false
        if (Boolean.FALSE.equals(user.getCompteValide())) {
            logger.warn("⚠️ Account not validated for user: {}", normalizedEmail);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account not yet validated. Please contact administrator.");
        }
        
        try {
            logger.info("🔑 Attempting authentication with Spring Security...");
            
            // 🔍 DEBUG: Check password matching
            logger.info("🔍 PASSWORD DEBUG:");
            logger.info("   Raw password: {}", request.getPassword());
            logger.info("   DB password: {}", user.getPassword());
            
            // Check if password matches (this is what Spring Security does internally)
            boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
            logger.info("   Password matches: {}", passwordMatches);
            
            if (!passwordMatches) {
                logger.error("❌ PASSWORD MISMATCH - This is the problem!");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
            }
            
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
            logger.info("✅ Authentication successful for user: {}", normalizedEmail);
        } catch (BadCredentialsException e) {
            logger.error("❌ Bad credentials for user: {}. Error: {}", normalizedEmail, e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        } catch (DisabledException e) {
            logger.error("❌ Account disabled for user: {}. Error: {}", normalizedEmail, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId());
        
        logger.info("🎫 Generating JWT token for user: {} with role: {}", normalizedEmail, user.getRole().name());
        var jwtToken = jwtService.generateToken(claims, user);
        
        // Convert User entity to UserDTO for safe serialization
        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .role(user.getRole().name())
                .compteValide(user.getCompteValide())
                .build();
        
        logger.info("✅ Authentication completed successfully for user: {} (Role: {})", normalizedEmail, user.getRole().name());
        
        // Return both token and user DTO
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .user(userDTO)
                .build();
    }
}
