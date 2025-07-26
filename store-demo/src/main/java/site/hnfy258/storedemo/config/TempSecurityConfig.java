package site.hnfy258.storedemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 临时安全配置 - 用于调试，完全禁用Spring Security
 * 使用方法：在application.properties中添加 spring.profiles.active=debug
 */
@Configuration
@EnableWebSecurity
@Profile("debug")
public class TempSecurityConfig {

    @Bean
    public SecurityFilterChain debugFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
        return http.build();
    }
}