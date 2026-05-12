package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.entities.StatutActionNotification;
import fsegs.pfebackendemnagouuiaa.entities.StatutNotification;
import fsegs.pfebackendemnagouuiaa.entities.TypeNotification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDestinataireResponse {
    private Long notificationDestinataireId;
    private Long notificationId;
    private String titre;
    private String body;
    private TypeNotification typeNotification;
    private LocalDateTime dateCreation;
    private Long stageId;
    private Long reunionId;
    private Long reunionFinaleId;
    private Long reunionHebdomadaireId;
    private Long cahierStageId;
    private Long ficheEvaluationId;
    private Long conventionId;
    private Long demandeId;
    private StatutNotification statutNotification;
    private StatutActionNotification statutActionNotif;
    private LocalDateTime dateLecture;
    private LocalDateTime dateAction;
    private String reponse;
}
