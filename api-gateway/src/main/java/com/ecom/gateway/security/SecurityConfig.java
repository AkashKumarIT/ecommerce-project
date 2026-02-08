package com.ecom.gateway.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange

                        // 🟢 PUBLIC
                        .pathMatchers(HttpMethod.GET,
                                "/product-service/api/products",
                                "/product-service/api/products/**"
                        ).permitAll()

                        // 🔐 ADMIN
                        .pathMatchers(HttpMethod.POST,
                                "/product-service/api/products/**"
                        ).hasRole("ADMIN")

                        .pathMatchers(HttpMethod.PUT,
                                "/product-service/api/products/**"
                        ).hasRole("ADMIN")

                        .pathMatchers(HttpMethod.DELETE,
                                "/product-service/api/products/**"
                        ).hasRole("ADMIN")

                        // 🔐 CUSTOMER
                        .pathMatchers("/order-service/**")
                        .hasRole("CUSTOMER")

                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
        }

    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // Custom logic to handle Keycloak's nested "realm_access" -> "roles" structure
        // CHANGE HERE: Use 'Jwt' instead of 'OAuth2ResourceServerProperties.Jwt'
        converter.setJwtGrantedAuthoritiesConverter(new Converter<Jwt, Collection<GrantedAuthority>>() {
            @Override
            public Collection<GrantedAuthority> convert(Jwt jwt) {
                // 1. Get the "realm_access" object
                Map<String, Object> realmAccess = jwt.getClaim("realm_access");

                if (realmAccess == null || realmAccess.isEmpty()) {
                    return Collections.emptyList();
                }

                // 2. Get the "roles" list from that object
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");

                if (roles == null || roles.isEmpty()) {
                    return Collections.emptyList();
                }

                // 3. Convert to SimpleGrantedAuthority with "ROLE_" prefix
                return roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
            }
        });

        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

}
