package fsegs.pfebackendemnagouuiaa.services;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.HorizontalAlignment;
import fsegs.pfebackendemnagouuiaa.entities.Signature;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Chargement des images de signature pour les PDF (data URL, URL distante, fichiers locaux).
 */
@Component
public class SignatureImagePdfHelper {

    private static final Pattern RAW_BASE64 = Pattern.compile("^[A-Za-z0-9+/\\r\\n=]+$");
    private static final List<String> PLACEHOLDER_MARKERS = List.of(
            "aperçu indisponible",
            "apercu indisponible",
            "signature enregistrée",
            "signature enregistree",
            "image indisponible"
    );

    private final UtilisateurRepository utilisateurRepository;

    @Value("${app.signature.base-dir:}")
    private String signatureBaseDir;

    public SignatureImagePdfHelper(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    /**
     * Priorité : image sur l'entité {@link Signature}, puis profil du signataire, puis repli profil stage.
     */
    public String resolveImageSource(Optional<Signature> signature, String profileFallback) {
        if (signature.isPresent()) {
            Signature s = signature.get();
            if (isExploitableImageSource(s.getUrlSignature())) {
                return s.getUrlSignature().trim();
            }
            if (s.getSignataireId() != null) {
                Optional<String> fromUser = utilisateurRepository.findById(s.getSignataireId())
                        .map(Utilisateur::getUrlSignature)
                        .filter(this::isExploitableImageSource);
                if (fromUser.isPresent()) {
                    return fromUser.get().trim();
                }
            }
        }
        return isExploitableImageSource(profileFallback) ? profileFallback.trim() : "";
    }

    public String firstExploitableSource(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String candidate : candidates) {
            if (isExploitableImageSource(candidate)) {
                return candidate.trim();
            }
        }
        return "";
    }

    public boolean isExploitableImageSource(String source) {
        if (source == null) {
            return false;
        }
        String trimmed = source.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        for (String marker : PLACEHOLDER_MARKERS) {
            if (lower.contains(marker)) {
                return false;
            }
        }
        if (trimmed.startsWith("data:image/")) {
            return trimmed.contains(",");
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file:")) {
            return true;
        }
        if (looksLikeRawBase64(trimmed)) {
            return true;
        }
        String normalized = trimmed.replace('\\', '/');
        if (normalized.contains("/signatures/") || normalized.contains("/uploads/")) {
            return true;
        }
        return hasImageExtension(normalized);
    }

    public Optional<Image> loadSignatureImage(String signatureSource, float maxWidth, float maxHeight) {
        if (!isExploitableImageSource(signatureSource)) {
            return Optional.empty();
        }

        String source = signatureSource.trim();
        try {
            ImageData imageData = readImageData(source);
            if (imageData == null) {
                return Optional.empty();
            }
            Image image = new Image(imageData);
            image.setAutoScale(false);
            image.scaleToFit(maxWidth, maxHeight);
            image.setHorizontalAlignment(HorizontalAlignment.CENTER);
            return Optional.of(image);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private ImageData readImageData(String source) throws Exception {
        if (source.startsWith("data:image/")) {
            return ImageDataFactory.create(decodeDataUrl(source));
        }
        if (source.startsWith("http://") || source.startsWith("https://")) {
            return ImageDataFactory.create(new URL(source));
        }
        if (looksLikeRawBase64(source)) {
            return ImageDataFactory.create(decodeBase64(source));
        }

        Path path = resolveSignaturePath(source);
        if (path != null && Files.isRegularFile(path)) {
            return ImageDataFactory.create(Files.readAllBytes(path));
        }
        return null;
    }

    private byte[] decodeDataUrl(String dataUrl) {
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex < 0) {
            throw new IllegalArgumentException("Data URL invalide");
        }
        return decodeBase64(dataUrl.substring(commaIndex + 1));
    }

    private byte[] decodeBase64(String payload) {
        String normalized = payload.replaceAll("\\s+", "");
        return Base64.getDecoder().decode(normalized);
    }

    private boolean looksLikeRawBase64(String value) {
        if (value.length() < 80) {
            return false;
        }
        return RAW_BASE64.matcher(value).matches();
    }

    private boolean hasImageExtension(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".gif")
                || lower.endsWith(".webp")
                || lower.endsWith(".bmp");
    }

    private Path resolveSignaturePath(String source) {
        try {
            if (source.startsWith("file:")) {
                Path filePath = Path.of(URI.create(source));
                return Files.isRegularFile(filePath) ? filePath.normalize() : null;
            }

            String normalized = source.replace('\\', '/').trim();
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }

            Path userDir = Path.of(System.getProperty("user.dir", "."));
            List<Path> candidates = new ArrayList<>();

            if (signatureBaseDir != null && !signatureBaseDir.isBlank()) {
                candidates.add(Path.of(signatureBaseDir).resolve(normalized).normalize());
            }

            candidates.add(userDir.resolve(normalized).normalize());
            candidates.add(userDir.resolve("uploads").resolve("signatures").resolve(Path.of(normalized).getFileName()).normalize());

            if (normalized.contains("/")) {
                candidates.add(userDir.resolve(normalized).normalize());
            } else {
                candidates.add(userDir.resolve("uploads").resolve("signatures").resolve(normalized).normalize());
                candidates.add(userDir.resolve("src").resolve("main").resolve("resources").resolve("static")
                        .resolve("uploads").resolve("signatures").resolve(normalized).normalize());
            }

            Path direct = Path.of(source);
            if (direct.isAbsolute()) {
                candidates.add(0, direct.normalize());
            }

            for (Path candidate : candidates) {
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }
}
