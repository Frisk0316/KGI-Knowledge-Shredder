package com.kgi.shredder.config;

import com.kgi.shredder.config.properties.KgiProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, KgiProperties properties) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/documents/upload").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/documents/*/source").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/documents/*").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/documents/*/reprocess").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/v1/audit/**", "/api/v1/admin/**").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated());

        String devTrainerId = properties.security().devTrainerId();
        if (devTrainerId != null && !devTrainerId.isBlank()) {
            http.addFilterBefore(new DevTrainerAuthenticationFilter(devTrainerId), AnonymousAuthenticationFilter.class);
        } else {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter(properties))));
        }

        return http.build();
    }

    @Bean
    @ConditionalOnExpression("'${kgi.security.dev-trainer-id:}' == ''")
    JwtDecoder jwtDecoder(KgiProperties properties) {
        String issuerUri = properties.security().issuerUri();
        String jwkSetUri = properties.security().jwkSetUri();
        NimbusJwtDecoder decoder;
        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        } else if (issuerUri != null && !issuerUri.isBlank()) {
            decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        } else {
            throw new IllegalStateException("KGI_JWT_ISSUER_URI or KGI_JWT_JWK_SET_URI is required when KGI_DEV_TRAINER_ID is blank.");
        }

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        if (issuerUri != null && !issuerUri.isBlank()) {
            validators.add(JwtValidators.createDefaultWithIssuer(issuerUri));
        } else {
            validators.add(JwtValidators.createDefault());
        }
        String audience = properties.security().audience();
        if (audience != null && !audience.isBlank()) {
            validators.add(jwt -> jwt.getAudience().contains(audience)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing required audience.", null)));
        }
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter(KgiProperties properties) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName(properties.security().trainerClaim());
        JwtGrantedAuthoritiesConverter scopeAuthorities = new JwtGrantedAuthoritiesConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> scopes = scopeAuthorities.convert(jwt);
            List<GrantedAuthority> authorities = new ArrayList<>(scopes == null ? List.of() : scopes);
            Object roles = jwt.getClaims().get("roles");
            if (roles instanceof Collection<?> roleCollection) {
                roleCollection.stream()
                        .map(String::valueOf)
                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
            }
            Object singleRole = jwt.getClaims().get("role");
            if (singleRole != null) {
                String role = String.valueOf(singleRole);
                authorities.add(new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role));
            }
            return authorities;
        });
        return converter;
    }

    private static final class DevTrainerAuthenticationFilter extends OncePerRequestFilter {
        private final String trainerId;

        private DevTrainerAuthenticationFilter(String trainerId) {
            this.trainerId = trainerId;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String actorId = normalizeActor(request.getHeader("X-Dev-User"));
                List<SimpleGrantedAuthority> authorities = actorId.equals("learner_002")
                        ? List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        : List.of(new SimpleGrantedAuthority("ROLE_TRAINER"), new SimpleGrantedAuthority("ROLE_ADMIN"));
                var authentication = new UsernamePasswordAuthenticationToken(
                        trainerId,
                        "dev",
                        authorities
                );
                authentication.setDetails(new SecurityContextUtil.DevUserDetails(actorId, trainerId));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        }

        private String normalizeActor(String requestedActor) {
            if ("learner_002".equals(requestedActor)) {
                return "learner_002";
            }
            return "learner_001";
        }
    }
}
