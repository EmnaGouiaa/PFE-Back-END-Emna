package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.MailTestRequest;
import fsegs.pfebackendemnagouuiaa.dto.MailTestResponse;
import fsegs.pfebackendemnagouuiaa.services.AccountEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/mail")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRATEUR')")
public class AdminMailController {

    private final AccountEmailService accountEmailService;

    @PostMapping("/test")
    public ResponseEntity<MailTestResponse> sendTestEmail(@Valid @RequestBody MailTestRequest request) {
        return ResponseEntity.ok(accountEmailService.testSmtpAndSend(request.getRecipientEmail()));
    }
}
