package com.creatorconnect.ai.service;

import com.creatorconnect.ai.dto.response.DiscoverResponse;
import com.creatorconnect.ai.dto.response.StatusResponse;

/**
 * AI Service use cases — the business logic contract layer.
 *
 * <p>Exposes the operations the AI Service API supports. Implementations live
 * in {@code service.impl}; the interface decouples the controller from
 * concrete logic.
 */
public interface AiService {

    /**
     * Runs a natural-language talent discovery: fetches the platform's
     * freelancer profiles from the Profile Service and asks the LLM to rank
     * them against the query.
     *
     * @param query the validated, trimmed hiring query
     * @return the ranked results (empty when no profiles exist or nothing matches)
     */
    DiscoverResponse discover(String query);

    /**
     * Returns the service status.
     *
     * @return the status payload ({@code service} + {@code status})
     */
    StatusResponse status();
}
