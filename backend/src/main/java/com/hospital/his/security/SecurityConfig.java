package com.hospital.his.security;

import com.hospital.his.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import org.springframework.http.HttpMethod;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, CustomAccessDeniedHandler customAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> {}).csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        /*
                         * Endpoints públicos de autenticación.
                         */
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/verify-dpi"
                        )
                        .permitAll()

                        /*
                        * Información de la sesión actual.
                         */
                        .requestMatchers("/api/auth/me")
                        .authenticated()

                        .requestMatchers("/api/users/**")
                        .hasRole("ADMIN")

                        /*
                              * CU-14:
                              * La URL exige autenticación.
                              *
                              * La operación específica se autoriza mediante
                              * @PreAuthorize en cada controlador.
                              */
                        .requestMatchers(
                                "/api/branches/**",
                                "/api/specialties/**",
                                "/api/roles/**",
                                "/api/admin/branch-specialties/**",
                                "/api/admin/laboratory-tests/**",
                                "/api/admin/medications/**",
                                "/api/admin/appointment-statuses/**",
                                "/api/admin/permissions/**",
                                "/api/admin/roles/*/permissions"
                        )
                        .authenticated()



                        /*
                         * CU-14:
                         * La autenticación es obligatoria.
                         *
                         * Los permisos concretos se verifican mediante
                         * @PreAuthorize dentro de RolePermissionController.
                         */
                        .requestMatchers(
                                "/api/admin/permissions/**",
                                "/api/admin/roles/*/permissions"
                        )
                        .authenticated()

                        /*
                         * CU-14:
                         * Mantenimiento administrativo del catálogo
                         * de exámenes de laboratorio.
                         */
                        .requestMatchers("/api/admin/laboratory-tests/**")
                        .hasRole("ADMIN")

                        /*
                         * CU-14:
                         * Mantenimiento administrativo
                         * del catálogo de medicamentos.
                         */
                        .requestMatchers("/api/admin/medications/**")
                        .hasRole("ADMIN")

                        /*
                         * CU-14:
                         * Configuración administrativa y visual
                         * de los estados técnicos de cita.
                         */
                        .requestMatchers("/api/admin/appointment-statuses/**")
                        .hasRole("ADMIN")


                        .requestMatchers("/api/appointments/**")
                        .hasRole("PACIENTE")

                        .requestMatchers("/api/payments/**")
                        .hasRole("PACIENTE")

                        .requestMatchers("/api/reception/**")
                        .hasRole("RECEPCIONISTA")

                        .requestMatchers("/api/cashier/**")
                        .hasRole("CAJERO")

                        .requestMatchers("/api/nursing/**")
                        .hasRole("ENFERMERIA")

                        .requestMatchers("/api/doctor/agenda/**")
                        .hasRole("MEDICO")

                        .requestMatchers("/api/doctor/**")
                        .hasRole("MEDICO")

                        .requestMatchers("/api/laboratory/**")
                        .hasRole("LABORATORIO")

                        /*
                         * CU-15:
                         * Bitácora consultable por ADMIN o FARMACIA.
                         *
                         * Los permisos concretos se validan mediante
                         * @PreAuthorize en el controlador.
                         */
                        .requestMatchers(
                                "/api/pharmacy/inventory-movements/**"
                        )
                        .authenticated()

                        /*CU-11:
                         Solo un usuario con rol FARMACIA puede consultar
                         el inventario y realizar despachos.
                         */
                        .requestMatchers("/api/pharmacy/**")
                        .hasRole("FARMACIA")

                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exception ->
                        exception.accessDeniedHandler(
                                customAccessDeniedHandler
                        )
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:5173"));

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}