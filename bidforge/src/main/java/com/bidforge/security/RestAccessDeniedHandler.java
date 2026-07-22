package com.bidforge.security;

import com.bidforge.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;


// Decides what an AUTHENTICATED user who lacks the required role receives when a URL rule blocks them
// like a normal user calling /api/admin/**
// HTTP 403 in the standard JSON error shape

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(ErrorResponse.of(
                403, "FORBIDDEN",
                "You do not have permission to perform this action.",
                request.getRequestURI()).toJson());
    }
}
