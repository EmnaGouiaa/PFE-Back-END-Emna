package fsegs.pfebackendemnagouuiaa.controller;

import fsegs.pfebackendemnagouuiaa.dto.DemandeAuthentification;
import fsegs.pfebackendemnagouuiaa.dto.ReponseAuthentification;
import fsegs.pfebackendemnagouuiaa.services.ServiceAuthentification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/authentification")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class ControleurAuthentification {

    private final ServiceAuthentification serviceAuthentification;

    @PostMapping("/login")
    public ResponseEntity<ReponseAuthentification> login(@RequestBody DemandeAuthentification request) {
        return ResponseEntity.ok(serviceAuthentification.authentifier(request));
    }
}