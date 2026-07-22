package com.bidforge.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bidforgeOpenApi() {
        String scheme = "bearerAuth";
        return new OpenAPI().info(new Info().title("BidForge: an Auction Engine API").description("""
                
                Login via POST /api/auth/login, click Authorize and paste the token.
                Seeded demo accounts are in companion document""").version("1.0")).components(new Components().addSecuritySchemes(scheme, new SecurityScheme().name(scheme).type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))).addSecurityItem(new SecurityRequirement().addList(scheme));
    }
}
