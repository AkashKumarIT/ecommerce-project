package com.ecom.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

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
                        .pathMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/*/v3/api-docs"
                        ).permitAll()

                        .pathMatchers(HttpMethod.GET, "/product-service/api/products/**").permitAll()

                        .pathMatchers(HttpMethod.POST, "/cart-service/api/cart/items").permitAll()
                        .pathMatchers(HttpMethod.GET, "/cart-service/api/cart").permitAll()
                        .pathMatchers(HttpMethod.PUT, "/cart-service/api/cart/items/**").permitAll()
                        .pathMatchers(HttpMethod.DELETE, "/cart-service/api/cart/items/**").permitAll()

                        .pathMatchers(HttpMethod.POST, "/product-service/api/products/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/product-service/api/products/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/product-service/api/products/**").hasRole("ADMIN")

                        .pathMatchers(HttpMethod.POST, "/order-service/api/order/**").hasRole("CUSTOMER")
                        .pathMatchers(HttpMethod.POST, "/order-service/api/order/cancel/**").hasRole("CUSTOMER")
                        .pathMatchers(HttpMethod.POST, "/cart-service/api/cart/checkout").hasRole("CUSTOMER")

                        .pathMatchers(HttpMethod.GET, "/payment-service/api/payments/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .pathMatchers(HttpMethod.POST, "/payment-service/api/payments/**").hasAnyRole("CUSTOMER", "ADMIN")

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

        converter.setJwtGrantedAuthoritiesConverter(new Converter<Jwt, Collection<GrantedAuthority>>() {
            @Override
            public Collection<GrantedAuthority> convert(Jwt jwt) {
                Map<String, Object> realmAccess = jwt.getClaim("realm_access");

                if (realmAccess == null || realmAccess.isEmpty()) {
                    return Collections.emptyList();
                }

                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");

                if (roles == null || roles.isEmpty()) {
                    return Collections.emptyList();
                }

                return roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
            }
        });

        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
