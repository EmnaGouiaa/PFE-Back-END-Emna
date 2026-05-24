package fsegs.pfebackendemnagouuiaa.services;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.DashedBorder;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import fsegs.pfebackendemnagouuiaa.dto.FicheEvaluationDto;
import fsegs.pfebackendemnagouuiaa.dto.NoteAttribueeDto;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FicheEvaluationPdfService {

    // Palette
    private static final DeviceRgb BLUE_DARK  = new DeviceRgb(26,  58,  110);
    private static final DeviceRgb BLUE_MID   = new DeviceRgb(41,  128, 185);
    private static final DeviceRgb BLUE_LIGHT = new DeviceRgb(189, 215, 238);
    private static final DeviceRgb BLUE_BG    = new DeviceRgb(235, 245, 255);
    private static final DeviceRgb YELLOW     = new DeviceRgb(243, 156,  18);
    private static final DeviceRgb GRAY_TEXT  = new DeviceRgb( 44,  62,  80);
    private static final DeviceRgb GRAY_BG    = new DeviceRgb(245, 245, 245);
    private static final DeviceRgb GREEN      = new DeviceRgb( 39, 174,  96);
    private static final DeviceRgb WHITE      = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb ROW_ALT    = new DeviceRgb(241, 248, 255);

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generer(FicheEvaluationDto fiche) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter   writer = new PdfWriter(baos);
        PdfDocument pdf    = new PdfDocument(writer);
        Document    doc    = new Document(pdf, PageSize.A4);
        doc.setMargins(28, 36, 28, 36);

        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont italic  = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

        ajouterHeader(doc, fiche, bold, regular);
        ajouterInfoStagiaire(doc, fiche, bold, regular);
        ajouterBlocsEvaluateurs(doc, fiche, bold, regular, italic);
        ajouterTableauNotes(doc, fiche, bold, regular, italic);
        ajouterInfoStage(doc, fiche, bold, regular);
        ajouterEchelleEtSignatures(doc, fiche, bold, regular, italic);

        doc.close();
        return baos.toByteArray();
    }

    // ─── HEADER ──────────────────────────────────────────────────────────────

    private void ajouterHeader(Document doc, FicheEvaluationDto fiche,
                               PdfFont bold, PdfFont regular) {
        Table top = new Table(UnitValue.createPercentArray(new float[]{68, 32}))
                .useAllAvailableWidth();

        // Left: institution + big title
        Cell left = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setPaddingRight(8);
        left.add(new Paragraph("Universite / FSEGS")
                .setFont(regular).setFontSize(7.5f).setFontColor(BLUE_MID).setMarginBottom(3));
        left.add(new Paragraph("FICHE D'EVALUATION DE STAGE")
                .setFont(bold).setFontSize(17f).setFontColor(BLUE_DARK).setMarginBottom(0));
        left.add(new Paragraph("Projet de Fin d'Etudes (PFE)")
                .setFont(regular).setFontSize(8.5f).setFontColor(BLUE_MID).setMarginTop(2));
        top.addCell(left);

        // Right: reference box
        String dateFin = fiche.getStageDateFin() != null
                ? fiche.getStageDateFin().format(DATE_FMT) : "-";
        Cell right = new Cell()
                .setBorder(new SolidBorder(BLUE_DARK, 1.5f))
                .setBackgroundColor(BLUE_BG)
                .setPadding(8)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        right.add(refLine("Ref     :", "FE-PFE-001", bold, regular));
        right.add(refLine("Version :", "1.0",        bold, regular));
        right.add(refLine("Date    :", dateFin,      bold, regular));
        top.addCell(right);

        doc.add(top);

        // Decorative bar: blue + yellow
        Table bar = new Table(UnitValue.createPercentArray(new float[]{85, 15}))
                .useAllAvailableWidth().setMarginTop(6).setMarginBottom(14);
        bar.addCell(new Cell().setHeight(4).setBackgroundColor(BLUE_DARK).setBorder(Border.NO_BORDER));
        bar.addCell(new Cell().setHeight(4).setBackgroundColor(YELLOW).setBorder(Border.NO_BORDER));
        doc.add(bar);
    }

    private Paragraph refLine(String label, String value, PdfFont bold, PdfFont regular) {
        return new Paragraph()
                .add(new Text(label + " ").setFont(bold).setFontColor(BLUE_DARK))
                .add(new Text(value).setFont(regular).setFontColor(GRAY_TEXT))
                .setFontSize(7.5f).setMarginBottom(2);
    }

    // ─── INFORMATIONS STAGIAIRE ───────────────────────────────────────────────

    private void ajouterInfoStagiaire(Document doc, FicheEvaluationDto fiche,
                                      PdfFont bold, PdfFont regular) {
        doc.add(bandeau("INFORMATIONS STAGIAIRE", bold));

        Table t = new Table(UnitValue.createPercentArray(new float[]{38, 62}))
                .useAllAvailableWidth().setMarginBottom(12);

        String[][] rows = {
            {"Nom et prenom :",       s(fiche.getStagiaireNomComplet())},
            {"Section :",             s(fiche.getSectionStagiaire())},
            {"Entreprise / Lieu :",   s(fiche.getEntrepriseNom())},
        };
        for (String[] row : rows) {
            t.addCell(dotCell(new Paragraph(row[0]).setFont(bold).setFontSize(8.5f).setFontColor(BLUE_DARK)));
            t.addCell(dotCell(new Paragraph(row[1]).setFont(regular).setFontSize(8.5f).setFontColor(GRAY_TEXT)));
        }
        doc.add(t);
    }

    // ─── BLOCS EVALUATEURS (côte à côte) ─────────────────────────────────────

    private void ajouterBlocsEvaluateurs(Document doc, FicheEvaluationDto fiche,
                                         PdfFont bold, PdfFont regular, PdfFont italic) {
        doc.add(bandeau("EVALUATION PAR LES ENCADRANTS", bold));

        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth().setMarginBottom(12);

        table.addCell(buildBlocEvaluateur(
                "Encadrant Professionnel", BLUE_DARK,
                fiche.getPointFortEncadrantPro(),
                fiche.getAxeAmeliorationEncadrantPro(),
                fiche.getNomSignataireEncadrantProfessionnel(),
                "Encadrant Professionnel",
                fiche.getSignatureEncadrantProfessionnel(),
                fiche.getDateSignatureEncadrantProfessionnel(),
                bold, regular, italic));

        table.addCell(buildBlocEvaluateur(
                "Responsable Entreprise", BLUE_MID,
                fiche.getPointFortResponsableEntreprise(),
                fiche.getAxeAmeliorationResponsableEntreprise(),
                fiche.getNomSignataireRepresentantEntreprise(),
                "Responsable Entreprise",
                fiche.getSignatureRepresentantEntreprise(),
                fiche.getDateSignatureRepresentantEntreprise(),
                bold, regular, italic));

        doc.add(table);
    }

    private Cell buildBlocEvaluateur(String titre, DeviceRgb couleurTitre,
                                      String pointsForts, String axesAmelioration,
                                      String nomSignataire, String role,
                                      String signature, LocalDateTime dateSig,
                                      PdfFont bold, PdfFont regular, PdfFont italic) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(BLUE_LIGHT, 1f))
                .setPadding(10);

        // Sous-titre coloré
        cell.add(new Paragraph(titre)
                .setFont(bold).setFontSize(9.5f).setFontColor(WHITE)
                .setBackgroundColor(couleurTitre)
                .setPadding(5).setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(8));

        // Points forts
        cell.add(new Paragraph("Points forts :")
                .setFont(bold).setFontSize(8f).setFontColor(BLUE_DARK).setMarginBottom(2));
        cell.add(new Paragraph(s(pointsForts))
                .setFont(regular).setFontSize(8f).setFontColor(GRAY_TEXT)
                .setBackgroundColor(BLUE_BG)
                .setBorder(new SolidBorder(BLUE_LIGHT, 0.5f))
                .setPadding(5).setMarginBottom(8));

        // Axes d'amélioration
        cell.add(new Paragraph("Axes d'amelioration :")
                .setFont(bold).setFontSize(8f).setFontColor(BLUE_DARK).setMarginBottom(2));
        cell.add(new Paragraph(s(axesAmelioration))
                .setFont(regular).setFontSize(8f).setFontColor(GRAY_TEXT)
                .setBackgroundColor(BLUE_BG)
                .setBorder(new SolidBorder(BLUE_LIGHT, 0.5f))
                .setPadding(5).setMarginBottom(8));

        // Signataire
        cell.add(new Paragraph()
                .add(new Text("Nom : ").setFont(bold).setFontColor(BLUE_DARK))
                .add(new Text(s(nomSignataire)).setFont(regular).setFontColor(GRAY_TEXT))
                .setFontSize(8f).setMarginBottom(2));
        cell.add(new Paragraph()
                .add(new Text("Role : ").setFont(bold).setFontColor(BLUE_DARK))
                .add(new Text(role).setFont(italic).setFontColor(BLUE_MID))
                .setFontSize(8f).setMarginBottom(6));

        // Zone signature
        cell.add(new Paragraph("Signature :").setFont(bold).setFontSize(8f)
                .setFontColor(BLUE_DARK).setMarginBottom(3));

        if (signature != null && !signature.isEmpty()) {
            cell.add(new Paragraph("[Signe electroniquement]")
                    .setFont(bold).setFontSize(8f).setFontColor(GREEN)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
            if (dateSig != null) {
                cell.add(new Paragraph("Date : " + dateSig.format(DATETIME_FMT))
                        .setFont(italic).setFontSize(7.5f).setFontColor(GRAY_TEXT)
                        .setTextAlignment(TextAlignment.CENTER));
            }
        } else {
            Table sigBox = new Table(UnitValue.createPercentArray(new float[]{100}))
                    .useAllAvailableWidth();
            sigBox.addCell(new Cell().setHeight(30)
                    .setBorder(new DashedBorder(BLUE_MID, 0.8f))
                    .setBackgroundColor(GRAY_BG));
            cell.add(sigBox);
            cell.add(new Paragraph("En attente de signature")
                    .setFont(italic).setFontSize(7.5f).setFontColor(BLUE_MID)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(3));
        }

        return cell;
    }

    // ─── TABLEAU DES NOTES ────────────────────────────────────────────────────

    private void ajouterTableauNotes(Document doc, FicheEvaluationDto fiche,
                                     PdfFont bold, PdfFont regular, PdfFont italic) {
        doc.add(bandeau("NOTES ATTRIBUEES", bold));

        List<NoteAttribueeDto> notes = fiche.getNotesAttribuees();

        // En-têtes colonnes
        float[] cols = {38f, 14f, 13f, 14f, 21f};
        Table table = new Table(UnitValue.createPercentArray(cols))
                .useAllAvailableWidth().setMarginBottom(6);

        String[] headers = {"Critere evalue", "Note /5", "Bareme", "Coefficient", "Score pondere"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(8f).setFontColor(WHITE))
                    .setBackgroundColor(BLUE_DARK)
                    .setBorder(new SolidBorder(WHITE, 0.5f))
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE));
        }

        if (notes != null && !notes.isEmpty()) {
            boolean alt = false;
            for (NoteAttribueeDto n : notes) {
                DeviceRgb bg = alt ? ROW_ALT : WHITE;
                String noteStr   = n.getNote()        != null ? String.valueOf(n.getNote())              : "-";
                String barStr    = n.getBareme()       != null ? String.valueOf(n.getBareme())             : "5";
                String poidsStr  = n.getPoids()        != null ? String.valueOf(n.getPoids())              : "-";
                String scoreStr  = n.getScorePondere() != null ? String.format("%.2f", n.getScorePondere()) : "-";
                String libelle   = n.getCritereLibelle() != null ? n.getCritereLibelle() : "-";

                table.addCell(noteCell(libelle,  regular, 8f, GRAY_TEXT,  TextAlignment.LEFT,   bg));
                table.addCell(noteCell(noteStr,  bold,    8f, BLUE_DARK,  TextAlignment.CENTER,  bg));
                table.addCell(noteCell(barStr,   regular, 8f, GRAY_TEXT,  TextAlignment.CENTER,  bg));
                table.addCell(noteCell(poidsStr, regular, 8f, GRAY_TEXT,  TextAlignment.CENTER,  bg));
                table.addCell(noteCell(scoreStr, bold,    8f, BLUE_MID,   TextAlignment.CENTER,  bg));
                alt = !alt;
            }
        } else {
            table.addCell(new Cell(1, 5)
                    .add(new Paragraph("Aucune note attribuee")
                            .setFont(italic).setFontSize(8.5f).setFontColor(GRAY_TEXT))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(10)
                    .setBorder(new SolidBorder(BLUE_LIGHT, 0.5f)));
        }
        doc.add(table);

        // Note finale encadrée à droite
        Table noteFinaleRow = new Table(UnitValue.createPercentArray(new float[]{52, 48}))
                .useAllAvailableWidth().setMarginBottom(14);
        noteFinaleRow.addCell(new Cell().setBorder(Border.NO_BORDER));

        String noteStr = fiche.getNoteFinale() != null
                ? String.format("%.2f / 20", fiche.getNoteFinale()) : "- / 20";

        Cell noteFinaleCell = new Cell()
                .setBackgroundColor(BLUE_DARK)
                .setBorder(new SolidBorder(BLUE_DARK, 1.5f))
                .setPadding(10)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        noteFinaleCell.add(new Paragraph("NOTE FINALE")
                .setFont(bold).setFontSize(9f).setFontColor(WHITE)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
        noteFinaleCell.add(new Paragraph("(Somme des scores ponderes)")
                .setFont(regular).setFontSize(7f).setFontColor(BLUE_LIGHT)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));
        noteFinaleCell.add(new Paragraph(noteStr)
                .setFont(bold).setFontSize(16f).setFontColor(YELLOW)
                .setTextAlignment(TextAlignment.CENTER));
        noteFinaleRow.addCell(noteFinaleCell);

        doc.add(noteFinaleRow);
    }

    private Cell noteCell(String text, PdfFont font, float size, DeviceRgb color,
                          TextAlignment align, DeviceRgb bg) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(size).setFontColor(color))
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(BLUE_LIGHT, 0.5f))
                .setPadding(5)
                .setTextAlignment(align)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    // ─── INFORMATIONS STAGE ───────────────────────────────────────────────────

    private void ajouterInfoStage(Document doc, FicheEvaluationDto fiche,
                                  PdfFont bold, PdfFont regular) {
        doc.add(bandeau("INFORMATIONS LIEES AU STAGE", bold));

        Table t = new Table(UnitValue.createPercentArray(new float[]{28, 72}))
                .useAllAvailableWidth().setMarginBottom(12);

        String debut  = fiche.getStageDateDebut() != null ? fiche.getStageDateDebut().format(DATE_FMT) : "-";
        String fin    = fiche.getStageDateFin()   != null ? fiche.getStageDateFin().format(DATE_FMT)   : "-";
        String reunNum  = fiche.getReunionFinaleNumero() != null ? fiche.getReunionFinaleNumero() : "-";
        String reunDate = fiche.getReunionFinaleDate()   != null ? fiche.getReunionFinaleDate().format(DATE_FMT) : "-";
        String reunHeure = fiche.getReunionFinaleHeure() != null ? fiche.getReunionFinaleHeure().toString() : "";

        String[][] rows = {
            {"Stage :",        s(fiche.getStageTitre() != null ? fiche.getStageTitre() : fiche.getStageSujet())},
            {"Periode :",      debut + "  ->  " + fin},
            {"Reunion finale :", reunNum + (fiche.getReunionFinaleDate() != null
                    ? "  -  " + reunDate + (reunHeure.isEmpty() ? "" : "  " + reunHeure) : "")},
        };
        for (String[] row : rows) {
            t.addCell(dotCell(new Paragraph(row[0]).setFont(bold).setFontSize(8.5f).setFontColor(BLUE_DARK)));
            t.addCell(dotCell(new Paragraph(row[1]).setFont(regular).setFontSize(8.5f).setFontColor(GRAY_TEXT)));
        }
        doc.add(t);
    }

    // ─── ECHELLE + SIGNATURES FINALES ─────────────────────────────────────────

    private void ajouterEchelleEtSignatures(Document doc, FicheEvaluationDto fiche,
                                             PdfFont bold, PdfFont regular, PdfFont italic) {
        // Echelle de notation (à gauche)
        Table echelleRow = new Table(UnitValue.createPercentArray(new float[]{42, 58}))
                .useAllAvailableWidth().setMarginBottom(12);

        Cell echelleCell = new Cell()
                .setBorder(new SolidBorder(BLUE_MID, 1f))
                .setBackgroundColor(BLUE_BG)
                .setPadding(10);
        echelleCell.add(new Paragraph("ECHELLE DE NOTATION")
                .setFont(bold).setFontSize(9f).setFontColor(BLUE_DARK)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(7));

        String[][] echelle = {
            {"1", "Tres insuffisant"},
            {"2", "Insuffisant"},
            {"3", "Satisfaisant"},
            {"4", "Bien"},
            {"5", "Excellent"},
        };
        for (String[] e : echelle) {
            echelleCell.add(new Paragraph()
                    .add(new Text(e[0] + " ").setFont(bold).setFontColor(BLUE_DARK))
                    .add(new Text("= " + e[1]).setFont(regular).setFontColor(GRAY_TEXT))
                    .setFontSize(8.5f).setMarginBottom(2));
        }
        echelleRow.addCell(echelleCell);
        echelleRow.addCell(new Cell().setBorder(Border.NO_BORDER));
        doc.add(echelleRow);

        // Signatures finales
        doc.add(bandeau("SIGNATURES FINALES", bold));

        Table sigTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth().setMarginBottom(8);

        sigTable.addCell(buildBlocSignatureFinale(
                "Signature Encadrant Professionnel", BLUE_DARK,
                fiche.getSignatureEncadrantProfessionnel(),
                fiche.getNomSignataireEncadrantProfessionnel(),
                fiche.getDateSignatureEncadrantProfessionnel(),
                bold, regular, italic));

        sigTable.addCell(buildBlocSignatureFinale(
                "Signature Responsable Entreprise", BLUE_MID,
                fiche.getSignatureRepresentantEntreprise(),
                fiche.getNomSignataireRepresentantEntreprise(),
                fiche.getDateSignatureRepresentantEntreprise(),
                bold, regular, italic));

        doc.add(sigTable);

        // Notice verrouillage
        boolean locked = Boolean.TRUE.equals(fiche.getVerrouillee());
        String noticeText = locked
                ? "[Fiche verrouillee] - Les deux signatures sont completes."
                : "La fiche est verrouillee lorsque les signatures sont completes.";
        DeviceRgb noticeColor = locked ? GREEN : BLUE_MID;

        doc.add(new Paragraph(noticeText)
                .setFont(locked ? bold : italic)
                .setFontSize(8f)
                .setFontColor(noticeColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(4));
    }

    private Cell buildBlocSignatureFinale(String titre, DeviceRgb couleur,
                                          String signature, String nomSignataire,
                                          LocalDateTime dateSig,
                                          PdfFont bold, PdfFont regular, PdfFont italic) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(BLUE_LIGHT, 1f))
                .setPadding(10);

        cell.add(new Paragraph(titre)
                .setFont(bold).setFontSize(9f).setFontColor(WHITE)
                .setBackgroundColor(couleur)
                .setPadding(5).setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10));

        if (signature != null && !signature.isEmpty()) {
            cell.add(new Paragraph("[Signe electroniquement]")
                    .setFont(bold).setFontSize(9f).setFontColor(GREEN)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));
            cell.add(new Paragraph("Par : " + s(nomSignataire))
                    .setFont(regular).setFontSize(8.5f).setFontColor(GRAY_TEXT)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
            if (dateSig != null) {
                cell.add(new Paragraph("Le : " + dateSig.format(DATETIME_FMT))
                        .setFont(italic).setFontSize(8f).setFontColor(GRAY_TEXT)
                        .setTextAlignment(TextAlignment.CENTER));
            }
        } else {
            Table sigBox = new Table(UnitValue.createPercentArray(new float[]{100}))
                    .useAllAvailableWidth();
            sigBox.addCell(new Cell().setHeight(45)
                    .setBorder(new DashedBorder(BLUE_MID, 0.8f))
                    .setBackgroundColor(GRAY_BG));
            cell.add(sigBox);
            cell.add(new Paragraph("En attente de signature")
                    .setFont(italic).setFontSize(8f).setFontColor(BLUE_MID)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(4));
        }

        return cell;
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private Table bandeau(String text, PdfFont bold) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{100}))
                .useAllAvailableWidth().setMarginTop(4).setMarginBottom(8);
        t.addCell(new Cell()
                .add(new Paragraph(text).setFont(bold).setFontSize(9.5f).setFontColor(WHITE))
                .setBackgroundColor(BLUE_DARK)
                .setBorder(Border.NO_BORDER)
                .setPadding(6));
        return t;
    }

    private Cell dotCell(Paragraph content) {
        return new Cell()
                .add(content)
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new DashedBorder(BLUE_MID, 0.7f))
                .setPaddingTop(5)
                .setPaddingBottom(5);
    }

    private String s(String value) {
        return (value != null && !value.isBlank()) ? value : "-";
    }
}
