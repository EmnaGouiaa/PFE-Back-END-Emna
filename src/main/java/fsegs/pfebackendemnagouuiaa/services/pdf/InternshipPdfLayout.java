package fsegs.pfebackendemnagouuiaa.services.pdf;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.DashedBorder;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import fsegs.pfebackendemnagouuiaa.services.SignatureImagePdfHelper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static fsegs.pfebackendemnagouuiaa.services.pdf.InternshipPdfTheme.*;

/**
 * Composants visuels partages pour Convention, Cahier de stage et Fiche d'evaluation.
 */
public class InternshipPdfLayout {

    private final Document document;
    private final PdfFont regular;
    private final PdfFont bold;
    private final PdfFont italic;
    private final SignatureImagePdfHelper signatureImagePdfHelper;

    public InternshipPdfLayout(Document document, SignatureImagePdfHelper signatureImagePdfHelper) throws IOException {
        this.document = document;
        this.signatureImagePdfHelper = signatureImagePdfHelper;
        this.regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        this.bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        this.italic = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);
    }

    public PdfFont regular() {
        return regular;
    }

    public PdfFont bold() {
        return bold;
    }

    public PdfFont italic() {
        return italic;
    }

    public Document document() {
        return document;
    }

    /** En-tete document : titre, sous-titre, badge et metadonnees optionnelles a droite. */
    public void addDocumentHeader(String documentTitle, String subtitle, String badgeText, boolean badgeSuccess,
                                  List<String[]> rightMetaRows) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{3.1f, 1.4f}))
                .useAllAvailableWidth()
                .setMarginBottom(6);

        Cell left = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setPaddingRight(10);

        left.add(new Paragraph(INSTITUTION)
                .setFont(regular)
                .setFontSize(8f)
                .setFontColor(SECONDARY)
                .setMarginBottom(5));
        left.add(new Paragraph(documentTitle)
                .setFont(bold)
                .setFontSize(17)
                .setFontColor(PRIMARY)
                .setMarginBottom(4));
        if (subtitle != null && !subtitle.isBlank()) {
            left.add(new Paragraph(subtitle)
                    .setFont(regular)
                    .setFontSize(10)
                    .setFontColor(MUTED));
        }

        Cell right = new Cell()
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(0);

        if (badgeText != null && !badgeText.isBlank()) {
            Cell badge = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setBackgroundColor(badgeSuccess ? BADGE_OK_BG : BADGE_WARN_BG)
                    .setPadding(7)
                    .setTextAlignment(TextAlignment.CENTER);
            badge.add(new Paragraph(badgeText)
                    .setFont(bold)
                    .setFontSize(8.5f)
                    .setFontColor(badgeSuccess ? BADGE_OK_TEXT : BADGE_WARN_TEXT));
            Table badgeWrap = new Table(1).useAllAvailableWidth();
            badgeWrap.addCell(badge);
            right.add(badgeWrap);
        }

        if (rightMetaRows != null) {
            for (String[] row : rightMetaRows) {
                right.add(new Paragraph()
                        .add(new com.itextpdf.layout.element.Text(row[0] + ": ")
                                .setFont(bold)
                                .setFontSize(7.5f)
                                .setFontColor(SECONDARY))
                        .add(new com.itextpdf.layout.element.Text(safeText(row[1]))
                                .setFont(regular)
                                .setFontSize(8f)
                                .setFontColor(TEXT))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setMarginTop(4));
            }
        }

        header.addCell(left);
        header.addCell(right);
        document.add(header);
        addAccentBar();
    }

    /** Barre decorative primary + secondary + accent. */
    public void addAccentBar() {
        Table bar = new Table(UnitValue.createPercentArray(new float[]{72f, 20f, 8f}))
                .useAllAvailableWidth()
                .setMarginBottom(14);
        bar.addCell(new Cell().setHeight(4).setBackgroundColor(PRIMARY).setBorder(Border.NO_BORDER));
        bar.addCell(new Cell().setHeight(4).setBackgroundColor(SECONDARY).setBorder(Border.NO_BORDER));
        bar.addCell(new Cell().setHeight(4).setBackgroundColor(ACCENT).setBorder(Border.NO_BORDER));
        document.add(bar);
    }

    /** Bandeau de section (titre sur fond primary). */
    public Table sectionBandeau(String title) {
        Table bandeau = new Table(UnitValue.createPercentArray(new float[]{1f}))
                .useAllAvailableWidth()
                .setMarginTop(10)
                .setMarginBottom(6);
        bandeau.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(PRIMARY)
                .setPadding(8)
                .add(new Paragraph(title)
                        .setFont(bold)
                        .setFontSize(10)
                        .setFontColor(WHITE)));
        return bandeau;
    }

    public void addSection(String title) {
        document.add(sectionBandeau(title));
    }

    /** Tableau cle / valeur deux colonnes. */
    public Table keyValueTable(List<String[]> rows) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1.15f, 2.35f}))
                .useAllAvailableWidth()
                .setMarginBottom(4);

        for (String[] row : rows) {
            table.addCell(new Cell()
                    .setBackgroundColor(ROW_LABEL_BG)
                    .setBorder(new SolidBorder(BORDER, 0.75f))
                    .setPadding(8)
                    .add(new Paragraph(row[0])
                            .setFont(bold)
                            .setFontSize(8.5f)
                            .setFontColor(PRIMARY)));

            table.addCell(new Cell()
                    .setBackgroundColor(WHITE)
                    .setBorder(new SolidBorder(BORDER, 0.75f))
                    .setPadding(8)
                    .add(new Paragraph(safeText(row[1]))
                            .setFont(regular)
                            .setFontSize(9.5f)
                            .setFontColor(TEXT)));
        }
        return table;
    }

    /** Tableau de donnees avec en-tete primary. */
    public Table dataTable(String[] headers, List<String[]> rows, boolean alternateRows) {
        float[] widths = new float[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = 1f;
        }
        Table table = new Table(UnitValue.createPercentArray(widths))
                .useAllAvailableWidth()
                .setMarginBottom(8);

        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .setBackgroundColor(PRIMARY)
                    .setBorder(new SolidBorder(WHITE, 0.5f))
                    .setPadding(7)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .add(new Paragraph(header)
                            .setFont(bold)
                            .setFontSize(8.5f)
                            .setFontColor(WHITE)));
        }

        boolean alt = false;
        for (String[] row : rows) {
            com.itextpdf.kernel.colors.Color bg = alternateRows && alt ? ROW_ALT_BG : WHITE;
            for (String cell : row) {
                table.addCell(dataCell(cell, bg));
            }
            alt = !alt;
        }
        return table;
    }

    public void addEmptyTableMessage(Table table, int colspan, String message) {
        table.addCell(new Cell(1, colspan)
                .setBorder(new SolidBorder(BORDER, 0.75f))
                .setPadding(12)
                .setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph(message)
                        .setFont(italic)
                        .setFontSize(9f)
                        .setFontColor(MUTED)));
    }

    /** Tableau unifie des notes par critere (/5, sans score pondere). */
    public Table evaluationNotesTable(List<String[]> rows) {
        if (rows == null || rows.isEmpty()) {
            Table empty = new Table(1).useAllAvailableWidth().setMarginBottom(8);
            addEmptyTableMessage(empty, 1, "Aucune note attribuée.");
            return empty;
        }
        return dataTable(InternshipPdfEvaluationFormat.CRITERION_TABLE_HEADERS, rows, true);
    }

    /** Section synthese + tableau des criteres lorsqu'une fiche d'evaluation est disponible. */
    public void addEvaluationSections(fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation fiche) {
        if (!InternshipPdfEvaluationFormat.hasEvaluationContent(fiche)) {
            return;
        }
        addSection("Évaluation du stage");
        document.add(keyValueTable(InternshipPdfEvaluationFormat.buildSummaryRows(fiche)));
        List<String[]> criterionRows = InternshipPdfEvaluationFormat.buildCriterionRows(fiche);
        if (!criterionRows.isEmpty()) {
            addSection("Notes attribuées");
            document.add(evaluationNotesTable(criterionRows));
        }
        addHighlightBox(
                "NOTE FINALE",
                InternshipPdfEvaluationFormat.formatFinalScoreValue(fiche.getNoteFinale()),
                InternshipPdfEvaluationFormat.FINAL_SCORE_SUBTITLE
        );
    }

    private Cell dataCell(String text, com.itextpdf.kernel.colors.Color bg) {
        return new Cell()
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(BORDER, 0.5f))
                .setPadding(7)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(safeText(text))
                        .setFont(regular)
                        .setFontSize(9f)
                        .setFontColor(TEXT));
    }

    /** Grille 4 cartes resume (KPI). */
    public Table summaryCards(List<String[]> labelValuePairs) {
        int cols = Math.min(4, Math.max(1, labelValuePairs.size()));
        Table summary = new Table(UnitValue.createPercentArray(new float[cols]))
                .useAllAvailableWidth()
                .setMarginBottom(12);

        for (String[] pair : labelValuePairs) {
            summary.addCell(new Cell()
                    .setBackgroundColor(WHITE)
                    .setBorder(new SolidBorder(BORDER, 1))
                    .setPadding(10)
                    .add(new Paragraph(pair[0])
                            .setFont(regular)
                            .setFontSize(8f)
                            .setFontColor(MUTED)
                            .setMarginBottom(4))
                    .add(new Paragraph(safeText(pair[1]))
                            .setFont(bold)
                            .setFontSize(10)
                            .setFontColor(PRIMARY)));
        }
        return summary;
    }

    /** Encadre mise en avant (ex. note finale). */
    public void addHighlightBox(String label, String value, String subtitle) {
        Table box = new Table(UnitValue.createPercentArray(new float[]{1f}))
                .useAllAvailableWidth()
                .setMarginTop(6)
                .setMarginBottom(12);

        Cell cell = new Cell()
                .setBackgroundColor(PRIMARY)
                .setBorder(new SolidBorder(SECONDARY, 2))
                .setPadding(12)
                .setTextAlignment(TextAlignment.CENTER);

        cell.add(new Paragraph(label)
                .setFont(bold)
                .setFontSize(9)
                .setFontColor(WHITE)
                .setMarginBottom(4));
        if (subtitle != null && !subtitle.isBlank()) {
            cell.add(new Paragraph(subtitle)
                    .setFont(regular)
                    .setFontSize(7.5f)
                    .setFontColor(ROW_LABEL_BG)
                    .setMarginBottom(6));
        }
        cell.add(new Paragraph(value)
                .setFont(bold)
                .setFontSize(18)
                .setFontColor(ACCENT));

        box.addCell(cell);
        document.add(box);
    }

    /** Grille de signatures (2 colonnes). */
    public Table signatureGrid(List<InternshipPdfSignatureSlot> slots) {
        Table grid = new Table(UnitValue.createPercentArray(new float[]{1f, 1f}))
                .useAllAvailableWidth()
                .setMarginTop(4);

        for (InternshipPdfSignatureSlot slot : slots) {
            grid.addCell(signatureCard(slot));
        }
        return grid;
    }

    /** Tableau signatures : role, nom, statut, date, image. */
    public Table signatureTable(List<InternshipPdfSignatureSlot> slots) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1.5f, 1.8f, 0.9f, 1.2f, 1.6f}))
                .useAllAvailableWidth()
                .setMarginBottom(8);

        String[] headers = {"Signataire", "Qualité", "Statut", "Date", "Signature"};
        for (String h : headers) {
            table.addHeaderCell(headerCell(h));
        }

        for (InternshipPdfSignatureSlot slot : slots) {
            table.addCell(bodyCell(slot.fullName()));
            table.addCell(bodyCell(slot.roleLabel()));
            table.addCell(statusCell(slot.signed()));
            table.addCell(bodyCell(slot.signed() && slot.signedAt() != null
                    ? slot.signedAt().format(DATETIME_FMT) : "—"));
            table.addCell(signatureImageCell(slot));
        }
        return table;
    }

    private Cell signatureCard(InternshipPdfSignatureSlot slot) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(BORDER, 1))
                .setPadding(11)
                .setMinHeight(140)
                .setMargin(3)
                .setVerticalAlignment(VerticalAlignment.TOP);

        cell.add(new Paragraph(slot.roleLabel().toUpperCase())
                .setFont(regular)
                .setFontSize(8f)
                .setFontColor(MUTED)
                .setMarginBottom(4));
        cell.add(new Paragraph(safeText(slot.fullName()))
                .setFont(bold)
                .setFontSize(10)
                .setFontColor(TEXT)
                .setMarginBottom(8));

        Optional<Image> img = loadImage(slot.signatureImageSource());
        if (img.isPresent()) {
            cell.add(img.get().setMarginBottom(6));
        } else if (slot.signed()) {
            cell.add(signaturePlaceholder("Signature enregistrée (aperçu indisponible)"));
        } else {
            cell.add(signaturePlaceholder("En attente de signature"));
        }

        if (slot.signed() && slot.signedAt() != null) {
            cell.add(new Paragraph("Signé le " + slot.signedAt().format(DATETIME_FMT))
                    .setFont(regular)
                    .setFontSize(8f)
                    .setFontColor(SIGNED_TEXT)
                    .setTextAlignment(TextAlignment.CENTER));
        } else if (slot.signed()) {
            cell.add(new Paragraph("✓ Signé")
                    .setFont(bold)
                    .setFontSize(8.5f)
                    .setFontColor(SIGNED_TEXT)
                    .setTextAlignment(TextAlignment.CENTER));
        } else {
            cell.add(new Paragraph("Non signé")
                    .setFont(regular)
                    .setFontSize(8.5f)
                    .setFontColor(PENDING_TEXT)
                    .setTextAlignment(TextAlignment.CENTER));
        }
        return cell;
    }

    private Cell signatureImageCell(InternshipPdfSignatureSlot slot) {
        Cell cell = new Cell()
                .setBackgroundColor(WHITE)
                .setBorder(new SolidBorder(BORDER, 0.75f))
                .setPadding(6)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setMinHeight(55);

        Optional<Image> img = loadImage(slot.signatureImageSource());
        if (img.isPresent()) {
            cell.add(img.get());
        } else if (slot.signed()) {
            cell.add(new Paragraph("Image indisponible")
                    .setFont(italic)
                    .setFontSize(8f)
                    .setFontColor(MUTED));
        } else {
            cell.add(new Paragraph("—")
                    .setFont(regular)
                    .setFontSize(9f)
                    .setFontColor(MUTED));
        }
        return cell;
    }

    private Table signaturePlaceholder(String message) {
        Table ph = new Table(1).useAllAvailableWidth().setMarginBottom(6);
        Cell box = new Cell()
                .setMinHeight(50)
                .setBackgroundColor(PLACEHOLDER_BG)
                .setBorder(new DashedBorder(SECONDARY, 1))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER);
        box.add(new Paragraph(message)
                .setFont(italic)
                .setFontSize(8f)
                .setFontColor(MUTED));
        ph.addCell(box);
        return ph;
    }

    private Optional<Image> loadImage(String source) {
        return signatureImagePdfHelper.loadSignatureImage(source, SIG_IMAGE_MAX_W, SIG_IMAGE_MAX_H);
    }

    private Cell headerCell(String text) {
        return new Cell()
                .setBackgroundColor(PRIMARY)
                .setBorder(Border.NO_BORDER)
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph(text)
                        .setFont(bold)
                        .setFontSize(8.5f)
                        .setFontColor(WHITE));
    }

    private Cell bodyCell(String text) {
        return new Cell()
                .setBackgroundColor(WHITE)
                .setBorder(new SolidBorder(BORDER, 0.75f))
                .setPadding(8)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(safeText(text))
                        .setFont(regular)
                        .setFontSize(9f)
                        .setFontColor(TEXT));
    }

    private Cell statusCell(boolean signed) {
        return new Cell()
                .setBackgroundColor(signed ? BADGE_OK_BG : BADGE_WARN_BG)
                .setBorder(new SolidBorder(BORDER, 0.75f))
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(signed ? "Signé" : "En attente")
                        .setFont(bold)
                        .setFontSize(8.5f)
                        .setFontColor(signed ? SIGNED_TEXT : PENDING_TEXT));
    }

    /** Carte deux colonnes (evaluateurs, etc.). */
    public Table twoColumnCards(Cell left, Cell right) {
        Table wrapper = new Table(UnitValue.createPercentArray(new float[]{1f, 1f}))
                .useAllAvailableWidth()
                .setMarginBottom(8);
        wrapper.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).setPaddingRight(5).add(left));
        wrapper.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).setPaddingLeft(5).add(right));
        return wrapper;
    }

    public Cell evaluatorCard(String title, String pointsForts, String axesAmelioration,
                              InternshipPdfSignatureSlot signature) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(BORDER, 1))
                .setPadding(10);

        cell.add(new Paragraph(title)
                .setFont(bold)
                .setFontSize(9.5f)
                .setFontColor(WHITE)
                .setBackgroundColor(SECONDARY)
                .setPadding(6)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(8));

        cell.add(labeledTextBlock("Points forts", pointsForts));
        cell.add(labeledTextBlock("Axes d'amélioration", axesAmelioration));

        cell.add(new Paragraph("Signature")
                .setFont(bold)
                .setFontSize(8f)
                .setFontColor(PRIMARY)
                .setMarginTop(6)
                .setMarginBottom(4));

        Optional<Image> img = loadImage(signature.signatureImageSource());
        if (img.isPresent()) {
            cell.add(img.get().setMarginBottom(4));
            if (signature.signedAt() != null) {
                cell.add(new Paragraph("Le " + signature.signedAt().format(DATETIME_FMT))
                        .setFont(italic)
                        .setFontSize(7.5f)
                        .setFontColor(MUTED)
                        .setTextAlignment(TextAlignment.CENTER));
            }
        } else if (signature.signed()) {
            cell.add(signaturePlaceholder("Signature enregistrée"));
        } else {
            cell.add(signaturePlaceholder("En attente de signature"));
        }
        return cell;
    }

    private Paragraph labeledTextBlock(String label, String value) {
        return new Paragraph()
                .add(new com.itextpdf.layout.element.Text(label + " : ")
                        .setFont(bold)
                        .setFontSize(8f)
                        .setFontColor(PRIMARY))
                .add(new com.itextpdf.layout.element.Text(safeText(value))
                        .setFont(regular)
                        .setFontSize(8.5f)
                        .setFontColor(TEXT))
                .setMarginBottom(6)
                .setBackgroundColor(ROW_ALT_BG)
                .setPadding(5);
    }

    public void addFooter() {
        document.add(new Paragraph(
                "Document généré le " + LocalDate.now().format(DATE_FMT)
                        + " — " + SYSTEM_NAME)
                .setFont(regular)
                .setFontSize(8.5f)
                .setFontColor(FOOTER)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(18)
                .setBorderTop(new SolidBorder(BORDER, 1))
                .setPaddingTop(10));
    }

    public void addNotice(String text, boolean positive) {
        document.add(new Paragraph(text)
                .setFont(positive ? bold : italic)
                .setFontSize(8.5f)
                .setFontColor(positive ? SIGNED_TEXT : SECONDARY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(6));
    }
}
