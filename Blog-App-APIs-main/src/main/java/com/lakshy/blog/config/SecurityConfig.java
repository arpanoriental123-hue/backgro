package com.lakshy.blog.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.lakshy.blog.security.CustomAccessDeniedHandler;
import com.lakshy.blog.security.CustomUserDetailService;
import com.lakshy.blog.security.JwtAuthenticationEntryPoint;
import com.lakshy.blog.security.JwtAuthenticationFilter;
import com.lakshy.blog.security.RateLimitingFilter;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableWebMvc
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
	
	public static final String[] PUBLIC_URLS = {
			"/api/auth/**",
			"/v3/api-docs",
			"/v3/api-docs/**",
			"/v2/api-docs",
			"/swagger-resources/**",
			"/swagger-ui/**",
			"/webjars/**"
	};
	
	@Autowired
	private CustomUserDetailService customUserDetailService;
	
	@Autowired
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	
	@Autowired
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Autowired
	private RateLimitingFilter rateLimitingFilter;

	@Autowired
	private CustomAccessDeniedHandler customAccessDeniedHandler;

	@Value("${cors.allowed-origins:http://localhost:3000}")
	private String allowedOriginsRaw;

	/**
	 * Set server.require-ssl=true when the application terminates TLS directly.
	 * Leave false (default) when HTTPS is enforced at the reverse-proxy / load-balancer level.
	 */
	@Value("${server.require-ssl:false}")
	private boolean requireSsl;


	@Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors().and()          // enable CORS using the bean below
            .csrf()
            .disable()
            .authorizeHttpRequests()
            .antMatchers(PUBLIC_URLS).permitAll()
            .antMatchers(HttpMethod.GET, "/api/auth/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/posts").permitAll()
            .antMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/categories").permitAll()
            // Individual user profile is public (returns PublicUserDto — no PII).
            // Full user list is restricted to ADMIN via @PreAuthorize on the controller method.
            .antMatchers(HttpMethod.GET, "/api/users/*").permitAll()
            .anyRequest().authenticated()
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(this.jwtAuthenticationEntryPoint)
            .accessDeniedHandler(this.customAccessDeniedHandler)
            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        if (requireSsl) {
            http.requiresChannel().anyRequest().requiresSecure();
        }
        
        http
        	.addFilterBefore(this.rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        http
        	.addFilterBefore(this.jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
		
	
	/* 
	 * === No Need for authenticationManager in Spring Security >= 5.7.2 === 
	 * https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter
	 * It is also no longer necessary to manually set UserDetailsService implementation in AuthenticationManager instance, 
	 * it only needs to exist in the spring context (example is done with the help of annotations to create beans).
	 */
//	@Bean
//    AuthenticationManager authenticationManager(AuthenticationManagerBuilder builder) throws Exception {
//        return builder.userDetailsService(this.customUserDetailService).passwordEncoder(passwordEncoder()).and().build();
//    }
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
		configuration.setAllowedOrigins(origins);
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
		configuration.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
