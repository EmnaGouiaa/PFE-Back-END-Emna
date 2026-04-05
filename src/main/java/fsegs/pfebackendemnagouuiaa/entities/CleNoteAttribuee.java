package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CleNoteAttribuee {
    private Long critereId;
    private Long ficheId;
}