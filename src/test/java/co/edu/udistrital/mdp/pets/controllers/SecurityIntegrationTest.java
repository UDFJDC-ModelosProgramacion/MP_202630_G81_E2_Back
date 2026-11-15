package co.edu.udistrital.mdp.pets.controllers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.FollowUpEntity;
import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.entities.UserRole;
import co.edu.udistrital.mdp.pets.repositories.ShelterRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.FollowUpRepository;
import co.edu.udistrital.mdp.pets.repositories.VeterinarianRepository;
import co.edu.udistrital.mdp.pets.repositories.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

	private static final String API = "/api";
	private static final String PASSWORD = "SafePassphrase-2026";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShelterRepository shelterRepository;

	@Autowired
	private PetRepository petRepository;

	@Autowired
	private AdoptionRepository adoptionRepository;

	@Autowired
	private FollowUpRepository followUpRepository;

	@Autowired
	private VeterinarianRepository veterinarianRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void privateEndpointsRequireAuthenticationAndReturnConsistent401() throws Exception {
		mockMvc.perform(get(API + "/users/1").contextPath(API))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.apierror.message").value("Authentication is required"));
	}

	@Test
	void followUpQueryParametersCannotImpersonateAnAdopterOrAdministrator() throws Exception {
		AdopterEntity owner = persistAdopter("owner");
		AdopterEntity other = persistAdopter("other");
		AdoptionEntity adoption = new AdoptionEntity();
		adoption.setAdopter(other);
		adoption.setStatus("FINALIZED");
		adoption = adoptionRepository.saveAndFlush(adoption);
		FollowUpEntity followUp = new FollowUpEntity();
		followUp.setAdoption(adoption);
		followUp.setObservation("Private follow-up");
		followUp.setDate(new Date());
		followUp = followUpRepository.saveAndFlush(followUp);

		MockHttpSession ownerSession = login(owner.getEmail());
		mockMvc.perform(get(API + "/adoptions/{adoptionId}/followUps/{followUpId}",
				adoption.getId(), followUp.getId())
				.contextPath(API)
				.session(ownerSession)
				.param("requesterId", other.getId().toString())
				.param("role", "ADMIN"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.apierror.message")
						.value("You do not have permission to access this resource"));
	}

	@Test
	void logoutInvalidatesTheServerSession() throws Exception {
		UserEntity adopter = persistUser("logout", UserRole.ADOPTER, null);
		MockHttpSession session = login(adopter.getEmail());

		mockMvc.perform(post(API + "/auth/logout").contextPath(API).session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.apierror.message")
						.value("You do not have permission to access this resource"));
		mockMvc.perform(postApi("/auth/logout").session(session).with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get(API + "/users/{id}", adopter.getId()).contextPath(API).session(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginUsesBcryptAndNeverReturnsThePassword() throws Exception {
		String email = "registered-" + System.nanoTime() + "@example.test";
		String registration = """
				{
				  "firstName": "Registered",
				  "lastName": "User",
				  "email": "%s",
				  "phone": "3001234567",
				  "password": "%s"
				}
				""".formatted(email, PASSWORD);

		mockMvc.perform(postApi("/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registration))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.password").doesNotExist());

		UserEntity registered = userRepository.findByEmailIgnoreCase(email).orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals(UserRole.ADOPTER, registered.getRole());
		org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(PASSWORD, registered.getPassword()));

		mockMvc.perform(postApi("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"%s"}
						""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(registered.getId()))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value("ADOPTER"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void csrfEndpointProvidesTheTokenNeededForSpaRequests() throws Exception {
		mockMvc.perform(getApi("/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(cookie().exists("XSRF-TOKEN"));
	}

	@Test
	void administratorCreatesVeterinarianWithServerAssignedRoleAndHashedPassword() throws Exception {
		ShelterEntity shelter = persistShelter("vet");
		UserEntity admin = persistUser("vet-admin", UserRole.ADMIN, null);
		MockHttpSession adminSession = login(admin.getEmail());
		String email = "vet-" + System.nanoTime() + "@example.test";

		mockMvc.perform(postApi("/veterinarians")
				.session(adminSession)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "firstName":"Vet",
						  "lastName":"User",
						  "email":"%s",
						  "phone":"3001234567",
						  "specialization":"General",
						  "availability":"Weekdays",
						  "password":"%s",
						  "shelterId":%d
						}
						""".formatted(email, PASSWORD, shelter.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.password").doesNotExist());

		UserEntity veterinarian = userRepository.findByEmailIgnoreCase(email).orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals(UserRole.VETERINARIAN, veterinarian.getRole());
		org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(PASSWORD, veterinarian.getPassword()));
		org.junit.jupiter.api.Assertions.assertEquals(shelter.getId(), veterinarian.getShelter().getId());
		org.junit.jupiter.api.Assertions.assertTrue(veterinarianRepository.findById(veterinarian.getId()).isPresent());
	}

	@Test
	void registrationCannotChooseAPrivilegedRole() throws Exception {
		String email = "cannot-escalate-" + System.nanoTime() + "@example.test";
		mockMvc.perform(postApi("/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "firstName":"Regular",
						  "lastName":"User",
						  "email":"%s",
						  "phone":"3001234567",
						  "password":"%s",
						  "role":"ADMIN"
						}
						""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").doesNotExist())
				.andExpect(jsonPath("$.password").doesNotExist());

		UserEntity registered = userRepository.findByEmailIgnoreCase(email).orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals(UserRole.ADOPTER, registered.getRole());
	}

	@Test
	void onlyAnAdministratorCanUpdateSheltersAndShelterAdminsAreRestrictedToTheirOwnShelter()
			throws Exception {
		ShelterEntity first = persistShelter("first");
		ShelterEntity second = persistShelter("second");
		UserEntity admin = persistUser("admin", UserRole.ADMIN, null);
		UserEntity shelterAdmin = persistUser("shelter-admin", UserRole.SHELTER_ADMIN, first);
		UserEntity adopter = persistUser("adopter", UserRole.ADOPTER, null);

		MockHttpSession adminSession = login(admin.getEmail());
		mockMvc.perform(updateShelter(first, "Admin update").session(adminSession).with(csrf()))
				.andExpect(status().isOk());

		MockHttpSession adopterSession = login(adopter.getEmail());
		mockMvc.perform(updateShelter(first, "Adopter denied").session(adopterSession).with(csrf()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.apierror.message")
						.value("You do not have permission to access this resource"));

		MockHttpSession shelterSession = login(shelterAdmin.getEmail());
		mockMvc.perform(updateShelter(first, "Own shelter").session(shelterSession).with(csrf()))
				.andExpect(status().isOk());
		mockMvc.perform(updateShelter(second, "Other shelter").session(shelterSession).with(csrf()))
				.andExpect(status().isForbidden());

		PetEntity otherShelterPet = persistPet(second, "Other shelter pet");
		mockMvc.perform(putApi("/pets/{id}", otherShelterPet.getId())
				.session(shelterSession)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(petUpdateRequest(otherShelterPet.getName(), second.getId())))
				.andExpect(status().isForbidden());

		PetEntity ownedPet = persistPet(first, "Owned pet");
		mockMvc.perform(putApi("/pets/{id}", ownedPet.getId())
				.session(shelterSession)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(petUpdateRequest(ownedPet.getName(), second.getId())))
				.andExpect(status().isForbidden());

		UserEntity promoted = persistUser("promoted", UserRole.ADOPTER, null);
		mockMvc.perform(putApi("/users/{id}/role", promoted.getId())
				.session(adminSession)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"role":"SHELTER_ADMIN","shelterId":%d}
						""".formatted(first.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.password").doesNotExist());
		UserEntity reloaded = userRepository.findById(promoted.getId()).orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals(UserRole.SHELTER_ADMIN, reloaded.getRole());
		org.junit.jupiter.api.Assertions.assertEquals(first.getId(), reloaded.getShelter().getId());
	}

	private ShelterEntity persistShelter(String suffix) {
		ShelterEntity shelter = new ShelterEntity();
		shelter.setName("Shelter " + suffix);
		shelter.setCity("Bogota");
		shelter.setNit("NIT-" + suffix);
		shelter.setLocation("Address " + suffix);
		return shelterRepository.saveAndFlush(shelter);
	}

	private UserEntity persistUser(String suffix, UserRole role, ShelterEntity shelter) {
		UserEntity user = new UserEntity();
		user.setFirstName("Test");
		user.setLastName(suffix);
		user.setEmail(suffix + "-" + System.nanoTime() + "@example.test");
		user.setPassword(passwordEncoder.encode(PASSWORD));
		user.setRole(role);
		user.setShelter(shelter);
		return userRepository.saveAndFlush(user);
	}

	private AdopterEntity persistAdopter(String suffix) {
		AdopterEntity adopter = new AdopterEntity();
		adopter.setFirstName("Test");
		adopter.setLastName(suffix);
		adopter.setEmail(suffix + "-" + System.nanoTime() + "@example.test");
		adopter.setPassword(passwordEncoder.encode(PASSWORD));
		adopter.setRole(UserRole.ADOPTER);
		return (AdopterEntity) userRepository.saveAndFlush(adopter);
	}

	private PetEntity persistPet(ShelterEntity shelter, String name) {
		PetEntity pet = new PetEntity();
		pet.setName(name);
		pet.setSpecies("Perro");
		pet.setBreed("Criollo");
		pet.setAge(3);
		pet.setSex("Macho");
		pet.setSize("Mediano");
		pet.setHealthStatus("Saludable");
		pet.setAdmissionDate(Date.from(Instant.parse("2000-01-02T05:00:00Z")));
		pet.setCompatibilityChildren(true);
		pet.setCompatibilityOtherPets(true);
		pet.setActivityLevel("Alto");
		pet.setRequiredSpace("Patio");
		pet.setShelter(shelter);
		return petRepository.saveAndFlush(pet);
	}

	private String petUpdateRequest(String name, Long shelterId) {
		return """
				{
				  "name":"%s",
				  "species":"Perro",
				  "breed":"Criollo",
				  "age":4,
				  "sex":"Macho",
				  "size":"Mediano",
				  "healthStatus":"Saludable",
				  "admissionDate":"2000-01-02T00:00:00.000-05:00",
				  "compatibilityChildren":true,
				  "compatibilityOtherPets":true,
				  "activityLevel":"Alto",
				  "requiredSpace":"Patio",
				  "shelter":{"id":%d}
				}
				""".formatted(name, shelterId);
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult result = mockMvc.perform(postApi("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"%s"}
						""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private MockHttpServletRequestBuilder updateShelter(ShelterEntity shelter, String name) {
		return putApi("/shelters/{id}", shelter.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"%s","city":"Bogota","nit":"%s","location":"Address %s"}
						""".formatted(name, shelter.getNit(),
						shelter.getNit().substring("NIT-".length())));
	}

	private MockHttpServletRequestBuilder postApi(String path, Object... uriVariables) {
		return post(API + path, uriVariables).contextPath(API).with(csrf());
	}

	private MockHttpServletRequestBuilder getApi(String path, Object... uriVariables) {
		return get(API + path, uriVariables).contextPath(API);
	}

	private MockHttpServletRequestBuilder putApi(String path, Object... uriVariables) {
		return put(API + path, uriVariables).contextPath(API);
	}
}
