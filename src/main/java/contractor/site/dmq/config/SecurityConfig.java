package contractor.site.dmq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for API endpoints (enable for form submissions)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            )
            
            // Authorization configuration
            .authorizeHttpRequests(auth -> auth
                // Allow public access to static resources
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/index",
                    "/*.html",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/assets/**",
                    "/uploads/**",
                    "/static/**"
                ).permitAll()
                
                // Allow public access to read-only API endpoints
                .requestMatchers(
                    "/api/services/**",
                    "/api/services",
                    "/api/projects/**",
                    "/api/projects",
                    "/api/testimonials/**",
                    "/api/testimonials",
                    "/api/previous-projects/**",
                    "/api/previous-projects"
                ).permitAll()
                
                // Allow public quotation submission
                .requestMatchers("/api/quotations").permitAll()
                
                // Require authentication for admin endpoints
                .requestMatchers("/api/admin/**").authenticated()
                .requestMatchers("/admin/**").authenticated()
                
                // All other requests need authentication
                .anyRequest().authenticated()
            )
            
            // Login configuration
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/admin.html", true)
                .failureUrl("/login.html?error")
                .permitAll()
            )
            
            // Logout configuration
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout")
                .permitAll()
            )
            
            // Exception handling
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/403.html")
            );

        return http.build();
    }
}