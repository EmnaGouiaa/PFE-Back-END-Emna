package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.AuthenticationRequest;
import fsegs.pfebackendemnagouuiaa.dto.AuthenticationResponse;
import fsegs.pfebackendemnagouuiaa.dto.ForgotPasswordRequest;
import fsegs.pfebackendemnagouuiaa.dto.PasswordResetResponse;
import fsegs.pfebackendemnagouuiaa.dto.ResetPasswordRequest;

public interface AuthenticationService {
    AuthenticationResponse authenticate(AuthenticationRequest request);
    PasswordResetResponse forgotPassword(ForgotPasswordRequest request);
    PasswordResetResponse resetPassword(ResetPasswordRequest request);
}
