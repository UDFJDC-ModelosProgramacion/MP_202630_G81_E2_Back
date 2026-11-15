package co.edu.udistrital.mdp.pets.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.FollowUpEntity;
import co.edu.udistrital.mdp.pets.entities.MedicalEventEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ReviewEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.AdopterRepository;
import co.edu.udistrital.mdp.pets.repositories.FollowUpRepository;
import co.edu.udistrital.mdp.pets.repositories.MedicalEventRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.ReviewRepository;
import co.edu.udistrital.mdp.pets.repositories.ShelterRepository;
import co.edu.udistrital.mdp.pets.repositories.UserRepository;
import co.edu.udistrital.mdp.pets.repositories.VaccinationRecordRepository;
import co.edu.udistrital.mdp.pets.repositories.VeterinarianRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ApiContractIntegrationTest {

	private static final String API = "/api";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PetRepository petRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	@Autowired
	private ShelterRepository shelterRepository;

	@Autowired
	private VaccinationRecordRepository vaccinationRecordRepository;

	@Autowired
	private VeterinarianRepository veterinarianRepository;

	@Autowired
	private MedicalEventRepository medicalEventRepository;

	@Autowired
	private AdoptionRepository adoptionRepository;

	@Autowired
	private FollowUpRepository followUpRepository;

	@Autowired
	private AdopterRepository adopterRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void cleanDatabase() {
		reviewRepository.deleteAll();
		followUpRepository.deleteAll();
		medicalEventRepository.deleteAll();
		adoptionRepository.deleteAll();
		vaccinationRecordRepository.deleteAll();
		petRepository.deleteAll();
		shelterRepository.deleteAll();
		adopterRepository.deleteAll();
		veterinarianRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void vaccinationRecordCanBeCreatedFromPetIdPayload() throws Exception {
		PetEntity pet = petRepository.saveAndFlush(new PetEntity());
		String request = """
				{"pet":{"id":%d}}
				""".formatted(pet.getId());

		mockMvc.perform(getApi("/pets"))
				.andExpect(status().isOk());

		MvcResult result = mockMvc.perform(postApi("/vaccinationRecords")
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
				.andReturn();

		assertEquals(201, result.getResponse().getStatus(),
				"Unexpected response: " + result.getResponse().getContentAsString()
						+ "; resolved exception: " + result.getResolvedException());
	}

	@Test
	void reviewListAndDetailIncludeOnlyTheAdopterAuthorSummary() throws Exception {
		PetEntity pet = petRepository.saveAndFlush(new PetEntity());
		AdopterEntity adopter = new AdopterEntity();
		adopter.setFirstName("Ana");
		adopter.setLastName("Gomez");
		adopter.setEmail("review-author-" + System.nanoTime() + "@example.test");
		adopter.setNationalId("NID-" + System.nanoTime());
		adopter.setPhone("3001234567");
		adopter = adopterRepository.saveAndFlush(adopter);

		AdoptionEntity adoption = new AdoptionEntity();
		adoption.setPet(pet);
		adoption.setAdopter(adopter);
		adoption.setStatus("FINALIZED");
		adoption = adoptionRepository.saveAndFlush(adoption);

		ReviewEntity review = new ReviewEntity();
		review.setPet(pet);
		review.setAdoption(adoption);
		review.setRating(5);
		review.setComment("Excelente experiencia");
		review.setDate(new java.util.Date());
		review.setTime(new java.util.Date());
		review = reviewRepository.saveAndFlush(review);

		MvcResult listResult = mockMvc.perform(getApi("/reviews")
				.param("adopterId", adopter.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].author.id").value(adopter.getId()))
				.andExpect(jsonPath("$[0].author.displayName").value("Ana Gomez"))
				.andReturn();
		JsonNode listedAuthor = new ObjectMapper()
				.readTree(listResult.getResponse().getContentAsString())
				.get(0).get("author");
		assertEquals(Set.of("id", "displayName"), fieldNames(listedAuthor));

		MvcResult detailResult = mockMvc.perform(getApi("/pets/{petId}/reviews/{reviewId}",
				pet.getId(), review.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.author.id").value(adopter.getId()))
				.andExpect(jsonPath("$.author.displayName").value("Ana Gomez"))
				.andReturn();
		JsonNode detailAuthor = new ObjectMapper()
				.readTree(detailResult.getResponse().getContentAsString())
				.get("author");
		assertEquals(Set.of("id", "displayName"), fieldNames(detailAuthor));
	}

	@Test
	void shelterUpdateAndDeleteKeepExistingContractAndProtectPetAssociations() throws Exception {
		String suffix = Long.toString(System.nanoTime());
		ShelterEntity shelter = persistShelter(suffix);
		Long shelterId = shelter.getId();
		String updateRequest = """
				{
				  "name": "Refugio actualizado %s",
				  "city": "Medellin",
				  "nit": "%s",
				  "location": "Laureles"
				}
				""".formatted(suffix, shelter.getNit());

		mockMvc.perform(putApi("/shelters/{id}", shelterId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateRequest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(shelterId))
				.andExpect(jsonPath("$.name").value("Refugio actualizado " + suffix))
				.andExpect(jsonPath("$.city").value("Medellin"))
				.andExpect(jsonPath("$.nit").value(shelter.getNit()))
				.andExpect(jsonPath("$.location").value("Laureles"));

		mockMvc.perform(putApi("/shelters/{id}", shelterId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateRequest.replace(shelter.getNit(), "OTHER-" + suffix)))
				.andExpect(status().isPreconditionFailed())
				.andExpect(jsonPath("$.apierror.message")
						.value("El NIT no se puede modificar una vez creado el refugio"));

		mockMvc.perform(putApi("/shelters/{id}", shelterId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateRequest.replace("\"city\": \"Medellin\"", "\"city\": \"\"")))
				.andExpect(status().isPreconditionFailed())
				.andExpect(jsonPath("$.apierror.message").value("La ciudad no puede ser nula ni estar vacía"));

		mockMvc.perform(putApi("/shelters/{id}", 999999L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateRequest))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.apierror.message").value("Refugio no encontrado"));

		persistPet(shelter, "Mascota asociada " + suffix);
		mockMvc.perform(deleteApi("/shelters/{id}", shelterId))
				.andExpect(status().isPreconditionFailed())
				.andExpect(jsonPath("$.apierror.message")
						.value("No se puede eliminar un refugio que tiene mascotas bajo su cuidado"));

		ShelterEntity emptyShelter = persistShelter("empty-" + suffix);
		mockMvc.perform(deleteApi("/shelters/{id}", emptyShelter.getId()))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		mockMvc.perform(getApi("/shelters/{id}", emptyShelter.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.apierror.message").value("Refugio no encontrado"));
	}

	@Test
	void petUpdateAndDeleteKeepShelterAssociationRulesAndHistoryGuards() throws Exception {
		String suffix = Long.toString(System.nanoTime());
		ShelterEntity currentShelter = persistShelter("current-" + suffix);
		ShelterEntity newShelter = persistShelter("new-" + suffix);
		PetEntity pet = persistPet(currentShelter, "Mascota original " + suffix);
		String updateRequest = petUpdateRequest("Mascota actualizada " + suffix,
				newShelter.getId(), "2000-01-02T00:00:00.000-05:00");

		mockMvc.perform(putApi("/pets/{id}", pet.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateRequest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(pet.getId()))
				.andExpect(jsonPath("$.name").value("Mascota actualizada " + suffix))
				.andExpect(jsonPath("$.shelter.id").value(newShelter.getId()))
				.andExpect(jsonPath("$.admissionDate").value(org.hamcrest.Matchers.startsWith("2000-01-02")));

		mockMvc.perform(putApi("/pets/{id}", pet.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(petUpdateRequest("Mascota actualizada " + suffix, newShelter.getId(),
						"2000-02-02T00:00:00.000-05:00")))
				.andExpect(status().isPreconditionFailed())
				.andExpect(jsonPath("$.apierror.message").value("La fecha de ingreso no se puede modificar"));

		mockMvc.perform(putApi("/pets/{id}", pet.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(petUpdateRequest("Mascota actualizada " + suffix, 999999L,
						"2000-01-02T00:00:00.000-05:00")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.apierror.message").value("No se encontró el refugio con el id indicado"));

		mockMvc.perform(putApi("/pets/{id}", 999999L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateRequest))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.apierror.message").value("No se encontró la mascota con el id indicado"));

		mockMvc.perform(putApi("/pets/{id}", pet.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(petUpdateRequest("", newShelter.getId(),
						"2000-01-02T00:00:00.000-05:00")))
				.andExpect(status().isPreconditionFailed())
				.andExpect(jsonPath("$.apierror.message")
						.value("El nombre de la mascota no puede ser nulo ni estar vacío"));

		PetEntity persistedPet = petRepository.findById(pet.getId()).orElseThrow();
		AdoptionEntity activeAdoption = new AdoptionEntity();
		activeAdoption.setPet(persistedPet);
		activeAdoption.setShelter(newShelter);
		activeAdoption.setStatus("IN_PROGRESS");
		adoptionRepository.saveAndFlush(activeAdoption);

		mockMvc.perform(deleteApi("/pets/{id}", pet.getId()))
				.andExpect(status().isPreconditionFailed())
				.andExpect(jsonPath("$.apierror.message")
						.value("No se puede eliminar una mascota con una adopción o convivencia de prueba activa"));

		PetEntity disposablePet = persistPet(currentShelter, "Mascota para eliminar " + suffix);
		mockMvc.perform(deleteApi("/pets/{id}", disposablePet.getId()))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		mockMvc.perform(getApi("/pets/{id}", disposablePet.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.apierror.message").value("No se encontró la mascota con el id indicado"));
	}

	@Test
	void medicalEventRoutesCreateFilterUpdateAndDelete() throws Exception {
		PetEntity pet = petRepository.saveAndFlush(new PetEntity());
		VeterinarianEntity veterinarian = veterinarianRepository.saveAndFlush(new VeterinarianEntity());
		String eventJson = """
				{
				  "date": "2000-01-02T00:00:00.000-05:00",
				  "type": "Control",
				  "description": "Revisión general",
				  "pet": {"id": %d},
				  "veterinarian": {"id": %d}
				}
				""".formatted(pet.getId(), veterinarian.getId());

		MvcResult created = mockMvc.perform(postApi("/medicalEvents")
				.contentType(MediaType.APPLICATION_JSON).content(eventJson))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.pet.id").value(pet.getId()))
				.andExpect(jsonPath("$.veterinarian.id").value(veterinarian.getId()))
				.andReturn();
		Long eventId = jsonId(created);

		mockMvc.perform(getApi("/medicalEvents")
				.param("petId", pet.getId().toString())
				.param("veterinarianId", veterinarian.getId().toString())
				.param("type", "control")
				.param("date", "2000-01-02"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(eventId));

		mockMvc.perform(getApi("/medicalEvents/{id}", eventId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pet.id").value(pet.getId()));

		mockMvc.perform(putApi("/medicalEvents/{id}", eventId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(eventJson.replace("Control", "Vacunación")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type").value("Vacunación"));

		mockMvc.perform(getApi("/medicalEvents").param("date", "no-es-fecha"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.apierror.message").isNotEmpty());

		mockMvc.perform(deleteApi("/medicalEvents/{id}", eventId))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		mockMvc.perform(getApi("/pets/{petId}/medicalEvents", pet.getId()))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	@Test
	void adopterRoutesKeepIdentityImmutableAndDoNotReturnPassword() throws Exception {
		String email = "adopter-" + System.nanoTime() + "@example.test";
		String nationalId = "NID-" + System.nanoTime();
		String request = """
				{
				  "firstName": "Ana",
				  "lastName": "Gomez",
				  "email": "%s",
				  "password": "secret123",
				  "phone": "3001234567",
				  "address": "Calle 1",
				  "nationalId": "%s",
				  "occupation": "Docente",
				  "earnings": 2500000,
				  "housingType": "Casa",
				  "allergies": "Ninguna",
				  "hasChildren": false,
				  "hasOtherPets": true
				}
				""".formatted(email, nationalId);
		Long adopterId = jsonId(mockMvc.perform(postApi("/adopters")
				.contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.password").doesNotExist())
				.andReturn());

		mockMvc.perform(getApi("/adopters").param("nationalId", nationalId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(adopterId))
				.andExpect(jsonPath("$[0].password").doesNotExist());

		mockMvc.perform(getApi("/adopters/{id}", adopterId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nationalId").value(nationalId))
				.andExpect(jsonPath("$.password").doesNotExist());

		String update = """
				{
				  "address": "Carrera 2",
				  "occupation": "Veterinaria",
				  "earnings": 3000000,
				  "housingType": "Apartamento",
				  "allergies": "Polen",
				  "hasChildren": true,
				  "hasOtherPets": false
				}
				""";
		mockMvc.perform(putApi("/adopters/{id}", adopterId)
				.contentType(MediaType.APPLICATION_JSON).content(update))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.address").value("Carrera 2"))
				.andExpect(jsonPath("$.nationalId").value(nationalId))
				.andExpect(jsonPath("$.password").doesNotExist());

		mockMvc.perform(deleteApi("/adopters/{id}", adopterId))
				.andExpect(status().isNoContent());
	}

	@Test
	void followUpRoutesCheckAdoptionMembershipAndHonorServiceAuthorization() throws Exception {
		VeterinarianEntity veterinarian = veterinarianRepository.saveAndFlush(new VeterinarianEntity());
		ShelterEntity shelter = shelterRepository.saveAndFlush(new ShelterEntity());
		PetEntity pet = new PetEntity();
		pet.setShelter(shelter);
		pet = petRepository.saveAndFlush(pet);

		AdopterEntity adopter = new AdopterEntity();
		adopter.setFirstName("Test");
		adopter.setLastName("Adopter");
		adopter.setEmail("follow-up-test-" + System.nanoTime() + "@example.test");
		adopter = adopterRepository.saveAndFlush(adopter);

		AdoptionEntity adoption = new AdoptionEntity();
		adoption.setStatus("FINALIZED");
		adoption.setAdopter(adopter);
		adoption.setPet(pet);
		adoption.setShelter(shelter);
		adoption = adoptionRepository.saveAndFlush(adoption);
		AdoptionEntity otherAdoption = new AdoptionEntity();
		otherAdoption.setStatus("FINALIZED");
		otherAdoption = adoptionRepository.saveAndFlush(otherAdoption);

		String request = """
				{
				  "date": "2099-12-30",
				  "observation": "Seguimiento inicial",
				  "veterinarian": {"id": %d}
				}
				""".formatted(veterinarian.getId());
		Long adoptionId = adoption.getId();
		Long followUpId = jsonId(mockMvc.perform(postApi("/adoptions/{id}/followUps", adoptionId)
				.contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.observation").value("Seguimiento inicial"))
				.andReturn());
		FollowUpEntity persisted = followUpRepository.findById(followUpId).orElseThrow();
		assertEquals(adoptionId, persisted.getAdoption().getId());

		mockMvc.perform(getApi("/adoptions/{id}/followUps", adoptionId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(followUpId));

		mockMvc.perform(getApi("/adoptions/{adoptionId}/followUps/{followUpId}", adoptionId, followUpId)
				.param("requesterId", veterinarian.getId().toString())
				.param("role", "VETERINARIAN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(followUpId));

		mockMvc.perform(getApi("/adoptions/{adoptionId}/followUps/{followUpId}", adoptionId, followUpId)
				.param("requesterId", adopter.getId().toString())
				.param("role", "ADOPTER"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(followUpId));

		mockMvc.perform(getApi("/adoptions/{adoptionId}/followUps/{followUpId}", adoptionId, followUpId)
				.param("requesterId", "999999")
				.param("role", "ADOPTER"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(followUpId));

		mockMvc.perform(getApi("/adoptions/{adoptionId}/followUps/{followUpId}",
				otherAdoption.getId(), followUpId)
				.param("role", "ADMIN"))
				.andExpect(status().isPreconditionFailed())
				.andExpect(jsonPath("$.apierror.message").value(
						"The follow-up is not associated with the adoption"));

		mockMvc.perform(putApi("/adoptions/{adoptionId}/followUps/{followUpId}",
				adoptionId, followUpId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"date":"2099-12-31","observation":"Actualizado"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.observation").value("Actualizado"));

		mockMvc.perform(deleteApi("/adoptions/{adoptionId}/followUps/{followUpId}",
				adoptionId, followUpId))
				.andExpect(status().isNoContent());

		mockMvc.perform(getApi("/adoptions/{id}/followUps", 999999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.apierror.message").isNotEmpty());
	}

	@Test
	void userRoutesAcceptCreationPasswordButNeverReturnIt() throws Exception {
		String email = "user-" + System.nanoTime() + "@example.test";
		Long userId = jsonId(mockMvc.perform(postApi("/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "firstName":"Maria",
						  "lastName":"Lopez",
						  "email":"%s",
						  "password":"StrongPass123",
						  "phone":"3001112233"
						}
						""".formatted(email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.password").doesNotExist())
				.andReturn());

		mockMvc.perform(getApi("/users/{id}", userId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.password").doesNotExist());

		mockMvc.perform(putApi("/users/{id}", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"firstName":"Maria Elena","lastName":"Lopez","email":"%s","phone":"3009998877"}
						""".formatted(email)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Maria Elena"))
				.andExpect(jsonPath("$.password").doesNotExist());

		mockMvc.perform(deleteApi("/users/{id}", userId))
				.andExpect(status().isNoContent());
	}

	@Test
	void healthIsExplicitAndUnknownRoutesReturnApiErrorNotSuccess() throws Exception {
		mockMvc.perform(getApi("/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("OK"));

		mockMvc.perform(getApi("/this-route-does-not-exist"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.apierror.message").isNotEmpty());
	}

	@Test
	void malformedJsonUsesTheStandardApiErrorShape() throws Exception {
		mockMvc.perform(postApi("/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.apierror.message").isNotEmpty());
	}

	private Long jsonId(MvcResult result) throws Exception {
		return new ObjectMapper()
				.readTree(result.getResponse().getContentAsString())
				.get("id").asLong();
	}

	private Set<String> fieldNames(JsonNode node) {
		Set<String> fields = new HashSet<>();
		node.fieldNames().forEachRemaining(fields::add);
		return fields;
	}

	private ShelterEntity persistShelter(String suffix) {
		ShelterEntity shelter = new ShelterEntity();
		shelter.setName("Refugio " + suffix);
		shelter.setCity("Bogota");
		shelter.setNit("NIT-" + suffix);
		shelter.setLocation("Direccion " + suffix);
		return shelterRepository.saveAndFlush(shelter);
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

	private String petUpdateRequest(String name, Long shelterId, String admissionDate) {
		return """
				{
				  "name": "%s",
				  "species": "Perro",
				  "breed": "Criollo",
				  "age": 4,
				  "sex": "Macho",
				  "size": "Mediano",
				  "healthStatus": "En tratamiento",
				  "description": "Descripcion actualizada",
				  "temperament": "Sociable",
				  "specificNeeds": "Ninguna",
				  "compatibilityChildren": true,
				  "compatibilityOtherPets": true,
				  "activityLevel": "Alto",
				  "requiredSpace": "Patio",
				  "admissionDate": "%s",
				  "shelter": {"id": %d}
				}
				""".formatted(name, admissionDate, shelterId);
	}

	private MockHttpServletRequestBuilder getApi(String path, Object... uriVariables) {
		return authenticated(get(API + path, uriVariables).contextPath(API));
	}

	private MockHttpServletRequestBuilder postApi(String path, Object... uriVariables) {
		return authenticated(post(API + path, uriVariables).contextPath(API));
	}

	private MockHttpServletRequestBuilder putApi(String path, Object... uriVariables) {
		return authenticated(put(API + path, uriVariables).contextPath(API));
	}

	private MockHttpServletRequestBuilder deleteApi(String path, Object... uriVariables) {
		return authenticated(delete(API + path, uriVariables).contextPath(API));
	}

	private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
		return request.with(user("contract-admin").roles("ADMIN")).with(csrf());
	}

}
