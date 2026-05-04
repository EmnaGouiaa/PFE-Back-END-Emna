package fsegs.pfebackendemnagouuiaa.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MailTestResponse {
    private final boolean authenticationOk;
    private final boolean emailSent;
    private final String host;
    private final int port;
    private final String username;
    private final String recipientEmail;
    private final String message;
}
