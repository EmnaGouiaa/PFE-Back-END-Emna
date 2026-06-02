package fsegs.pfebackendemnagouuiaa.services;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.RoleSignature;
import fsegs.pfebackendemnagouuiaa.entities.Signature;
import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
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
import java.util.List;
import java.util.Optional;

import static fsegs.pfebackendemnagouuiaa.services.pdf.InternshipPdfTheme.*;

/**
 * Convention de stage — template PDF unifie FSEGS.
 */
@Service
@RequiredArgsConstructor
public class ConventionStagePdfService {

    private final SignatureImagePdfHelper signatureImagePdfHelper;
    private final UtilisateurRepository utilisateurRepository;

    public byte[] generer(Stage stage, ConventionStage convention) throws IOException {
        return generer(stage, convention, null);
    }

    public byte[] generer(Stage stage, ConventionStage convention, FicheEvaluation evaluation) throws IOException {
        initializeAssociations(stage, convention);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDocument = new PdfDocument(writer);
        Document document = new Document(pdfDocument, PageSize.A4);
        document.setMargins(MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, MARGIN_LEFT);

        InternshipPdfLayout layout = new InternshipPdfLayout(document, signatureImagePdfHelper);

        LocalDate dateDebut = convention.getDateDebut() != null ? convention.getDateDebut() : stage.getDateDebut();
        String numConv = convention.getNumConv() != null ? String.valueOf(convention.getNumConv()) : String.valueOf(convention.getId());
        boolean complete = convention.estCompletementSigne();

        layout.addDocumentHeader(
                "Convention de stage",
                "N° " + numConv + " — " + safeText(stage.getTitre()),
                complete ? "✓ Toutes signatures" : "⏳ En cours de signature",
                complete,
                List.of(
                        new String[]{"Année univ.", computeAnneeUniversitaire(dateDebut)},
                        new String[]{"Génération", LocalDate.now().format(DATE_FMT)}
                )
        );

        layout.addSection("Références");
        document.add(layout.keyValueTable(buildConventionReferenceRows(stage, convention)));

        layout.addSection("Stagiaire");
        document.add(layout.keyValueTable(buildStudentRows(stage)));

        layout.addSection("Entreprise d'accueil");
        document.add(layout.keyValueTable(buildCompanyRows(stage)));

        layout.addSection("Stage et mission");
        document.add(layout.keyValueTable(buildInternshipRows(stage, convention)));

        layout.addSection("Encadrement");
        document.add(layout.keyValueTable(buildSupervisionRows(stage)));

        layout.addSection("Établissement / formation");
        document.add(layout.keyValueTable(buildUniversityRows(stage)));

        layout.addEvaluationSections(evaluation);

        layout.addSection("Signatures des parties");
        document.add(layout.signatureGrid(buildSignatureSlots(stage, convention)));

        layout.addFooter();
        document.close();
        return outputStream.toByteArray();
    }

    public void initializeAssociations(Stage stage, ConventionStage convention) {
        if (stage == null) return;
        safeText(stage.getTitre());
        safeText(stage.getSujet());
        if (stage.getStatut() != null) stage.getStatut().name();
        if (stage.getStatutSujet() != null) stage.getStatutSujet().name();
        Stagiaire stagiaire = stage.getStagiaire();
        if (stagiaire != null) {
            stagiaire.getEmail();
            stagiaire.getTelephone();
            stagiaire.getMatricule();
            if (stagiaire.getFiliere() != null) stagiaire.getFiliere().getNom();
            stagiaire.getUrlSignature();
        }
        Entreprise entreprise = stage.getEntreprise();
        if (entreprise != null) {
            entreprise.getNom();
            entreprise.getAdresse();
        }
        if (stage.getEncadrantAcademique() != null) stage.getEncadrantAcademique().getUrlSignature();
        if (stage.getEncadrantProfessionnel() != null) stage.getEncadrantProfessionnel().getUrlSignature();
        if (stage.getTuteurEntreprise() != null) stage.getTuteurEntreprise().getUrlSignature();
        if (convention != null) {
            if (convention.getDemandeStage() != null) convention.getDemandeStage().getId();
            convention.getSignatures().forEach(sig -> {
                sig.getUrlSignature();
                if (sig.getSignataireId() != null) {
                    utilisateurRepository.findById(sig.getSignataireId()).ifPresent(u -> u.getUrlSignature());
                }
            });
        }
    }

    private List<InternshipPdfSignatureSlot> buildSignatureSlots(Stage stage, ConventionStage convention) {
        return List.of(
                slot(convention, RoleSignature.STAGIAIRE, "Stagiaire", buildStagiaireName(stage), profileUrl(stage.getStagiaire())),
                slot(convention, RoleSignature.ENCADRANT_ACADEMIQUE, "Encadrant académique", buildEncadrantAcademiqueName(stage), profileUrl(stage.getEncadrantAcademique())),
                slot(convention, RoleSignature.ENCADRANT_PROFESSIONNEL, "Encadrant professionnel", buildEncadrantProfessionnelName(stage), profileUrl(stage.getEncadrantProfessionnel())),
                slot(convention, RoleSignature.RESPONSABLE_ENTREPRISE, "Représentant entreprise", buildTuteurEntrepriseName(stage), profileUrl(stage.getTuteurEntreprise())),
                slot(convention, RoleSignature.RESPONSABLE_UNIVERSITAIRE, "Responsable des stages",
                        resolveSignatoryName(convention, RoleSignature.RESPONSABLE_UNIVERSITAIRE, "Responsable des stages"), "")
        );
    }

    private InternshipPdfSignatureSlot slot(ConventionStage convention, RoleSignature role, String label,
                                            String fallbackName, String profileUrl) {
        Optional<Signature> sig = convention.getSignaturePour(role);
        boolean signed = sig.isPresent();
        String name = signed ? resolveSignatoryName(convention, role, fallbackName) : fallbackName;
        String image = resolveImageSource(sig, profileUrl);
        LocalDateTime at = sig.map(Signature::getDateSignature).orElse(null);
        return new InternshipPdfSignatureSlot(label, name, image, signed, at);
    }

    private String resolveImageSource(Optional<Signature> sig, String profileFallback) {
        return signatureImagePdfHelper.resolveImageSource(sig, profileFallback);
    }

    private String resolveSignatoryName(ConventionStage convention, RoleSignature role, String fallback) {
        return convention.getSignaturePour(role)
                .flatMap(sig -> sig.getSignataireId() != null
                        ? utilisateurRepository.findById(sig.getSignataireId()).map(this::buildFullName)
                        : Optional.empty())
                .filter(n -> !n.isBlank() && !"—".equals(n))
                .orElse(fallback);
    }

    private String profileUrl(Utilisateur user) {
        return user != null && user.getUrlSignature() != null && !user.getUrlSignature().isBlank()
                ? user.getUrlSignature().trim() : "";
    }

    private List<String[]> buildConventionReferenceRows(Stage stage, ConventionStage convention) {
        LocalDate dateDebut = convention.getDateDebut() != null ? convention.getDateDebut() : stage.getDateDebut();
        LocalDate dateFin = convention.getDateFin() != null ? convention.getDateFin() : stage.getDateFin();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Numéro de convention", convention.getNumConv() != null ? String.valueOf(convention.getNumConv()) : String.valueOf(convention.getId())});
        rows.add(new String[]{"Identifiant interne", String.valueOf(convention.getId())});
        rows.add(new String[]{"Identifiant stage", stage.getId() != null ? String.valueOf(stage.getId()) : "—"});
        rows.add(new String[]{"Année universitaire", computeAnneeUniversitaire(dateDebut)});
        rows.add(new String[]{"Période", formatDate(dateDebut) + " → " + formatDate(dateFin)});
        rows.add(new String[]{"État des signatures", convention.estCompletementSigne() ? "Complètes" : "En cours"});
        return rows;
    }

    private List<String[]> buildStudentRows(Stage stage) {
        Stagiaire s = stage.getStagiaire();
        return List.of(
                new String[]{"Nom complet", buildStagiaireName(stage)},
                new String[]{"Email", s != null ? safeText(s.getEmail()) : "—"},
                new String[]{"Téléphone", s != null ? safeText(s.getTelephone()) : "—"},
                new String[]{"Matricule", s != null ? safeText(s.getMatricule()) : "—"},
                new String[]{"Adresse", s != null ? safeText(s.getAdresse()) : "—"},
                new String[]{"Filière / niveau", buildStudentTrack(stage)}
        );
    }

    private List<String[]> buildCompanyRows(Stage stage) {
        Entreprise e = stage.getEntreprise();
        return List.of(
                new String[]{"Raison sociale", e != null ? safeText(e.getNom()) : "—"},
                new String[]{"Adresse", e != null ? safeText(e.getAdresse()) : "—"},
                new String[]{"Email", e != null ? safeText(e.getEmail()) : "—"},
                new String[]{"Téléphone", e != null ? safeText(e.getTelephone()) : "—"},
                new String[]{"Secteur", e != null ? safeText(e.getSecteurActivite()) : "—"},
                new String[]{"Représentant", buildTuteurEntrepriseName(stage)}
        );
    }

    private List<String[]> buildInternshipRows(Stage stage, ConventionStage convention) {
        LocalDate dateDebut = convention.getDateDebut() != null ? convention.getDateDebut() : stage.getDateDebut();
        LocalDate dateFin = convention.getDateFin() != null ? convention.getDateFin() : stage.getDateFin();
        return List.of(
                new String[]{"Titre", safeText(stage.getTitre())},
                new String[]{"Sujet / mission", safeText(stage.getSujet())},
                new String[]{"Date de début", formatDate(dateDebut)},
                new String[]{"Date de fin", formatDate(dateFin)},
                new String[]{"Durée", buildDuration(stage)},
                new String[]{"Semaines", stage.getNbSemaine() != null ? String.valueOf(stage.getNbSemaine()) : "—"},
                new String[]{"Statut stage", stage.getStatut() != null ? stage.getStatut().name() : "—"},
                new String[]{"Statut sujet", stage.getStatutSujet() != null ? stage.getStatutSujet().name() : "—"}
        );
    }

    private List<String[]> buildSupervisionRows(Stage stage) {
        return List.of(
                new String[]{"Encadrant académique", buildEncadrantAcademiqueName(stage)},
                new String[]{"Email EA", stage.getEncadrantAcademique() != null ? safeText(stage.getEncadrantAcademique().getEmail()) : "—"},
                new String[]{"Encadrant professionnel", buildEncadrantProfessionnelName(stage)},
                new String[]{"Email EP", stage.getEncadrantProfessionnel() != null ? safeText(stage.getEncadrantProfessionnel().getEmail()) : "—"}
        );
    }

    private List<String[]> buildUniversityRows(Stage stage) {
        Stagiaire s = stage.getStagiaire();
        String filiere = s != null && s.getFiliere() != null ? safeText(s.getFiliere().getNom()) : "—";
        return List.of(
                new String[]{"Établissement", InternshipPdfTheme.INSTITUTION},
                new String[]{"Filière", filiere}
        );
    }

    private String computeAnneeUniversitaire(LocalDate dateDebut) {
        if (dateDebut == null) return "—";
        int year = dateDebut.getYear();
        return dateDebut.getMonthValue() >= 9 ? year + "/" + (year + 1) : (year - 1) + "/" + year;
    }

    private String buildDuration(Stage stage) {
        if (stage.getDuree() != null) return stage.getDuree() + " semaine(s)";
        if (stage.getNbSemaine() != null) return stage.getNbSemaine() + " semaine(s)";
        return "—";
    }

    private String buildStudentTrack(Stage stage) {
        Stagiaire s = stage.getStagiaire();
        if (s == null) return "—";
        String f = s.getFiliere() != null ? safeText(s.getFiliere().getNom()) : "—";
        String n = s.getNiveau() != null ? "Niveau " + s.getNiveau() : "—";
        if ("—".equals(f)) return n;
        if ("—".equals(n)) return f;
        return f + " / " + n;
    }

    private String buildStagiaireName(Stage stage) {
        return stage.getStagiaire() == null ? "—" : buildFullName(stage.getStagiaire());
    }

    private String buildEncadrantAcademiqueName(Stage stage) {
        return stage.getEncadrantAcademique() == null ? "—" : buildFullName(stage.getEncadrantAcademique());
    }

    private String buildEncadrantProfessionnelName(Stage stage) {
        return stage.getEncadrantProfessionnel() == null ? "—" : buildFullName(stage.getEncadrantProfessionnel());
    }

    private String buildTuteurEntrepriseName(Stage stage) {
        if (stage.getTuteurEntreprise() != null) return buildFullName(stage.getTuteurEntreprise());
        return stage.getEntreprise() != null ? safeText(stage.getEntreprise().getNom()) : "—";
    }

    private String buildFullName(Utilisateur user) {
        String full = ((user.getPrenom() == null ? "" : user.getPrenom().trim()) + " "
                + (user.getNom() == null ? "" : user.getNom().trim())).trim();
        return full.isBlank() ? "—" : full;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FMT);
    }
}
