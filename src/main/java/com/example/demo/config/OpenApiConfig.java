package com.example.demo.config;


import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    public OpenAPI openApi() {
        return new OpenAPI();
    }

}
