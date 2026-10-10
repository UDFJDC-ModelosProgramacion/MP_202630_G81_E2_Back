package co.edu.udistrital.mdp.pets;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionRequestEntity;
import co.edu.udistrital.mdp.pets.entities.FollowUpEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.PhotoEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationRequestEntity;
import co.edu.udistrital.mdp.pets.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.pets.repositories.AdopterRepository;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRequestRepository;
import co.edu.udistrital.mdp.pets.repositories.FollowUpRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.PhotoRepository;
import co.edu.udistrital.mdp.pets.repositories.ShelterRepository;
import co.edu.udistrital.mdp.pets.repositories.TrialCohabitationRepository;
import co.edu.udistrital.mdp.pets.repositories.TrialCohabitationRequestRepository;
import co.edu.udistrital.mdp.pets.repositories.VeterinarianRepository;

@SpringBootApplication
public class MainApplication {

	public static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}

	/**
	 * Seeds a deterministic entity graph ONLY when the "integration-tests" profile
	 * is active (the profile wired in pom.xml for spring-boot:run and the
	 * integration-test executions). The graph matches the fixed ids referenced by
	 * the Postman collections (Adopter, FollowUp, Photo, TrialCohabitation) so the
	 * positive flows resolve their relationships without relying on controllers
	 * not yet implemented by other team members.
	 */
	@Bean
	@Profile("integration-tests")
	public CommandLineRunner integrationTestDataSeeder(VeterinarianRepository veterinarianRepository,
			AdopterRepository adopterRepository, ShelterRepository shelterRepository, PetRepository petRepository,
			PhotoRepository photoRepository, AdoptionRequestRepository adoptionRequestRepository,
			AdoptionRepository adoptionRepository, TrialCohabitationRequestRepository trialCohabitationRequestRepository,
			TrialCohabitationRepository trialCohabitationRepository, FollowUpRepository followUpRepository) {
		return args -> {
			if (shelterRepository.count() > 0)
				return;

			ShelterEntity shelter = new ShelterEntity();
			shelter.setName("Refugio Esperanza");
			shelter.setCity("Bogotá");
			shelter.setNit("900123456-7");
			shelter.setLocation("Calle 45 # 20-30");
			shelterRepository.save(shelter);

			VeterinarianEntity veterinarian = new VeterinarianEntity();
			veterinarian.setFirstName("Laura");
			veterinarian.setLastName("Gómez");
			veterinarian.setEmail("laura.gomez@pets.example");
			veterinarian.setPassword("secret123");
			veterinarian.setPhone("3001112233");
			veterinarian.setSpecialization("General");
			veterinarian.setAvailability("Lunes a viernes");
			veterinarianRepository.save(veterinarian);

			AdopterEntity adopterOne = new AdopterEntity();
			adopterOne.setFirstName("Ana");
			adopterOne.setLastName("Martínez");
			adopterOne.setEmail("ana.martinez@pets.example");
			adopterOne.setPassword("secret123");
			adopterOne.setPhone("3102223344");
			adopterOne.setAddress("Calle 10 # 5-20");
			adopterOne.setNationalId("CC-1011122233");
			adopterOne.setOccupation("Diseñadora");
			adopterOne.setEarnings(2500000.0);
			adopterOne.setHousingType("Apartamento");
			adopterOne.setAllergies("Ninguna");
			adopterOne.setHasChildren(true);
			adopterOne.setHasOtherPets(false);
			adopterRepository.save(adopterOne);

			AdopterEntity adopterTwo = new AdopterEntity();
			adopterTwo.setFirstName("Carlos");
			adopterTwo.setLastName("Pérez");
			adopterTwo.setEmail("carlos.perez@mail.com");
			adopterTwo.setPassword("secret123");
			adopterTwo.setPhone("3055556677");
			adopterTwo.setAddress("Carrera 15 # 20-30");
			adopterTwo.setNationalId("CC-1020304050");
			adopterTwo.setOccupation("Ingeniero");
			adopterTwo.setEarnings(3500000.0);
			adopterTwo.setHousingType("Casa");
			adopterTwo.setAllergies("Polvo");
			adopterTwo.setHasChildren(true);
			adopterTwo.setHasOtherPets(true);
			adopterRepository.save(adopterTwo);

			PetEntity petOne = new PetEntity();
			petOne.setName("Rocky");
			petOne.setSpecies("Perro");
			petOne.setBreed("Labrador");
			petOne.setAge(3);
			petOne.setSex("M");
			petOne.setSize("Grande");
			petOne.setHealthStatus("Saludable");
			petOne.setDescription("Perro amigable");
			petOne.setAdmissionDate(relativeDate(-400));
			petOne.setTemperament("Tranquilo");
			petOne.setSpecificNeeds("Ninguna");
			petOne.setCompatibilityChildren(true);
			petOne.setCompatibilityOtherPets(true);
			petOne.setActivityLevel("Alta");
			petOne.setRequiredSpace("Patio");
			petOne.setShelter(shelter);
			petRepository.save(petOne);

			PetEntity petTwo = new PetEntity();
			petTwo.setName("Luna");
			petTwo.setSpecies("Gato");
			petTwo.setBreed("Mestizo");
			petTwo.setAge(2);
			petTwo.setSex("H");
			petTwo.setSize("Pequeña");
			petTwo.setHealthStatus("Saludable");
			petTwo.setDescription("Gata independiente");
			petTwo.setAdmissionDate(relativeDate(-300));
			petTwo.setTemperament("Juguetona");
			petTwo.setSpecificNeeds("Ninguna");
			petTwo.setCompatibilityChildren(true);
			petTwo.setCompatibilityOtherPets(false);
			petTwo.setActivityLevel("Media");
			petTwo.setRequiredSpace("Dentro de casa");
			petTwo.setShelter(shelter);
			petRepository.save(petTwo);

			PetEntity petThree = new PetEntity();
			petThree.setName("Mimi");
			petThree.setSpecies("Gato");
			petThree.setBreed("Siamés");
			petThree.setAge(1);
			petThree.setSex("H");
			petThree.setSize("Pequeña");
			petThree.setHealthStatus("En recuperación");
			petThree.setDescription("Gatita en adopción sin fotografías");
			petThree.setAdmissionDate(relativeDate(-10));
			petThree.setTemperament("Tímida");
			petThree.setSpecificNeeds("Tratamiento dermatológico");
			petThree.setCompatibilityChildren(true);
			petThree.setCompatibilityOtherPets(true);
			petThree.setActivityLevel("Baja");
			petThree.setRequiredSpace("Dentro de casa");
			petThree.setShelter(shelter);
			petRepository.save(petThree);

			PhotoEntity photoOne = new PhotoEntity();
			photoOne.setUrl("https://example.com/fotos/rocky.jpg");
			photoOne.setType("JPG");
			photoOne.setDescription("Foto de perfil de la mascota");
			photoOne.setPet(petOne);
			photoOne.setShelter(shelter);
			photoRepository.save(photoOne);

			PhotoEntity photoTwo = new PhotoEntity();
			photoTwo.setUrl("https://example.com/fotos/refugio.jpg");
			photoTwo.setType("PNG");
			photoTwo.setDescription("Foto del refugio");
			photoTwo.setShelter(shelter);
			photoRepository.save(photoTwo);

			PhotoEntity photoThree = new PhotoEntity();
			photoThree.setUrl("https://example.com/fotos/luna.jpg");
			photoThree.setType("JPG");
			photoThree.setDescription("Foto de perfil de la mascota");
			photoThree.setPet(petTwo);
			photoRepository.save(photoThree);

			AdoptionRequestEntity requestOne = new AdoptionRequestEntity();
			requestOne.setStatus("APPROVED");
			requestOne.setDate(relativeDate(-400));
			requestOne.setDescription("Solicitud de adopción de Rocky");
			requestOne.setPet(petOne);
			requestOne.setShelter(shelter);
			requestOne.setAdopter(adopterOne);
			adoptionRequestRepository.save(requestOne);

			AdoptionRequestEntity requestTwo = new AdoptionRequestEntity();
			requestTwo.setStatus("APPROVED");
			requestTwo.setDate(relativeDate(-100));
			requestTwo.setDescription("Solicitud de adopción de Luna");
			requestTwo.setPet(petTwo);
			requestTwo.setShelter(shelter);
			requestTwo.setAdopter(adopterOne);
			adoptionRequestRepository.save(requestTwo);

			AdoptionEntity adoptionOne = new AdoptionEntity();
			adoptionOne.setDate(relativeDate(-365));
			adoptionOne.setStatus("FINALIZED");
			adoptionOne.setImportantNotes("Adopción finalizada exitosamente");
			adoptionOne.setPet(petOne);
			adoptionOne.setShelter(shelter);
			adoptionOne.setAdopter(adopterOne);
			adoptionOne.setAdoptionRequest(requestOne);
			adoptionRepository.save(adoptionOne);

			AdoptionEntity adoptionTwo = new AdoptionEntity();
			adoptionTwo.setDate(relativeDate(-30));
			adoptionTwo.setStatus("PENDING");
			adoptionTwo.setImportantNotes("En proceso");
			adoptionTwo.setPet(petTwo);
			adoptionTwo.setShelter(shelter);
			adoptionTwo.setAdopter(adopterOne);
			adoptionTwo.setAdoptionRequest(requestTwo);
			adoptionRepository.save(adoptionTwo);

			TrialCohabitationRequestEntity requestOneBis = buildTrialRequest(petOne, adopterOne, shelter);
			TrialCohabitationRequestEntity requestTwoBis = buildTrialRequest(petTwo, adopterOne, shelter);
			TrialCohabitationRequestEntity requestThree = buildTrialRequest(petTwo, adopterOne, shelter);
			TrialCohabitationRequestEntity requestFour = buildTrialRequest(petOne, adopterOne, shelter);
			TrialCohabitationRequestEntity requestFive = buildTrialRequest(petTwo, adopterOne, shelter);
			trialCohabitationRequestRepository.save(requestOneBis);
			trialCohabitationRequestRepository.save(requestTwoBis);
			trialCohabitationRequestRepository.save(requestThree);
			trialCohabitationRequestRepository.save(requestFour);
			trialCohabitationRequestRepository.save(requestFive);

			saveTrial(trialCohabitationRepository, "PENDING", adopterOne, shelter, requestFour);
			saveTrial(trialCohabitationRepository, "PENDING", adopterOne, shelter, requestFive);
			saveTrial(trialCohabitationRepository, "FINALIZED", adopterOne, shelter, requestThree);
			saveTrial(trialCohabitationRepository, "IN_PROGRESS", adopterOne, shelter, requestTwoBis);

			saveFollowUp(followUpRepository, veterinarian, adoptionOne, relativeDate(7), "Seguimiento inicial");
			saveFollowUp(followUpRepository, veterinarian, adoptionOne, relativeDate(30), "Seguimiento de control");
			saveFollowUp(followUpRepository, veterinarian, adoptionOne, relativeDate(-30), "Seguimiento cerrado");
		};
	}

	private static Date relativeDate(int daysFromToday) {
		return Date.from(LocalDate.now().plusDays(daysFromToday).atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	private static TrialCohabitationRequestEntity buildTrialRequest(PetEntity pet, AdopterEntity adopter,
			ShelterEntity shelter) {
		TrialCohabitationRequestEntity request = new TrialCohabitationRequestEntity();
		request.setStatus("APPROVED");
		request.setDate(relativeDate(-30));
		request.setDescription("Solicitud de convivencia");
		request.setStartDate(relativeDate(0));
		request.setEndDate(relativeDate(60));
		request.setPet(pet);
		request.setAdopter(adopter);
		request.setShelter(shelter);
		return request;
	}

	private static void saveTrial(TrialCohabitationRepository repository, String status, AdopterEntity adopter,
			ShelterEntity shelter, TrialCohabitationRequestEntity request) {
		TrialCohabitationEntity trial = new TrialCohabitationEntity();
		trial.setStatus(status);
		trial.setStartDate(relativeDate(11));
		trial.setEndDate(relativeDate(72));
		trial.setObservations(status.equals("FINALIZED") ? "Periodo finalizado" : "Periodo de adaptacion");
		trial.setAdopter(adopter);
		trial.setShelter(shelter);
		trial.setTrialCohabitationRequest(request);
		repository.save(trial);
	}

	private static void saveFollowUp(FollowUpRepository repository, VeterinarianEntity veterinarian,
			AdoptionEntity adoption, Date date, String observation) {
		FollowUpEntity followUp = new FollowUpEntity();
		followUp.setVeterinarian(veterinarian);
		followUp.setAdoption(adoption);
		followUp.setDate(date);
		followUp.setObservation(observation);
		repository.save(followUp);
	}
}