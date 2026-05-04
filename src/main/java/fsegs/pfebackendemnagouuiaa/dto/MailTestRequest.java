package fsegs.pfebackendemnagouuiaa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailTestRequest {

    @NotBlank(message = "L'email destinataire est obligatoire.")
    @Email(message = "Le destinataire doit avoir un format email valide.")
    private String recipientEmail;
}
