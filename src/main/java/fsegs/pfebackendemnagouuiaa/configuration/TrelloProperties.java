package fsegs.pfebackendemnagouuiaa.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "trello")
@Data
public class TrelloProperties {
    private String key;
    private String token;
}