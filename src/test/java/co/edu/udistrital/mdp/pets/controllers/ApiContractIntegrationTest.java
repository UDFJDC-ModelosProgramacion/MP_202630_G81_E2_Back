package co.edu.udistrital.mdp.pets.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.MedicalEventEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.AdopterRepository;
import co.edu.udistrital.mdp.pets.repositories.FollowUpRepository;
import co.edu.udistrital.mdp.pets.repositories.MedicalEventRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.UserRepository;
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
		followUpRepository.deleteAll();
		medicalEventRepository.deleteAll();
		adoptionRepository.deleteAll();
		petRepository.deleteAll();
		adopterRepository.deleteAll();
		veterinarianRepository.deleteAll();
		userRepository.deleteAll();
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
		AdoptionEntity adoption = new AdoptionEntity();
		adoption.setStatus("FINALIZED");
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

		mockMvc.perform(getApi("/adoptions/{id}/followUps", adoptionId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(followUpId));

		mockMvc.perform(getApi("/adoptions/{adoptionId}/followUps/{followUpId}", adoptionId, followUpId)
				.param("requesterId", veterinarian.getId().toString())
				.param("role", "VETERINARIAN"))
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
		return new com.fasterxml.jackson.databind.ObjectMapper()
				.readTree(result.getResponse().getContentAsString())
				.get("id").asLong();
	}

	private MockHttpServletRequestBuilder getApi(String path, Object... uriVariables) {
		return get(API + path, uriVariables).contextPath(API);
	}

	private MockHttpServletRequestBuilder postApi(String path, Object... uriVariables) {
		return post(API + path, uriVariables).contextPath(API);
	}

	private MockHttpServletRequestBuilder putApi(String path, Object... uriVariables) {
		return put(API + path, uriVariables).contextPath(API);
	}

	private MockHttpServletRequestBuilder deleteApi(String path, Object... uriVariables) {
		return delete(API + path, uriVariables).contextPath(API);
	}

}
