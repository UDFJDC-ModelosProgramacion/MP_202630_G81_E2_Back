package co.edu.udistrital.mdp.pets.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.entities.UserRole;
import co.edu.udistrital.mdp.pets.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRequestRepository;
import co.edu.udistrital.mdp.pets.repositories.MessageRepository;
import co.edu.udistrital.mdp.pets.repositories.TrialCohabitationRepository;
import co.edu.udistrital.mdp.pets.repositories.UserRepository;
import co.edu.udistrital.mdp.pets.repositories.ShelterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

	private static final int MIN_PASSWORD_LENGTH = 8;
	private static final String ADMIN_ROLE = "ADMIN";

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
	private final ShelterRepository shelterRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public UserEntity createUser(UserEntity user) throws IllegalOperationException {
		log.info("Inicia proceso de creación del usuario");

		if (user == null || isBlank(user.getFirstName()) || isBlank(user.getLastName()) || isBlank(user.getEmail())
				|| isBlank(user.getPassword()))
			throw new IllegalOperationException("Required attributes cannot be null or empty");

		if (user.getPassword().length() < MIN_PASSWORD_LENGTH)
			throw new IllegalOperationException(
					"The password must meet the minimum security requirements (at least 8 characters)");

		if (userRepository.findAll().stream()
				.anyMatch(existing -> existing.getEmail() != null
						&& existing.getEmail().equalsIgnoreCase(user.getEmail())))
			throw new IllegalOperationException("The email address must be unique in the system");

		user.setId(null);
		user.setRole(UserRole.ADOPTER);
		user.setShelter(null);
		user.setPassword(passwordEncoder.encode(user.getPassword()));

		log.info("Termina proceso de creación del usuario");
		return userRepository.save(user);
	}

	@Transactional
	public UserEntity assignRole(Long id, UserRole role, Long shelterId, String password)
			throws EntityNotFoundException, IllegalOperationException {
		if (id == null || id <= 0)
			throw new IllegalOperationException(USER_ID_NOT_VALID);
		if (role == null)
			throw new IllegalOperationException("A role is required");
		UserEntity user = userRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));
		if ((role == UserRole.SHELTER_ADMIN || role == UserRole.VETERINARIAN) && shelterId == null)
			throw new IllegalOperationException("Shelter-scoped roles must be assigned to a shelter");
		if (role != UserRole.SHELTER_ADMIN && role != UserRole.VETERINARIAN && shelterId != null)
			throw new IllegalOperationException("Only shelter-scoped roles may be assigned to a shelter");
		if (role == UserRole.VETERINARIAN && !(user instanceof VeterinarianEntity))
			throw new IllegalOperationException("Only a veterinarian profile can receive the veterinarian role");
		if (role == UserRole.VETERINARIAN && (password == null || password.length() < MIN_PASSWORD_LENGTH))
			throw new IllegalOperationException("A veterinarian password of at least 8 characters is required");
		if (role != UserRole.VETERINARIAN && password != null)
			throw new IllegalOperationException("A password can only be set while assigning the veterinarian role");
		if (user.getRole() == UserRole.ADMIN && role != UserRole.ADMIN
				&& userRepository.countByRole(UserRole.ADMIN) <= 1)
			throw new IllegalOperationException("The last administrator cannot be demoted");

		user.setRole(role);
		user.setShelter(role == UserRole.SHELTER_ADMIN || role == UserRole.VETERINARIAN
				? shelterRepository.findById(shelterId)
						.orElseThrow(() -> new EntityNotFoundException("Shelter not found"))
				: null);
		if (role == UserRole.VETERINARIAN)
			user.setPassword(passwordEncoder.encode(password));
		return userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public UserEntity readUser(Long id) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar el usuario con id = {}", id);

		if (id == null || id <= 0)
			throw new IllegalOperationException(USER_ID_NOT_VALID);

		UserEntity user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

		log.info("Termina proceso de consultar el usuario con id = {}", id);
		return withoutSensitiveData(user);
	}

	@Transactional(readOnly = true)
	public List<UserEntity> readAllUsers(String currentUserRole) throws IllegalOperationException {
		return doReadAllUsers(currentUserRole, null, null, null, null);
	}

	@Transactional(readOnly = true)
	public List<UserEntity> readAllUsers(String currentUserRole, String firstName, String lastName, String email,
			Long shelterId) throws IllegalOperationException {
		return doReadAllUsers(currentUserRole, firstName, lastName, email, shelterId);
	}

	private List<UserEntity> doReadAllUsers(String currentUserRole, String firstName, String lastName, String email,
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

		boolean emailUsedByAnotherUser = userRepository.findAll().stream()
				.anyMatch(existing -> existing.getEmail() != null
						&& existing.getEmail().equalsIgnoreCase(userUpdate.getEmail())
						&& !existing.getId().equals(id));
		if (emailUsedByAnotherUser)
			throw new IllegalOperationException(
					"The email address cannot be updated to one that already belongs to another user");

		current.setFirstName(userUpdate.getFirstName());
		current.setLastName(userUpdate.getLastName());
		current.setEmail(userUpdate.getEmail());
		current.setPhone(userUpdate.getPhone());

		log.info("Termina proceso de actualizar el usuario con id = {}", id);
		return userRepository.save(current);
	}

	@Transactional
	public void deleteUser(Long id) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de eliminar el usuario con id = {}", id);

		if (id == null || id <= 0)
			throw new IllegalOperationException(USER_ID_NOT_VALID);

		UserEntity user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

		if (user.getRole() == UserRole.ADMIN && userRepository.countByRole(UserRole.ADMIN) <= 1)
			throw new IllegalOperationException("The last administrator cannot be deleted");

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