package com.creatorconnect.ai.controller;

import com.creatorconnect.ai.dto.request.DiscoverRequest;
import com.creatorconnect.ai.dto.response.ApiResponse;
import com.creatorconnect.ai.dto.response.DiscoverResponse;
import com.creatorconnect.ai.dto.response.StatusResponse;
import com.creatorconnect.ai.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the AI Service public API.
 *
 * <p>Base path: {@code /ai}. The API Gateway forwards {@code /ai/**} to this
 * service unchanged (no rewritePath filter on the AI route), so the internal
 * mappings are exactly {@code /ai/status} and {@code /ai/discover} — the same
 * paths clients use through the Gateway.
 *
 * <p>{@code POST /ai/discover} requires a valid JWT issued by the Auth
 * Service (enforced by {@code SecurityBeansConfig}); {@code GET /ai/status}
 * is public.
 */
@RestController
@RequestMapping("/ai")
@Tag(name = "AI Discovery", description = "AI-assisted talent discovery — natural-language search over freelancer profiles")
public class AiController {

    private final AiService aiService;

    /**
     * Creates the controller with its service dependency.
     *
     * @param aiService the discovery business logic
     */
    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * Lightweight status/health check.
     *
     * @param httpRequest the raw request (used to echo the request path)
     * @return {@code 200 OK} with the service status
     */
    @GetMapping("/status")
    @Operation(
            summary = "AI service status",
            description = "Returns the service status. Public — no JWT required. "
                    + "It reports UP as long as the AI service itself is running (it does not "
                    + "depend on the LLM API key being configured)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "AI service is up")
    })
    public ResponseEntity<ApiResponse<StatusResponse>> status(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "AI service is up",
                aiService.status(),
                httpRequest.getRequestURI()
        ));
    }

    /**
     * Natural-language talent discovery.
     *
     * @param request     the validated discovery payload
     * @param httpRequest the raw request (used to echo the request path)
     * @return {@code 200 OK} with the ranked results
     */
    @PostMapping("/discover")
    @Operation(
            summary = "Natural-language talent discovery",
            description = "Accepts a natural-language hiring query, fetches the platform's freelancer "
                    + "profiles from the Profile Service, and returns LLM-ranked matches. Requires a "
                    + "valid JWT issued by the Auth Service."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Discovery completed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502", description = "LLM API failure"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503", description = "LLM not configured or Profile Service unavailable")
    })
    public ResponseEntity<ApiResponse<DiscoverResponse>> discover(
            @Valid @RequestBody DiscoverRequest request,
            HttpServletRequest httpRequest) {

        DiscoverResponse result = aiService.discover(request.getQuery().trim());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Talent discovery completed",
                result,
                httpRequest.getRequestURI()
        ));
    }
}
