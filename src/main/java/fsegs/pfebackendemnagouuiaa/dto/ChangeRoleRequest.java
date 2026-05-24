package fsegs.pfebackendemnagouuiaa.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeRoleRequest {

    @NotBlank(message = "Le role est obligatoire.")
    private String role;
}
