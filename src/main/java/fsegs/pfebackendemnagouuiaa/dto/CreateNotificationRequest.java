package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.entities.TypeNotification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateNotificationRequest {
    @NotBlank
    private String titre;

    @NotBlank
    private String body;

    @NotNull
    private TypeNotification typeNotification;

    @NotEmpty
    private List<Long> destinataireIds;

    private Long parentNotificationId;
    private Long stageId;
    private Long reunionId;
    private Long reunionFinaleId;
    private Long reunionHebdomadaireId;
    private Long cahierStageId;
    private Long ficheEvaluationId;
    private Long conventionId;
    private Long demandeId;
}
