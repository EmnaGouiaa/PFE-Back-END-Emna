package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyValidationItemDto {
    private String key;
    private String type;
    private String title;
    private String description;
    private String status;
    private boolean pending;
    private Long itemId;
    private Long stageId;
    private Long relatedEntityId;
    private String stageTitle;
    private String studentName;
    private String companyName;
    private String dateDebut;
    private String dateFin;
}
