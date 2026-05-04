package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteAttribueeDto {

    private Long ficheEvaluationId;
    private Long critereEvaluationId;

    private Integer poids;
    private Integer bareme;
    private Integer note;
    private String commentaire;

    private String critereLibelle;
    private Double scorePondere;
    private Boolean evaluee;
}