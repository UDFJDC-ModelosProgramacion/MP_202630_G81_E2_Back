package co.edu.udistrital.mdp.pets.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udistrital.mdp.pets.dto.AuthenticationDTO;
import co.edu.udistrital.mdp.pets.dto.CsrfTokenDTO;
import co.edu.udistrital.mdp.pets.dto.LoginRequestDTO;
import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.exceptions.ApiError;
import co.edu.udistrital.mdp.pets.repositories.UserRepository;
import co.edu.udistrital.mdp.pets.security.AppUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final CsrfTokenRepository csrfTokenRepository;
	private final UserRepository userRepository;
	private final SecurityContextHolderStrategy securityContextHolderStrategy =
			SecurityContextHolder.getContextHolderStrategy();

	@GetMapping("/csrf")
	public CsrfTokenDTO csrf(CsrfToken csrfToken, HttpServletRequest request, HttpServletResponse response) {
		csrfTokenRepository.saveToken(csrfToken, request, response);
		return new CsrfTokenDTO(csrfToken.getToken());
	}

	@PostMapping("/login")
	public ResponseEntity<Object> login(
			@RequestBody LoginRequestDTO request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		if (request == null || request.getEmail() == null || request.getEmail().isBlank()
				|| request.getPassword() == null || request.getPassword().isBlank())
			return error(HttpStatus.BAD_REQUEST, "Email and password are required");

		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
			if (httpRequest.getSession(false) != null)
				httpRequest.changeSessionId();
			else
				httpRequest.getSession(true);

			SecurityContext context = securityContextHolderStrategy.createEmptyContext();
			context.setAuthentication(authentication);
			securityContextHolderStrategy.setContext(context);
			securityContextRepository.saveContext(context, httpRequest, httpResponse);

			AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
			return ResponseEntity.ok(authenticationDTO(principal));
		} catch (AuthenticationException exception) {
			return error(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		}
	}

	@GetMapping("/me")
	public AuthenticationDTO currentUser(@AuthenticationPrincipal AppUserPrincipal principal) {
		return authenticationDTO(principal);
	}

	private AuthenticationDTO authenticationDTO(AppUserPrincipal principal) {
		Long shelterId = userRepository.findById(principal.id())
				.map(UserEntity::getShelter)
				.map(shelter -> shelter.getId())
				.orElse(null);
		return new AuthenticationDTO(principal.id(), principal.email(), principal.role(), shelterId);
	}

	private ResponseEntity<Object> error(HttpStatus status, String message) {
		ApiError apiError = new ApiError(status);
		apiError.setMessage(message);
		return ResponseEntity.status(status).body(apiError);
	}
}
