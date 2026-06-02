package fsegs.pfebackendemnagouuiaa.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalTime;

/**
 * Désérialiseur Jackson personnalisé pour {@link LocalTime} acceptant plusieurs formats JSON.
 *
 * <p><b>Rôle :</b> tolérer les payloads front Angular / API qui envoient l'heure soit en
 * chaîne ISO ({@code "09:30:00"}), soit en tableau {@code [h,m,s,nano]}, soit en objet
 * {@code {hour, minute, second, nano}}.</p>
 *
 * <p><b>Relations :</b> enregistré sur les DTO concernés (réunions, créneaux) lorsque
 * le format par défaut Jackson est insuffisant.</p>
 */
public class LocalTimeObjectDeserializer extends JsonDeserializer<LocalTime> {

    /**
     * Convertit le nœud JSON courant en {@link LocalTime}.
     *
     * @param p   parseur Jackson positionné sur la valeur
     * @param ctxt contexte de désérialisation
     * @return heure locale construite
     * @throws IOException si le format n'est ni texte, ni tableau, ni objet reconnu
     */
    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // Format ISO-8601 local time : "HH:mm:ss" ou "HH:mm"
        if (node.isTextual()) {
            return LocalTime.parse(node.asText());
        }

        // Format tableau [heure, minute, seconde, nano] — défaut 0 pour les indices absents
        if (node.isArray()) {
            int hour = node.size() > 0 ? node.get(0).asInt() : 0;
            int minute = node.size() > 1 ? node.get(1).asInt() : 0;
            int second = node.size() > 2 ? node.get(2).asInt() : 0;
            int nano = node.size() > 3 ? node.get(3).asInt() : 0;
            return LocalTime.of(hour, minute, second, nano);
        }

        // Format objet { hour, minute, second?, nano? }
        if (node.isObject()) {
            int hour = node.has("hour") ? node.get("hour").asInt() : 0;
            int minute = node.has("minute") ? node.get("minute").asInt() : 0;
            int second = node.has("second") ? node.get("second").asInt() : 0;
            int nano = node.has("nano") ? node.get("nano").asInt() : 0;
            return LocalTime.of(hour, minute, second, nano);
        }

        throw new IOException("Format invalide pour LocalTime");
    }
}
