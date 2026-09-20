package com.algopoolja.quiz.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class SecurityConfigTest {
    @Test
    void allowsConfiguredProductionFrontendOrigin() {
        SecurityConfig securityConfig = new SecurityConfig(
                "https://www.algopooja.com, https://*.vercel.app/");
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("https://www.algopooja.com"))
                .isEqualTo("https://www.algopooja.com");
        assertThat(configuration.checkOrigin("https://preview.vercel.app"))
                .isEqualTo("https://preview.vercel.app");
        assertThat(configuration.checkOrigin("https://untrusted.example.com")).isNull();
    }
}
