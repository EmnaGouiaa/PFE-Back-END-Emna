package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CahierStage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateGeneration;

    private LocalDate dateSignature;

    private Boolean estSigne = false;

    private Boolean signeeEncAcad = false;
    private Long signataireEncAcadId;
    private String roleSignatureEncAcad;
    private String nomSignataireEncAcad;
    private String imageSignatureEncAcad;
    private LocalDateTime dateSignatureEncAcad;

    private Boolean signeeEncPro = false;
    private Long signataireEncProId;
    private String roleSignatureEncPro;
    private String nomSignataireEncPro;
    private String imageSignatureEncPro;
    private LocalDateTime dateSignatureEncPro;

    private Boolean signeeRespEntreprise = false;
    private Long signataireRespEntrepriseId;
    private String roleSignatureRespEntreprise;
    private String nomSignataireRespEntreprise;
    private String imageSignatureRespEntreprise;
    private LocalDateTime dateSignatureRespEntreprise;

    private Boolean signeeStagiaire = false;
    private Long signataireStagiaireId;
    private String roleSignatureStagiaire;
    private String nomSignataireStagiaire;
    private String imageSignatureStagiaire;
    private LocalDateTime dateSignatureStagiaire;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", unique = true)
    private Stage stage;
    @OneToMany(mappedBy = "cahierStage")
    private List<ReunionHebdomadaire> reunionHebdomadaires = new ArrayList<>();

}
