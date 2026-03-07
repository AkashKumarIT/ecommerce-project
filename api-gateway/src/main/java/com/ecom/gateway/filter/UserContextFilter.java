package com.ecom.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class UserContextFilter
        extends AbstractGatewayFilterFactory<UserContextFilter.Config> {

    public UserContextFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> exchange.getPrincipal()
                .cast(Authentication.class)
                .flatMap(auth -> {
                    if (!(auth.getPrincipal() instanceof Jwt jwt)) {
                        return chain.filter(exchange);
                    }

                    String userId = jwt.getSubject();
                    String username = jwt.getClaimAsString("preferred_username");

                    // --- FIX START ---

                    // 1. Get the raw claim safely
                    Map<String, Object> realmAccess = jwt.getClaim("realm_access");

                    // 2. Prepare a temporary variable to hold the logic
                    List<String> tempRoles = List.of();

                    if (realmAccess != null && realmAccess.containsKey("roles")) {
                        // Suppress the warning because we know Keycloak returns a list here
                        @SuppressWarnings("unchecked")
                        List<String> extracted = (List<String>) realmAccess.get("roles");
                        tempRoles = extracted;
                    }

                    // 3. Create a FINAL copy. This satisfies the "effectively final" error.
                    final List<String> roles = tempRoles;

                    // --- FIX END ---

                    return chain.filter(
                            exchange.mutate()
                                    .request(r -> r.headers(headers -> {
                                        // Now we use the 'roles' variable which is guaranteed not to change
                                        if (userId != null) headers.add("X-User-Id", userId);
                                        if (username != null) headers.add("X-Username", username);
                                        if (!roles.isEmpty()) headers.add("X-Roles", String.join(",", roles));
                                    }))
                                    .build()
                    );
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    public static class Config {}
}
