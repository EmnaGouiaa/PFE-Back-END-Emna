package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDto {
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private LocalDateTime dateTime;
    private boolean read;
    private LocalDateTime readAt;
    private String type;
    private Long relatedEntityId;
    private String relatedEntityType;
}
