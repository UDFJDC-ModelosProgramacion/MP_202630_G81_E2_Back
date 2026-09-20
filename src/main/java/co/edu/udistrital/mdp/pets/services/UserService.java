package co.edu.udistrital.mdp.pets.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRequestRepository;
import co.edu.udistrital.mdp.pets.repositories.MessageRepository;
import co.edu.udistrital.mdp.pets.repositories.TrialCohabitationRepository;
import co.edu.udistrital.mdp.pets.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

	private static final int MIN_PASSWORD_LENGTH = 8;
	private static final String ADMIN_ROLE = "ADMIN";

	// Estados que se consideran "procesos de adopción activos" de un usuario
	private static final Set<String> ACTIVE_REQUEST_STATUSES = Set.of("PENDING", "APPROVED", "IN_PROGRESS");
	private static final Set<String> CLOSED_ADOPTION_STATUSES = Set.of("FINALIZED", "CANCELLED");
	private static final Set<String> ACTIVE_TRIAL_STATUSES = Set.of("PENDING", "IN_PROGRESS");

	private static final String USER_ID_NOT_VALID = "Invalid identifiers are not accepted";
	private static final String USER_NOT_FOUND = "User not found";

	private final UserRepository userRepository;
	private final MessageRepository messageRepository;
	private final AdoptionRequestRepository adoptionRequestRepository;
	private final AdoptionRepository adoptionRepository;
	private final TrialCohabitationRepository trialCohabitationRepository;

	/**
	 * Crea un usuario. Nombre, apellido, correo y contraseña son obligatorios; el
	 * correo debe ser único y la contraseña cumplir el mínimo de seguridad.
	 */
	@Transactional
	public UserEntity createUser(UserEntity user) throws IllegalOperationException {
		log.info("Inicia proceso de creación del usuario");

		if (user == null || isBlank(user.getFirstName()) || isBlank(user.getLastName()) || isBlank(user.getEmail())
				|| isBlank(user.getPassword()))
			throw new IllegalOperationException("Required attributes cannot be null or empty");

		if (user.getPassword().length() < MIN_PASSWORD_LENGTH)
			throw new IllegalOperationException(
					"The password must meet the minimum security requirements (at least 8 characters)");

		if (userRepository.findByEmail(user.getEmail()).isPresent())
			throw new IllegalOperationException("The email address must be unique in the system");

		user.setId(null);

		log.info("Termina proceso de creación del usuario");
		return userRepository.save(user);
	}

	/**
	 * Obtiene un usuario por su id sin datos sensibles (la contraseña no se
	 * devuelve). Se responde con una copia para no modificar la entidad gestionada.
	 */
	@Transactional(readOnly = true)
	public UserEntity readUser(Long id) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar el usuario con id = {}", id);

		if (id == null || id <= 0)
			throw new IllegalOperationException(USER_ID_NOT_VALID);

		UserEntity user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

		log.info("Termina proceso de consultar el usuario con id = {}", id);
		return withoutSensitiveData(user);
	}

	/**
	 * Lista todos los usuarios (solo administradores).
	 */
	@Transactional(readOnly = true)
	public List<UserEntity> readAllUsers(String currentUserRole) throws IllegalOperationException {
		return readAllUsers(currentUserRole, null, null, null, null);
	}

	/**
	 * Lista los usuarios (solo administradores) filtrando, de forma opcional, por
	 * nombre, apellido, correo y refugio. Si se combinan varios filtros, todos
	 * deben coincidir.
	 */
	@Transactional(readOnly = true)
	public List<UserEntity> readAllUsers(String currentUserRole, String firstName, String lastName, String email,
			Long shelterId) throws IllegalOperationException {
		log.info("Inicia proceso de consultar todos los usuarios");

		if (!ADMIN_ROLE.equalsIgnoreCase(currentUserRole))
			throw new IllegalOperationException(
					"The complete list of users is exclusively accessible to administrator roles");

		if ((firstName != null && firstName.isBlank()) || (lastName != null && lastName.isBlank())
				|| (email != null && email.isBlank()))
			throw new IllegalOperationException("Search filters cannot be empty");

		List<UserEntity> users = userRepository.findAll().stream()
				.filter(u -> matches(firstName, u.getFirstName()))
				.filter(u -> matches(lastName, u.getLastName()))
				.filter(u -> matches(email, u.getEmail()))
				.filter(u -> shelterId == null || (u.getShelter() != null && shelterId.equals(u.getShelter().getId())))
				.map(this::withoutSensitiveData)
				.toList();

		log.info("Termina proceso de consultar todos los usuarios");
		return users;
	}

	/**
	 * Actualiza los datos básicos de un usuario. El correo no puede pasar a ser el
	 * de otro usuario.
	 */
	@Transactional
	public UserEntity updateUser(Long id, UserEntity userUpdate)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar el usuario con id = {}", id);

		if (id == null || id <= 0)
			throw new IllegalOperationException(USER_ID_NOT_VALID);
		if (userUpdate == null || isBlank(userUpdate.getFirstName()) || isBlank(userUpdate.getLastName())
				|| isBlank(userUpdate.getEmail()))
			throw new IllegalOperationException("Null attributes are not accepted");

		UserEntity current = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

		Optional<UserEntity> userWithEmail = userRepository.findByEmail(userUpdate.getEmail());
		if (userWithEmail.isPresent() && !userWithEmail.get().getId().equals(id))
			throw new IllegalOperationException(
					"The email address cannot be updated to one that already belongs to another user");

		current.setFirstName(userUpdate.getFirstName());
		current.setLastName(userUpdate.getLastName());
		current.setEmail(userUpdate.getEmail());
		current.setPhone(userUpdate.getPhone());

		log.info("Termina proceso de actualizar el usuario con id = {}", id);
		return userRepository.save(current);
	}

	/**
	 * Elimina un usuario. No se puede eliminar un usuario con procesos de adopción
	 * activos (solicitudes, adopciones o convivencias de prueba) ni con mensajes
	 * registrados.
	 */
	@Transactional
	public void deleteUser(Long id) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de eliminar el usuario con id = {}", id);

		if (id == null || id <= 0)
			throw new IllegalOperationException(USER_ID_NOT_VALID);

		UserEntity user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

		if (hasActiveAdoptionProcesses(id))
			throw new IllegalOperationException("A user with active adoption processes cannot be deleted");

		if (!messageRepository.findBySendUserIdOrReceivesUserId(id, id).isEmpty())
			throw new IllegalOperationException("A user with registered messages cannot be deleted");

		userRepository.delete(user);
		log.info("Termina proceso de eliminar el usuario con id = {}", id);
	}

	private boolean hasActiveAdoptionProcesses(Long userId) {
		boolean activeRequest = adoptionRequestRepository.findAll().stream()
				.anyMatch(r -> r.getAdopter() != null && userId.equals(r.getAdopter().getId())
						&& r.getStatus() != null && ACTIVE_REQUEST_STATUSES.contains(r.getStatus().toUpperCase()));

		boolean activeAdoption = adoptionRepository.findAll().stream()
				.anyMatch(a -> a.getAdopter() != null && userId.equals(a.getAdopter().getId())
						&& (a.getStatus() == null || !CLOSED_ADOPTION_STATUSES.contains(a.getStatus().toUpperCase())));

		boolean activeTrial = trialCohabitationRepository.findAll().stream()
				.anyMatch(t -> t.getAdopter() != null && userId.equals(t.getAdopter().getId())
						&& t.getStatus() != null && ACTIVE_TRIAL_STATUSES.contains(t.getStatus().toUpperCase()));

		return activeRequest || activeAdoption || activeTrial;
	}

	/**
	 * Copia del usuario sin la contraseña; nunca se modifica la entidad gestionada
	 * para evitar que el cambio se persista por accidente.
	 */
	private UserEntity withoutSensitiveData(UserEntity source) {
		UserEntity copy = new UserEntity();
		copy.setId(source.getId());
		copy.setFirstName(source.getFirstName());
		copy.setLastName(source.getLastName());
		copy.setEmail(source.getEmail());
		copy.setPhone(source.getPhone());
		copy.setShelter(source.getShelter());
		return copy;
	}

	private static boolean matches(String filter, String value) {
		return filter == null || filter.equalsIgnoreCase(value);
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}