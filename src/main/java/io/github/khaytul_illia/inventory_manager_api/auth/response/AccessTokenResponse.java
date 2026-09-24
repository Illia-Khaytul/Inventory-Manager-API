package io.github.khaytul_illia.inventory_manager_api.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

@Schema(
    description = "Response containing the access and refresh tokens",
    example = """
        {
            "issuer": "http://localhost:8080/api/v1",
            "issuedAt": "2026-09-24T10:33:00Z",
            "expiresAt": "2026-09-24T10:48:00Z",
            "subject": "user1",
            "role": "CUSTOMER",
            "accessToken": "eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXBpL3YxIiwiaWF0IjoxNzkwMjM4NzgwLCJleHAiOjE3OTAyMzk2ODAsInN1YiI6InVzZXIxIiwicm9sZXMiOiJDVVNUT01FUiJ9.PW4wAq0nBMW0hGgzDGvuDWaHLQ_9YdC_R0-n8djdGKuiNxKGFBamazuePc5of7rXkF8CUbEPNJYnSXgaGYGuvuSeqmFecqdBU_EuV7oHODnWPWErjLLyEynKiz8LCASk2wcRfWAq4XLy_4VZqpG418X76i2tl30JaNeWQ2nCbW-m-t40yKDpup2vUjBe5FhsiW0e3tGSQkPottOt2K17O-yiJONl99FKP_h9JAiggQAhy2Xxi2SvjqdyFtyXWh3HLKF9OrxCwZ0wp_vAGx0R5NyfS-R9CZwkMuhzfDJdWpZw0e7Qi1TlIdxTv3EFRSZ-DAWOCw3S33Bq4H6MsNjErPvE9qvNg5_e57TdBUR2KDQAotWumuoKqTIVZaTcj65KyF0DYZ65QKYLkKlOme9QHXUgXBSxgce1VkHlKg0AQhUtLC3cDPIMLiXNoeBr_ERkREDJMXfSx_rNeAXqJeZr2Ki2fjCTnNCYtFjBcN8F5Z1y-GFX_J7d8ecIMLJ5dmKFyItbZBfO0-lIBKUeG8iEI2syK5sZS4LQFvqrb5C355b1Yi9I7ex7ndITM19YLPM_11-bPNvtRt2yI0NZjdtgfoGSjk-ZINlsqxGC_xhnxhYQeqNqUPa3Ro_8CRYzHe746QwWDc79n-QJ2KyUy7Y5JaJp0sYpdduxtM6jnHuyyn8",
            "refreshToken": "e58ed763-928c-4155-bee9-fdbaaadc15f3"
        }
        """
)
public record AccessTokenResponse(
    String issuer,
    Instant issuedAt,
    Instant expiresAt,
    String subject,
    String role,
    String accessToken,
    String refreshToken
) {

    public AccessTokenResponse(Jwt jwt, String refreshToken){
        this(
            jwt.getIssuer().toString(),
            jwt.getIssuedAt(),
            jwt.getExpiresAt(),
            jwt.getSubject(),
            jwt.getClaimAsString("roles"),
            jwt.getTokenValue(),
            refreshToken
        );
    }

}
