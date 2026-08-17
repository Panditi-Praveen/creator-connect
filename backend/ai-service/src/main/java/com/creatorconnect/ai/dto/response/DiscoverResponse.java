package com.creatorconnect.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Payload returned by {@code POST /ai/discover}.
 *
 * @param query   the echoed natural-language query
 * @param count   the number of ranked results
 * @param results the ranked talent matches (may be empty when nothing matches)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoverResponse {

    private String query;

    private int count;

    private List<TalentResult> results;
}
