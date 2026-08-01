package com.creatorconnect.auth.service;

import com.creatorconnect.auth.dto.request.RegisterRequest;
import com.creatorconnect.auth.dto.response.RegisterResponse;

/**
 * Auth Service use cases — the business logic contract layer.
 *
 * <p>Exposes the operations the Auth Service API supports. Implementations live
 * in {@code service.impl}; the interface decouples the controller from concrete
 * logic (SOLID — dependency inversion).
 */
public interface AuthService {

    /**
     * Registers a new CreatorConnect user.
     *
     * @param request the validated registration payload
     * @return the persisted user as a safe response DTO
     * @throws com.creatorconnect.auth.exception.EmailAlreadyExistsException
     *         when the email is already registered
     */
    RegisterResponse register(RegisterRequest request);
}
