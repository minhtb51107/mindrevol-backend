package com.mindrevol.backend.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.backend.common.dto.ApiResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        String jwtError = (String) request.getAttribute("JWT_ERROR");
        
        String message = "Unauthorized";
        String errorCode = "SESSION_INVALID"; 

        if ("TOKEN_EXPIRED".equals(jwtError)) {
            message = "Access Token has expired";
            errorCode = "TOKEN_EXPIRED"; 
        } else if ("TOKEN_INVALID".equals(jwtError)) {
            message = "Invalid Token";
            errorCode = "TOKEN_INVALID";
        }

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); 

        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .status(401)
                .message(message)
                .data(errorCode) 
                .build();

        ObjectMapper mapper = new ObjectMapper();
        
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        
        mapper.writeValue(response.getOutputStream(), apiResponse);
    }
}