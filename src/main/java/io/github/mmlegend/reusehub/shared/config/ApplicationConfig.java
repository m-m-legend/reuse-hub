package io.github.mmlegend.reusehub.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;

@Configuration
public class ApplicationConfig {
    @Bean
    Clock clock() { return Clock.systemUTC(); }

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("ReUse Hub API").version("1.0.0")
                .description("Equipamentos circulando, oportunidades crescendo. Use /auth/register e /auth/login para obter um token."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
