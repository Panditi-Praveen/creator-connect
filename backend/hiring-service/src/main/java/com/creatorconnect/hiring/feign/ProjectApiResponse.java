package com.creatorconnect.hiring.feign;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Client-side envelope mirroring the Project Service's generic
 * {@code ApiResponse} JSON shape:
 * <pre>
 * { "timestamp": "...", "status": 200, "message": "...", "data": {...}, "path": "/projects/..." }
 * </pre>
 *
 * <p>This is a mirror of the Project Service's success envelope, needed only
 * to deserialize Feign responses — the Hiring Service's own
 * {@code ApiResponse} cannot be reused because its private constructor and
 * immutable fields are not Jackson-deserializable. The envelope is never
 * exposed to API clients; callers unwrap {@link #getData()}.
 *
 * @param <T> the type of the payload carried in {@code data}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectApiResponse<T> {

    private LocalDateTime timestamp;

    private int status;

    private String message;

    private T data;

    private String path;
}
