package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conventions_stage")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConventionStage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Integer numConv;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private Boolean signeeEncAca = false;
    private Long signataireEncAcaId;
    private String roleSignatureEncAca;
    private String nomSignataireEncAca;
    private String imageSignatureEncAca;
    private LocalDateTime dateSignatureEncAca;

    private Boolean signeeEncPro = false;
    private Long signataireEncProId;
    private String roleSignatureEncPro;
    private String nomSignataireEncPro;
    private String imageSignatureEncPro;
    private LocalDateTime dateSignatureEncPro;

    private Boolean signeeEntreprise = false;
    private Long signataireEntrepriseId;
    private String roleSignatureEntreprise;
    private String nomSignataireEntreprise;
    private String imageSignatureEntreprise;
    private LocalDateTime dateSignatureEntreprise;

    private Boolean signeeResp = false;
    private Long signataireResponsableUniversitaireId;
    private String roleSignatureResponsableUniversitaire;
    private String imageSignatureResponsableUniversitaire;

    private LocalDateTime dateSignatureResponsableUniversitaire;

    private String nomResponsableUniversitaireSignataire;

    private Boolean signeeStagiaire = false;
    private Long signataireStagiaireId;
    private String roleSignatureStagiaire;
    private String nomSignataireStagiaire;
    private String imageSignatureStagiaire;
    private LocalDateTime dateSignatureStagiaire;

    private Boolean statutSignatures = false;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", unique = true)
    private Stage stage;
    @ManyToOne
    @JoinColumn(name = "demande_stage_id")
    private DemandeCreationCompteEntreprise demandeStage;
}
