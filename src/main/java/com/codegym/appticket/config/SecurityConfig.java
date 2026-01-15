package com.codegym.appticket.config;

import com.codegym.appticket.service.impl.UserInforDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Autowired
    private UserInforDetailService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Enable CSRF for standard web application security
                // .csrf((csrf) -> csrf.disable()) 
                .authorizeHttpRequests((authorizeHttpRequests) ->
                        authorizeHttpRequests
                                .requestMatchers("/login", "/register", "/forgot-password", "/verify/**", "/error/**").permitAll()
                                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/webjars/**").permitAll()
                                .requestMatchers("/admin/**").hasAnyAuthority("ADMIN")
                                .anyRequest().authenticated())
                .formLogin((formLogin) ->
                        formLogin
                                .usernameParameter("email")
                                .passwordParameter("password")
                                .loginPage("/login")
                                .failureUrl("/login?error=true")
                                .loginProcessingUrl("/login")
                                .defaultSuccessUrl("/admin/users", true)
                                .permitAll())
                .rememberMe((remember) ->
                        remember
                                .key("UniqueAndSecretKeyForAppTicket")
                                .rememberMeParameter("remember")
                                .tokenValiditySeconds(7 * 24 * 60 * 60)
                                .userDetailsService(userDetailsService))
                .logout((logout) ->
                        logout.deleteCookies("JSESSIONID", "remember-me")
                                .invalidateHttpSession(true)
                                .logoutUrl("/logout")
                                .logoutSuccessUrl("/login?logout")
                                .permitAll())
                .exceptionHandling((exception) ->
                        exception
                                .accessDeniedPage("/403")
                );
        return http.build();
    }
}
