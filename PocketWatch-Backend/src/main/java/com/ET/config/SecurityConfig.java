package com.ET.config;

import com.ET.security.JwtAuthenticationFilter;
import com.ET.security.RateLimitFilter;
import com.ET.security.UserDetailsServiceImpl;
import com.ET.security.UserRateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final UserRateLimitFilter userRateLimitFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    /**
     * Configures Cross-Origin Resource Sharing (CORS).
     *
     * The allowed origins are read from application configuration so that
     * different environments can use different frontend URLs.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(
                List.of(allowedOrigins.split(","))
                        .stream()
                        .map(String::trim)
                        .toList()
        );

        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        config.setAllowedHeaders(
                List.of("Authorization", "Content-Type")
        );

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    /**
     * Configures the application's Spring Security filter chain.
     *
     * The application uses:
     * - Stateless authentication through JWT
     * - Public authentication endpoints
     * - JWT authentication for protected endpoints
     * - IP-based rate limiting for public auth endpoints
     * - User-based rate limiting for authenticated, resource-intensive endpoints
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // Enable CORS using the configuration defined above.
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )

                // JWT authentication is stateless, so CSRF protection is
                // not required for this API configuration.
                .csrf(AbstractHttpConfigurer::disable)

                // Do not create or maintain HTTP sessions.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /*
                 * Return HTTP 401 when an unauthenticated request attempts
                 * to access a protected endpoint.
                 */
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(
                                (request, response, authException) ->
                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                "Unauthorized"
                                        )
                        )
                )

                /*
                 * Define which endpoints are publicly accessible.
                 *
                 * All other endpoints require authentication.
                 */
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/verify-email",
                                "/api/v1/auth/resend-verification",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/actuator/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Configure username/password authentication.
                .authenticationProvider(authenticationProvider())

                /*
                 * JWT filter runs before Spring Security's standard
                 * UsernamePasswordAuthenticationFilter.
                 */
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                /*
                 * Public endpoint rate limiter runs before the JWT filter.
                 *
                 * This allows anonymous authentication endpoints to be
                 * rate-limited using the client's IP address.
                 */
                .addFilterBefore(
                        rateLimitFilter,
                        JwtAuthenticationFilter.class
                )

                /*
                 * User rate limiter runs after JWT authentication because
                 * it needs the authenticated user's identity from the
                 * SecurityContext.
                 */
                .addFilterAfter(
                        userRateLimitFilter,
                        JwtAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * Configures the authentication provider used by Spring Security.
     *
     * UserDetailsService loads the user from the database and BCrypt is
     * used to verify the supplied password against the stored hash.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    /**
     * Exposes the AuthenticationManager used by the authentication flow.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {

        return config.getAuthenticationManager();
    }

    /**
     * Creates the password encoder used for password hashing and verification.
     *
     * BCrypt strength 12 increases the computational cost of password
     * hashing, making offline password cracking more expensive.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}