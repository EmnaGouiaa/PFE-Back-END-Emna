package fsegs.pfebackendemnagouuiaa.services;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Table;
import fsegs.pfebackendemnagouuiaa.entities.Absence;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.Reunion;
import fsegs.pfebackendemnagouuiaa.entities.RoleSignature;
import fsegs.pfebackendemnagouuiaa.entities.Signature;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.service.LogbookMeetingSupport;
import fsegs.pfebackendemnagouuiaa.service.LogbookTrelloSupport;
import fsegs.pfebackendemnagouuiaa.services.pdf.InternshipPdfLayout;
import fsegs.pfebackendemnagouuiaa.services.pdf.InternshipPdfSignatureSlot;
import fsegs.pfebackendemnagouuiaa.services.pdf.InternshipPdfTheme;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fsegs.pfebackendemnagouuiaa.services.pdf.InternshipPdfTheme.*;

/**
 * Cahier de stage — template PDF unifie FSEGS.
 */
@Service
@RequiredArgsConstructor
public class CahierStagePdfService {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public record TrelloSnapshot(boolean synchronizedBoard, List<Map<String, Object>> tasks, Map<String, String> listNames) {
    }

    private final SignatureImagePdfHelper signatureImagePdfHelper;
    private final UtilisateurRepository utilisateurRepository;

    public byte[] generer(Stage stage, CahierStage cahier, TrelloSnapshot trello, List<Reunion> meetings,
                          List<Absence> absences) throws IOException {
        return generer(stage, cahier, trello, meetings, absences, null);
    }

    public byte[] generer(Stage stage, CahierStage cahier, TrelloSnapshot trello, List<Reunion> meetings,
                          List<Absence> absences, FicheEvaluation evaluation) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDocument = new PdfDocument(writer);
        Document document = new Document(pdfDocument, PageSize.A4);
        document.setMargins(MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, MARGIN_LEFT);

        InternshipPdfLayout layout = new InternshipPdfLayout(document, signatureImagePdfHelper);
        boolean complete = cahier.estCompletementSigne();

        layout.addDocumentHeader(
                "Cahier de stage",
                safeText(stage.getTitre()) + " — " + buildStagiaireName(stage),
                complete ? "✓ Validé" : "⏳ En cours",
                complete,
                List.of(
                        new String[]{"Stage", safeText(stage.getTitre())},
                        new String[]{"Génération", formatDate(cahier.getDateGeneration())}
                )
        );

        document.add(layout.summaryCards(List.of(
                new String[]{"Stagiaire", buildStagiaireName(stage)},
                new String[]{"Entreprise", stage.getEntreprise() != null ? safeText(stage.getEntreprise().getNom()) : "—"},
                new String[]{"Période", formatDate(stage.getDateDebut()) + " → " + formatDate(stage.getDateFin())},
                new String[]{"Statut", complete ? "Complet" : "Incomplet"}
        )));

        layout.addSection("Informations du stage");
        document.add(layout.keyValueTable(buildStageRows(stage, cahier)));

        layout.addSection("État des signatures");
        document.add(layout.keyValueTable(buildSignatureStatusRows(cahier)));

        layout.addSection("Tâches Trello synchronisées");
        document.add(buildTrelloTable(layout, trello));

        layout.addSection("Réunions et observations");
        document.add(buildMeetingsTable(layout, meetings));

        layout.addSection("Feuille de présence / absences");
        document.add(buildAbsencesTable(layout, absences));

        layout.addEvaluationSections(evaluation);

        layout.addSection("Signatures");
        document.add(layout.signatureGrid(buildSignatureSlots(stage, cahier)));

        layout.addFooter();
        document.close();
        return outputStream.toByteArray();
    }

    private List<String[]> buildStageRows(Stage stage, CahierStage cahier) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Titre du stage", safeText(stage.getTitre())});
        rows.add(new String[]{"Sujet du stage", safeText(stage.getSujet())});
        rows.add(new String[]{"Date de début", formatDate(stage.getDateDebut())});
        rows.add(new String[]{"Date de fin", formatDate(stage.getDateFin())});
        rows.add(new String[]{"Stagiaire", buildStagiaireName(stage)});
        rows.add(new String[]{"Email stagiaire", stage.getStagiaire() != null ? safeText(stage.getStagiaire().getEmail()) : "—"});
        rows.add(new String[]{"Encadrant académique", buildEncadrantAcademiqueName(stage)});
        rows.add(new String[]{"Encadrant professionnel", buildEncadrantProfessionnelName(stage)});
        if (stage.getEntreprise() != null) {
            rows.add(new String[]{"Entreprise", safeText(stage.getEntreprise().getNom())});
            rows.add(new String[]{"Email entreprise", safeText(stage.getEntreprise().getEmail())});
            rows.add(new String[]{"Téléphone entreprise", safeText(stage.getEntreprise().getTelephone())});
            rows.add(new String[]{"Secteur d'activité", safeText(stage.getEntreprise().getSecteurActivite())});
        } else {
            rows.add(new String[]{"Entreprise", "—"});
        }
        rows.add(new String[]{"Date génération cahier", formatDate(cahier.getDateGeneration())});
        return rows;
    }

    private List<String[]> buildSignatureStatusRows(CahierStage cahier) {
        List<String[]> rows = new ArrayList<>();
        rows.add(signatureStatusRow(cahier, RoleSignature.STAGIAIRE, "Stagiaire"));
        rows.add(signatureStatusRow(cahier, RoleSignature.ENCADRANT_ACADEMIQUE, "Encadrant académique"));
        rows.add(signatureStatusRow(cahier, RoleSignature.ENCADRANT_PROFESSIONNEL, "Encadrant professionnel"));
        rows.add(signatureStatusRow(cahier, RoleSignature.RESPONSABLE_ENTREPRISE, "Responsable entreprise"));
        rows.add(new String[]{"Statut global", cahier.estCompletementSigne() ? "Complet" : "Incomplet"});
        return rows;
    }

    private String[] signatureStatusRow(CahierStage cahier, RoleSignature role, String label) {
        return cahier.getSignaturePour(role)
                .map(sig -> new String[]{label, "Signé le " + formatDateTime(sig.getDateSignature())})
                .orElse(new String[]{label, "En attente"});
    }

    private Table buildTrelloTable(InternshipPdfLayout layout, TrelloSnapshot trello) {
        if (trello == null || !trello.synchronizedBoard() || trello.tasks().isEmpty()) {
            Table empty = new Table(1).useAllAvailableWidth();
            layout.addEmptyTableMessage(empty, 1, "Aucune tâche Trello synchronisée.");
            return empty;
        }
        Map<String, List<Map<String, Object>>> columns = LogbookTrelloSupport.groupTasksByColumn(
                trello.tasks(),
                trello.listNames()
        );
        List<String[]> rows = new ArrayList<>();
        for (String column : List.of(
                LogbookTrelloSupport.COLUMN_TODO,
                LogbookTrelloSupport.COLUMN_IN_PROGRESS,
                LogbookTrelloSupport.COLUMN_DONE)) {
            for (Map<String, Object> task : columns.getOrDefault(column, List.of())) {
                String desc = String.valueOf(task.getOrDefault("desc", ""));
                rows.add(new String[]{
                        column,
                        String.valueOf(task.getOrDefault("name", "—")),
                        desc.isBlank() ? "—" : desc
                });
            }
        }
        if (rows.isEmpty()) {
            Table empty = new Table(1).useAllAvailableWidth();
            layout.addEmptyTableMessage(empty, 1, "Aucune tâche Trello synchronisée.");
            return empty;
        }
        return layout.dataTable(new String[]{"Colonne", "Tâche", "Description"}, rows, true);
    }

    private Table buildMeetingsTable(InternshipPdfLayout layout, List<Reunion> meetings) {
        if (meetings == null || meetings.isEmpty()) {
            Table empty = new Table(1).useAllAvailableWidth();
            layout.addEmptyTableMessage(empty, 1, "Aucune réunion hebdomadaire enregistrée.");
            return empty;
        }
        List<String[]> rows = new ArrayList<>();
        for (Reunion m : meetings) {
            String observation = LogbookMeetingSupport.resolveCreatorObservation(m);
            rows.add(new String[]{
                    safeText(m.getNumReunion()),
                    formatDate(m.getDate()),
                    m.getHeure() != null ? m.getHeure().toString() : "—",
                    observation.isBlank() ? "—" : observation
            });
        }
        return layout.dataTable(new String[]{"N° réunion", "Date", "Heure", "Observation encadrant"}, rows, true);
    }

    private Table buildAbsencesTable(InternshipPdfLayout layout, List<Absence> absences) {
        if (absences == null || absences.isEmpty()) {
            Table empty = new Table(1).useAllAvailableWidth();
            layout.addEmptyTableMessage(empty, 1, "Aucune absence enregistrée.");
            return empty;
        }
        List<String[]> rows = new ArrayList<>();
        for (Absence a : absences) {
            rows.add(new String[]{
                    formatDate(a.getDateAbsence()),
                    a.getNbAbsence() != null ? String.valueOf(a.getNbAbsence()) : "—",
                    safeText(a.getJustification()),
                    safeText(a.getStatut()),
                    safeText(a.getCommentaire())
            });
        }
        return layout.dataTable(new String[]{"Date", "Nombre", "Justification", "Statut", "Commentaire"}, rows, true);
    }

    private List<InternshipPdfSignatureSlot> buildSignatureSlots(Stage stage, CahierStage cahier) {
        return List.of(
                slot(cahier, stage, RoleSignature.STAGIAIRE, "Stagiaire", buildStagiaireName(stage), profileUrl(stage.getStagiaire())),
                slot(cahier, stage, RoleSignature.ENCADRANT_ACADEMIQUE, "Encadrant académique", buildEncadrantAcademiqueName(stage), profileUrl(stage.getEncadrantAcademique())),
                slot(cahier, stage, RoleSignature.ENCADRANT_PROFESSIONNEL, "Encadrant professionnel", buildEncadrantProfessionnelName(stage), profileUrl(stage.getEncadrantProfessionnel())),
                slot(cahier, stage, RoleSignature.RESPONSABLE_ENTREPRISE, "Représentant entreprise", buildTuteurName(stage), profileUrl(stage.getTuteurEntreprise()))
        );
    }

    private InternshipPdfSignatureSlot slot(CahierStage cahier, Stage stage, RoleSignature role, String label,
                                            String fallback, String profileUrl) {
        Optional<Signature> sig = cahier.getSignaturePour(role);
        boolean signed = sig.isPresent();
        String name = signed ? resolveSignerName(sig, fallback) : fallback;
        return new InternshipPdfSignatureSlot(label, name, resolveImageSource(sig, profileUrl), signed,
                sig.map(Signature::getDateSignature).orElse(null));
    }

    private String resolveImageSource(Optional<Signature> sig, String profileFallback) {
        return signatureImagePdfHelper.resolveImageSource(sig, profileFallback);
    }

    private String resolveSignerName(Optional<Signature> sig, String fallback) {
        return sig.flatMap(s -> s.getSignataireId() != null
                        ? utilisateurRepository.findById(s.getSignataireId()).map(this::buildFullName)
                        : Optional.empty())
                .orElse(fallback);
    }

    private String profileUrl(Utilisateur user) {
        return user != null && user.getUrlSignature() != null && !user.getUrlSignature().isBlank()
                ? user.getUrlSignature().trim() : "";
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

    private String buildTuteurName(Stage stage) {
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

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "—" : dateTime.format(DATE_TIME_FMT);
    }
}
