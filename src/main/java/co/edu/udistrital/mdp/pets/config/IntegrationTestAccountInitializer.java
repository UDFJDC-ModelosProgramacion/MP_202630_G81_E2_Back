package co.edu.udistrital.mdp.pets.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.entities.UserRole;
import co.edu.udistrital.mdp.pets.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

@Component
@Profile("integration-tests")
@RequiredArgsConstructor
public class IntegrationTestAccountInitializer {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${POSTMAN_ADMIN_EMAIL:}")
	private String adminEmail;

	@Value("${POSTMAN_ADMIN_PASSWORD:}")
	private String adminPassword;

	/**
	 * Runs as a ready-event listener ordered after the team seeds (orders 1-3) so
	 * the Newman account does not shift the fixed ids those seeds and the Postman
	 * collections rely on (veterinarian 1, adopter 2, adoption 1).
	 */
	@EventListener(ApplicationReadyEvent.class)
	@Order(100)
	public void initializeNewmanAccount() {
		if (adminEmail.isBlank() || adminPassword.length() < 8)
			throw new IllegalStateException(
					"POSTMAN_ADMIN_EMAIL and a POSTMAN_ADMIN_PASSWORD of at least 8 characters are required "
							+ "for the integration-tests profile");

		userRepository.findByEmailIgnoreCase(adminEmail).ifPresentOrElse(user -> {
			if (user.getRole() != UserRole.ADMIN)
				throw new IllegalStateException("The configured Newman account must have the ADMIN role");
		}, () -> {
			UserEntity admin = new UserEntity();
			admin.setFirstName("Newman");
			admin.setLastName("Integration");
			admin.setEmail(adminEmail);
			admin.setPassword(passwordEncoder.encode(adminPassword));
			admin.setRole(UserRole.ADMIN);
			userRepository.save(admin);
		});
	}
}
