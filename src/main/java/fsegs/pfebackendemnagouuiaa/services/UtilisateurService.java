package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ChangeRoleRequest;
import fsegs.pfebackendemnagouuiaa.dto.CreateUserRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateEmailRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateEmailResponse;
import fsegs.pfebackendemnagouuiaa.dto.UpdatePasswordRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateProfileRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateUserRequest;
import fsegs.pfebackendemnagouuiaa.dto.UserResponse;

import java.util.List;

public interface UtilisateurService {
    UserResponse createUser(CreateUserRequest request);
    UserResponse updateUser(Long id, UpdateUserRequest request);
    UserResponse getCurrentProfile();
    UserResponse getProfile(Long id);
    UserResponse updateCurrentProfile(UpdateProfileRequest request);
    UserResponse updateProfile(Long id, UpdateProfileRequest request);
    UpdateEmailResponse updateCurrentEmail(UpdateEmailRequest request);
    UserResponse updateCurrentPassword(UpdatePasswordRequest request);
    UserResponse updatePassword(Long id, UpdatePasswordRequest request);
    UserResponse getUserById(Long id);
    List<UserResponse> getAllUsers();
    UserResponse deactivateUser(Long id);
    UserResponse activateUser(Long id);
    void deleteUser(Long id);
    UserResponse changeUserRole(Long id, ChangeRoleRequest request);
}
