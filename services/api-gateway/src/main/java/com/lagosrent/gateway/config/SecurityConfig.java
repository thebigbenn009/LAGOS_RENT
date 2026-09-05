package com.lagosrent.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // public endpoints
                        .pathMatchers("/actuator/**")
                        .permitAll()
                        .pathMatchers("/api/v1/search/**")
                        .permitAll()
                        // All other API endpoints require valid JWT authentication
                        .anyExchange()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
        return http.build();
    }

    //    @Bean
    //    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    //        return builder.routes()
    //                .route("account-service", r -> r.path("/api/v1/accounts/**")
    //                        .filters(f -> f.removeRequestHeader("X-User-Id").removeRequestHeader("X-Internal-Service"))
    //                        .uri("lb://account-service"))
    //                .route("account-service-admin", r -> r.path("/api/v1/admin/**")
    //                        .filters(f -> f.removeRequestHeader("X-User-Id").removeRequestHeader("X-Internal-Service"))
    //                        .uri("lb://account-service"))
    //                .route("listing-service", r -> r.path("/api/v1/listings/**")
    //                        .filters(f -> f.removeRequestHeader("X-User-Id").removeRequestHeader("X-Internal-Service"))
    //                        .uri("lb://listing-service"))
    //                .route("search-service", r -> r.path("/api/v1/search/**")
    //                        .filters(f -> f.removeRequestHeader("X-User-Id").removeRequestHeader("X-Internal-Service"))
    //                        .uri("lb://search-service"))
    //                .route("rental-service", r -> r.path("/api/v1/rentals/**")
    //                        .filters(f -> f.removeRequestHeader("X-User-Id").removeRequestHeader("X-Internal-Service"))
    //                        .uri("lb://rental-service"))
    //                .route("payment-service", r -> r.path("/api/v1/payments/**")
    //                        .filters(f -> f.removeRequestHeader("X-User-Id").removeRequestHeader("X-Internal-Service"))
    //                        .uri("lb://payment-service"))
    //                .build();
    //    }
}
