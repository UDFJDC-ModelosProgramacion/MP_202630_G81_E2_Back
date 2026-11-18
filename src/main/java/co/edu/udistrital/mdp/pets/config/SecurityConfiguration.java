package co.edu.udistrital.mdp.pets.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.udistrital.mdp.pets.exceptions.ApiError;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

	private final UserDetailsService userDetailsService;
	private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
	private final ObjectMapper objectMapper;

	@Value("${app.cors.allowed-origins:}")
	private String allowedOrigins;

	// Integration profile (spring-boot:run on 8999, newman collections, the front
	// without login) exposes the public API as the team designed it; any other
	// profile (including prod) keeps the full authentication rules.
	@Value("${app.security.public-api:false}")
	private boolean publicApi;

	@Bean
	AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
		return new org.springframework.security.authentication.ProviderManager(authenticationProvider);
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	CookieCsrfTokenRepository csrfTokenRepository() {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setHeaderName("X-XSRF-TOKEN");
		return repository;
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toList());
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
		cors.setAllowCredentials(true);
		cors.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", cors);
		return source;
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			CorsConfigurationSource corsConfigurationSource,
			CookieCsrfTokenRepository csrfTokenRepository,
			SecurityContextRepository securityContextRepository) throws Exception {
		AccessDeniedHandler accessDeniedHandler = (request, response, exception) ->
				writeError(response, HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(csrf -> {
					if (publicApi) {
						csrf.disable();
						return;
					}
					csrf
							.csrfTokenRepository(csrfTokenRepository)
							.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
							.withObjectPostProcessor(new ObjectPostProcessor<CsrfFilter>() {
								@Override
								public CsrfFilter postProcess(CsrfFilter filter) {
									filter.setAccessDeniedHandler(accessDeniedHandler);
									return filter;
								}
							});
				})
				.securityContext(context -> context.securityContextRepository(securityContextRepository))
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
						.sessionFixation(fixation -> fixation.changeSessionId()))
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint((request, response, exception) ->
								writeError(response, HttpStatus.UNAUTHORIZED, "Authentication is required"))
						.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(authorize -> {
					if (publicApi) {
						authorize.anyRequest().permitAll();
						return;
					}
					authorize
							.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
							.requestMatchers("/auth/csrf", "/auth/login").permitAll()
							.requestMatchers("/health").permitAll()
							.requestMatchers(HttpMethod.GET, "/pets", "/pets/**", "/shelters", "/shelters/**",
									"/veterinarians", "/veterinarians/**", "/vaccines", "/vaccines/**",
									"/reviews", "/pets/*/reviews/*", "/events", "/events/**").permitAll()
							.requestMatchers(HttpMethod.POST, "/users", "/adopters").permitAll()
							.anyRequest().authenticated();
				})
				.logout(logout -> logout
						.logoutUrl("/auth/logout")
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.deleteCookies("JSESSIONID")
						.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)));
		return http.build();
	}

	private void writeError(
			jakarta.servlet.http.HttpServletResponse response,
			HttpStatus status,
			String message) throws java.io.IOException {
		ApiError apiError = new ApiError(status);
		apiError.setMessage(message);
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), apiError);
	}
}
