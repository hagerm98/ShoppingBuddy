package com.hager.shoppingbuddy.config;

import com.hager.shoppingbuddy.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@AllArgsConstructor
@EnableWebSecurity
public class WebSecurityConfig {
    private final UserService userService;

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/", "/home", "/about", "/contact", "/signup", "/login",
                            "/css/**", "/js/**", "/images/**", "/static/**"
                    ).permitAll();
                    auth.requestMatchers(
                            "/api/user/login", "/api/user/register", "/api/user/confirm", "/api/contact/submit"
                    ).permitAll();
                    auth.anyRequest().authenticated();
                })
                .userDetailsService(userService)
                .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/api/user/login")
                    .defaultSuccessUrl("/", true)
                    .failureUrl("/login?error=true")
                    .permitAll()
                )
                .rememberMe(remember -> remember
                    .userDetailsService(userService)
                    .tokenValiditySeconds(604800) // 1 Week
                    .key("shoppingBuddyRememberMeKey")
                )
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
