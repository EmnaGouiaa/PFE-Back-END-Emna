package fsegs.pfebackendemnagouuiaa.services.pdf;

import com.itextpdf.kernel.colors.DeviceRgb;

import java.time.format.DateTimeFormatter;

/**
 * Charte graphique unifiee des documents PDF de stage (FSEGS).
 * Couleurs : #154A7C, #0099A9, #CCD323
 */
public final class InternshipPdfTheme {

    private InternshipPdfTheme() {
    }

    public static final DeviceRgb PRIMARY = new DeviceRgb(21, 74, 124);
    public static final DeviceRgb SECONDARY = new DeviceRgb(0, 153, 169);
    public static final DeviceRgb ACCENT = new DeviceRgb(204, 211, 35);
    public static final DeviceRgb TEXT = new DeviceRgb(30, 41, 59);
    public static final DeviceRgb MUTED = new DeviceRgb(100, 116, 139);
    public static final DeviceRgb BORDER = new DeviceRgb(203, 213, 225);
    public static final DeviceRgb ROW_LABEL_BG = new DeviceRgb(232, 245, 248);
    public static final DeviceRgb ROW_ALT_BG = new DeviceRgb(248, 252, 253);
    public static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);
    public static final DeviceRgb BADGE_OK_BG = new DeviceRgb(220, 252, 231);
    public static final DeviceRgb BADGE_OK_TEXT = new DeviceRgb(22, 101, 52);
    public static final DeviceRgb BADGE_WARN_BG = new DeviceRgb(254, 249, 195);
    public static final DeviceRgb BADGE_WARN_TEXT = new DeviceRgb(133, 77, 14);
    public static final DeviceRgb SIGNED_TEXT = new DeviceRgb(22, 101, 52);
    public static final DeviceRgb PENDING_TEXT = new DeviceRgb(185, 28, 28);
    public static final DeviceRgb FOOTER = new DeviceRgb(148, 163, 184);
    public static final DeviceRgb PLACEHOLDER_BG = new DeviceRgb(248, 250, 252);

    public static final float MARGIN_TOP = 42f;
    public static final float MARGIN_BOTTOM = 42f;
    public static final float MARGIN_LEFT = 40f;
    public static final float MARGIN_RIGHT = 40f;

    public static final float SIG_IMAGE_MAX_W = 165f;
    public static final float SIG_IMAGE_MAX_H = 62f;

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static final String INSTITUTION = " ";
    public static final String SYSTEM_NAME = "Système de gestion des stages";

    public static String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return value.trim();
    }
}
