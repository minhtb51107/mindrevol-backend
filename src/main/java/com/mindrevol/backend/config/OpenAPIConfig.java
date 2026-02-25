package com.mindrevol.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Value("${mindrevol.openapi.dev-url:http://localhost:8080}")
    private String devUrl;

    @Value("${mindrevol.openapi.prod-url:https://mindrevol-api.onrender.com}")
    private String prodUrl;

    @Bean
    public OpenAPI myOpenAPI() {
        // 1. Cấu hình Server (Dev & Prod) để test API đúng môi trường
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Server phát triển (Local)");

        Server prodServer = new Server();
        prodServer.setUrl(prodUrl);
        prodServer.setDescription("Server thực tế (Production)");

        // 2. Thông tin chung về dự án
        Info info = new Info()
                .title("MindRevol API Documentation")
                .version("1.0")
                .contact(new Contact().email("team@mindrevol.com").name("MindRevol Team"))
                .description("Tài liệu API cho ứng dụng mạng xã hội phát triển bản thân MindRevol.");

        // 3. Cấu hình bảo mật JWT (Nút Authorize ổ khóa)
        String securitySchemeName = "Bearer Authentication";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securitySchemeName);
        
        Components components = new Components().addSecuritySchemes(securitySchemeName,
                new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}