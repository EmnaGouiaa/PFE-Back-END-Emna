package fsegs.pfebackendemnagouuiaa.services;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import fsegs.pfebackendemnagouuiaa.dto.FicheEvaluationDto;
import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.NoteAttribuee;
import fsegs.pfebackendemnagouuiaa.entities.RoleSignature;
import fsegs.pfebackendemnagouuiaa.entities.Signature;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.services.pdf.InternshipPdfEvaluationFormat;
import fsegs.pfebackendemnagouuiaa.services.pdf.InternshipPdfLayout;
import fsegs.pfebackendemnagouuiaa.services.pdf.InternshipPdfSignatureSlot;
import fsegs.pfebackendemnagouuiaa.services.pdf.InternshipPdfTheme;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static fsegs.pfebackendemnagouuiaa.services.pdf.InternshipPdfTheme.*;

/**
 * Fiche d'evaluation de stage — template PDF unifie FSEGS.
 */
@Service
@RequiredArgsConstructor
public class FicheEvaluationPdfService {

    private final SignatureImagePdfHelper signatureImagePdfHelper;
    private final UtilisateurRepository utilisateurRepository;

    public byte[] generer(FicheEvaluationDto fiche) throws IOException {
        return renderPdf(fiche, null);
    }

    public byte[] generer(Stage stage, FicheEvaluation fiche) throws IOException {
        FicheEvaluationDto dto = mapToDto(stage, fiche);
        return renderPdf(dto, stage);
    }

    private byte[] renderPdf(FicheEvaluationDto fiche, Stage stage) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, MARGIN_LEFT);

        InternshipPdfLayout layout = new InternshipPdfLayout(document, signatureImagePdfHelper);

        boolean locked = Boolean.TRUE.equals(fiche.getVerrouillee());
        boolean bothSigned = Boolean.TRUE.equals(fiche.getSignaturesCompletes());
        String noteStr = InternshipPdfEvaluationFormat.formatFinalScoreValue(fiche.getNoteFinale());

        layout.addDocumentHeader(
                "Fiche d'évaluation de stage",
                "Projet de Fin d'Études (PFE) — " + safeText(fiche.getStageTitre()),
                bothSigned ? "✓ Signatures complètes" : "⏳ En cours",
                bothSigned,
                List.of(
                        new String[]{"Réf.", "FE-PFE-001"},
                        new String[]{"Note finale", InternshipPdfEvaluationFormat.formatFinalScoreHeaderValue(fiche.getNoteFinale())},
                        new String[]{"État", locked ? "Verrouillée" : "En cours"}
                )
        );

        document.add(layout.summaryCards(List.of(
                new String[]{"Stagiaire", safeText(fiche.getStagiaireNomComplet())},
                new String[]{"Entreprise", safeText(fiche.getEntrepriseNom())},
                new String[]{"Section", safeText(fiche.getSectionStagiaire())},
                new String[]{"Verrouillage", locked ? "Actif" : "En attente"}
        )));

        layout.addSection("Informations stagiaire");
        document.add(layout.keyValueTable(List.of(
                new String[]{"Nom et prénom", safeText(fiche.getStagiaireNomComplet())},
                new String[]{"Section / filière", safeText(fiche.getSectionStagiaire())},
                new String[]{"Entreprise / lieu", safeText(fiche.getEntrepriseNom())}
        )));

        layout.addSection("Évaluation par les encadrants");
        InternshipPdfSignatureSlot epSig = signatureSlotFromDto(
                fiche, RoleSignature.ENCADRANT_PROFESSIONNEL,
                "Encadrant professionnel",
                fiche.getNomSignataireEncadrantProfessionnel(),
                fiche.getSignatureEncadrantProfessionnel(),
                fiche.getDateSignatureEncadrantProfessionnel(),
                stage != null ? profileUrl(stage.getEncadrantProfessionnel()) : "");

        InternshipPdfSignatureSlot reSig = signatureSlotFromDto(
                fiche, RoleSignature.RESPONSABLE_ENTREPRISE,
                "Responsable entreprise",
                fiche.getNomSignataireRepresentantEntreprise(),
                fiche.getSignatureRepresentantEntreprise(),
                fiche.getDateSignatureRepresentantEntreprise(),
                stage != null ? profileUrl(stage.getTuteurEntreprise()) : "");

        document.add(layout.twoColumnCards(
                layout.evaluatorCard("Encadrant professionnel",
                        fiche.getPointFortEncadrantPro(),
                        fiche.getAxeAmeliorationEncadrantPro(),
                        epSig),
                layout.evaluatorCard("Responsable entreprise",
                        fiche.getPointFortResponsableEntreprise(),
                        fiche.getAxeAmeliorationResponsableEntreprise(),
                        reSig)
        ));

        layout.addSection("Notes attribuées");
        document.add(buildNotesTable(layout, fiche));

        String debut = fiche.getStageDateDebut() != null ? fiche.getStageDateDebut().format(DATE_FMT) : "—";
        String fin = fiche.getStageDateFin() != null ? fiche.getStageDateFin().format(DATE_FMT) : "—";
        String reunion = safeText(fiche.getReunionFinaleNumero());
        if (fiche.getReunionFinaleDate() != null) {
            reunion += " — " + fiche.getReunionFinaleDate().format(DATE_FMT);
            if (fiche.getReunionFinaleHeure() != null) {
                reunion += " " + fiche.getReunionFinaleHeure();
            }
        }

        layout.addSection("Informations liées au stage");
        document.add(layout.keyValueTable(List.of(
                new String[]{"Stage", safeText(fiche.getStageTitre())},
                new String[]{"Sujet", safeText(fiche.getStageSujet())},
                new String[]{"Période", debut + " → " + fin},
                new String[]{"Réunion finale", reunion}
        )));

        layout.addHighlightBox("NOTE FINALE", noteStr, InternshipPdfEvaluationFormat.FINAL_SCORE_SUBTITLE);

        layout.addSection("Échelle de notation");
        document.add(layout.keyValueTable(List.of(
                new String[]{"1", "Très insuffisant"},
                new String[]{"2", "Insuffisant"},
                new String[]{"3", "Satisfaisant"},
                new String[]{"4", "Bien"},
                new String[]{"5", "Excellent"}
        )));

        layout.addSection("Signatures finales");
        document.add(layout.signatureGrid(List.of(epSig, reSig)));

        layout.addNotice(locked
                ? "Fiche verrouillée — les deux signatures sont complètes."
                : "La fiche est verrouillée lorsque les deux signatures sont complètes.", locked);

        layout.addFooter();
        document.close();
        return baos.toByteArray();
    }

    private Table buildNotesTable(InternshipPdfLayout layout, FicheEvaluationDto fiche) {
        List<NoteAttribueeDto> notes = fiche.getNotesAttribuees();
        if (notes == null || notes.isEmpty()) {
            return layout.evaluationNotesTable(List.of());
        }

        List<String[]> rows = new ArrayList<>();
        notes.stream()
                .sorted(Comparator.comparing(n -> safeText(n.getCritereLibelle())))
                .forEach(n -> rows.add(new String[]{
                        safeText(n.getCritereLibelle()),
                        InternshipPdfEvaluationFormat.formatCriterionNote(n.getNote()),
                        InternshipPdfEvaluationFormat.formatComment(n.getCommentaire())
                }));

        return layout.evaluationNotesTable(rows);
    }

    private InternshipPdfSignatureSlot signatureSlotFromDto(FicheEvaluationDto fiche, RoleSignature role,
                                                          String roleLabel, String nomDto, String urlDto,
                                                          LocalDateTime dateDto, String profileFallback) {
        boolean signed = dateDto != null || (urlDto != null && !urlDto.isBlank());
        if (fiche.getSignatures() != null && !fiche.getSignatures().isEmpty()) {
            signed = fiche.getSignatures().stream().anyMatch(s -> role == s.getRoleSignature());
            Optional<fsegs.pfebackendemnagouuiaa.dto.SignatureDto> sigDto = fiche.getSignatures().stream()
                    .filter(s -> role == s.getRoleSignature())
                    .findFirst();
            if (sigDto.isPresent()) {
                String fromSig = signatureImagePdfHelper.firstExploitableSource(
                        sigDto.get().getUrlSignature(), urlDto, profileFallback);
                LocalDateTime at = sigDto.get().getDateSignature() != null ? sigDto.get().getDateSignature() : dateDto;
                String name = sigDto.get().getNomSignataire() != null && !sigDto.get().getNomSignataire().isBlank()
                        ? sigDto.get().getNomSignataire() : safeText(nomDto);
                return new InternshipPdfSignatureSlot(roleLabel, name, fromSig, true, at);
            }
        }
        String image = signatureImagePdfHelper.firstExploitableSource(urlDto, profileFallback);
        return new InternshipPdfSignatureSlot(roleLabel, safeText(nomDto), image, signed, dateDto);
    }

    private FicheEvaluationDto mapToDto(Stage stage, FicheEvaluation fiche) {
        FicheEvaluationDto dto = new FicheEvaluationDto();
        dto.setId(fiche.getId());
        dto.setPointFortEncadrantPro(fiche.getPointFortEncadrantPro());
        dto.setAxeAmeliorationEncadrantPro(fiche.getAxeAmeliorationEncadrantPro());
        dto.setPointFortResponsableEntreprise(fiche.getPointFortResponsableEntreprise());
        dto.setAxeAmeliorationResponsableEntreprise(fiche.getAxeAmeliorationResponsableEntreprise());
        dto.setNoteFinale(fiche.getNoteFinale());
        dto.setDonneesCompletes(fiche.donneesCompletes());
        dto.setSignaturesCompletes(fiche.estCompletementSigne());
        dto.setVerrouillee(fiche.estVerrouillee());

        fiche.getSignaturePour(RoleSignature.ENCADRANT_PROFESSIONNEL).ifPresent(sig -> {
            dto.setDateSignatureEncadrantProfessionnel(sig.getDateSignature());
            dto.setSignatureEncadrantProfessionnel(signatureImagePdfHelper.resolveImageSource(Optional.of(sig),
                    stage.getEncadrantProfessionnel() != null ? stage.getEncadrantProfessionnel().getUrlSignature() : ""));
            dto.setNomSignataireEncadrantProfessionnel(resolveSignerName(sig,
                    stage.getEncadrantProfessionnel() != null ? buildFullName(stage.getEncadrantProfessionnel()) : "—"));
        });

        fiche.getSignaturePour(RoleSignature.RESPONSABLE_ENTREPRISE).ifPresent(sig -> {
            dto.setDateSignatureRepresentantEntreprise(sig.getDateSignature());
            dto.setSignatureRepresentantEntreprise(signatureImagePdfHelper.resolveImageSource(Optional.of(sig),
                    stage.getTuteurEntreprise() != null ? stage.getTuteurEntreprise().getUrlSignature() : ""));
            dto.setNomSignataireRepresentantEntreprise(resolveSignerName(sig, buildTuteurName(stage)));
        });

        if (stage != null) {
            dto.setStageId(stage.getId());
            dto.setStageTitre(stage.getTitre());
            dto.setStageSujet(stage.getSujet());
            dto.setStageDateDebut(stage.getDateDebut());
            dto.setStageDateFin(stage.getDateFin());
            if (stage.getStagiaire() != null) {
                dto.setStagiaireNomComplet(buildFullName(stage.getStagiaire()));
                dto.setSectionStagiaire(stage.getStagiaire().getFiliere() != null
                        ? stage.getStagiaire().getFiliere().getNom() : "");
            }
            if (stage.getEntreprise() != null) {
                dto.setEntrepriseNom(stage.getEntreprise().getNom());
            }
        }

        if (fiche.getReunionFinale() != null) {
            dto.setReunionFinaleId(fiche.getReunionFinale().getId());
            dto.setReunionFinaleNumero(fiche.getReunionFinale().getNumReunion());
            dto.setReunionFinaleDate(fiche.getReunionFinale().getDate());
            dto.setReunionFinaleHeure(fiche.getReunionFinale().getHeure());
        }

        List<NoteAttribueeDto> noteDtos = new ArrayList<>();
        if (fiche.getNotesAttribuees() != null) {
            for (NoteAttribuee n : fiche.getNotesAttribuees()) {
                NoteAttribueeDto nd = new NoteAttribueeDto();
                nd.setNote(n.getNote());
                nd.setCommentaire(n.getCommentaire());
                nd.setPoids(n.getPoids());
                nd.setBareme(n.getBareme());
                if (n.getCritereEvaluation() != null) {
                    nd.setCritereLibelle(n.getCritereEvaluation().getLibelle());
                }
                noteDtos.add(nd);
            }
        }
        dto.setNotesAttribuees(noteDtos);
        return dto;
    }

    private String resolveSignerName(Signature sig, String fallback) {
        if (sig.getSignataireId() != null) {
            return utilisateurRepository.findById(sig.getSignataireId())
                    .map(this::buildFullName)
                    .orElse(fallback);
        }
        return fallback;
    }

    private String buildTuteurName(Stage stage) {
        if (stage.getTuteurEntreprise() != null) return buildFullName(stage.getTuteurEntreprise());
        return stage.getEntreprise() != null ? safeText(stage.getEntreprise().getNom()) : "—";
    }

    private String profileUrl(Utilisateur user) {
        return user != null && user.getUrlSignature() != null && !user.getUrlSignature().isBlank()
                ? user.getUrlSignature().trim() : "";
    }

    private String buildFullName(Utilisateur user) {
        String full = ((user.getPrenom() == null ? "" : user.getPrenom().trim()) + " "
                + (user.getNom() == null ? "" : user.getNom().trim())).trim();
        return full.isBlank() ? "—" : full;
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return "";
    }
}
