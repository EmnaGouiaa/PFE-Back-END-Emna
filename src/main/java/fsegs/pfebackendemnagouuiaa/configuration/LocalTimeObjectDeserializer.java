package fsegs.pfebackendemnagouuiaa.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalTime;

public class LocalTimeObjectDeserializer extends JsonDeserializer<LocalTime> {

    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        if (node.isTextual()) {
            return LocalTime.parse(node.asText());
        }

        if (node.isArray()) {
            int hour = node.size() > 0 ? node.get(0).asInt() : 0;
            int minute = node.size() > 1 ? node.get(1).asInt() : 0;
            int second = node.size() > 2 ? node.get(2).asInt() : 0;
            int nano = node.size() > 3 ? node.get(3).asInt() : 0;
            return LocalTime.of(hour, minute, second, nano);
        }

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