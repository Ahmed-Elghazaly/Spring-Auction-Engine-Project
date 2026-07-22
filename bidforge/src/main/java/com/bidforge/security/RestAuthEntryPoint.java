package com.bidforge.security;

import com.bidforge.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;


// Decides what an UNAUTHENTICATED request to a protected endpoint receives
// Spring Security's default is an HTML login redirect which is meaningless for a REST API
// This replacement returns 401 with the same JSON error shape the rest of the API uses
// It runs before Spring MVC, which is why it writes the response directly instead of going through the GlobalExceptionHandler

@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(ErrorResponse.of(
                401, "UNAUTHORIZED",
                "Authentication required: send a valid 'Authorization: Bearer <token>' header.",
                request.getRequestURI()).toJson());
    }
}
