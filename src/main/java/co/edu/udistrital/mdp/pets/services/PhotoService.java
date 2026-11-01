package co.edu.udistrital.mdp.pets.services;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.PhotoEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.PhotoRepository;
import co.edu.udistrital.mdp.pets.repositories.ShelterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

@Slf4j
@Service
@RequiredArgsConstructor 
public class PhotoService {

	private static final Set<String> VALID_FORMATS = Set.of("JPG", "JPEG", "PNG", "GIF", "WEBP", "BMP");
	private static final String FINALIZED_STATUS = "FINALIZED";

	private static final String PHOTO_NOT_FOUND = "Photo not found";
	private static final String PHOTO_ID_NOT_VALID = "Photo id is not valid";
	private static final String PET_NOT_FOUND = "Pet not found";

	private final PhotoRepository photoRepository;
	private final PetRepository petRepository;
	private final ShelterRepository shelterRepository;
	private final AdoptionRepository adoptionRepository;

	@Transactional
	public PhotoEntity createPhoto(PhotoEntity photo) throws EntityNotFoundException, IllegalOperationException {
		return persistPhoto(photo);
	}

	@Transactional
	public PhotoEntity createPetPhoto(Long petId, PhotoEntity photo)
			throws EntityNotFoundException, IllegalOperationException {
		photo.setPet(findPetOrThrow(petId));
		photo.setShelter(null);
		return persistPhoto(photo);
	}

	@Transactional(readOnly = true)
	public List<PhotoEntity> getPetPhotos(Long petId)
			throws EntityNotFoundException, IllegalOperationException {
		findPetOrThrow(petId);
		return photoRepository.findAll().stream()
				.filter(photo -> photo.getPet() != null && petId.equals(photo.getPet().getId()))
				.toList();
	}

	@Transactional(readOnly = true)
	public PhotoEntity getPetPhoto(Long petId, Long photoId)
			throws EntityNotFoundException, IllegalOperationException {
		findPetOrThrow(petId);
		PhotoEntity photo = findPhotoOrThrow(photoId);
		validatePhotoBelongsToPet(photo, petId);
		return photo;
	}

	@Transactional
	public PhotoEntity updatePetPhoto(Long petId, Long photoId, PhotoEntity photo)
			throws EntityNotFoundException, IllegalOperationException {
		findPetOrThrow(petId);
		validatePhotoBelongsToPet(findPhotoOrThrow(photoId), petId);
		return applyPhotoUpdate(photoId, photo);
	}

	@Transactional
	public void deletePetPhoto(Long petId, Long photoId)
			throws EntityNotFoundException, IllegalOperationException {
		findPetOrThrow(petId);
		validatePhotoBelongsToPet(findPhotoOrThrow(photoId), petId);
		removePhoto(photoId);
	}

	private PhotoEntity persistPhoto(PhotoEntity photo) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación de la foto");

		validatePhotoData(photo);

		if (photo.getPet() == null && photo.getShelter() == null)
			throw new IllegalOperationException("Photo must be associated with an existing pet, shelter or user");

		attachToPet(photo);
		attachToShelter(photo);

		log.info("Termina proceso de creación de la foto");
		return photoRepository.save(photo);
	}

	private PetEntity findPetOrThrow(Long petId) throws EntityNotFoundException, IllegalOperationException {
		if (petId == null || petId <= 0)
			throw new IllegalOperationException("Pet id is not valid");
		return petRepository.findById(petId)
				.orElseThrow(() -> new EntityNotFoundException(PET_NOT_FOUND));
	}

	private PhotoEntity findPhotoOrThrow(Long photoId) throws EntityNotFoundException, IllegalOperationException {
		if (photoId == null || photoId <= 0)
			throw new IllegalOperationException(PHOTO_ID_NOT_VALID);
		return photoRepository.findById(photoId)
				.orElseThrow(() -> new EntityNotFoundException(PHOTO_NOT_FOUND));
	}

	private PhotoEntity applyPhotoUpdate(Long photoId, PhotoEntity photo)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la foto con id = {}", photoId);
		PhotoEntity current = findPhotoOrThrow(photoId);

		if (photo.getUrl() == null || photo.getUrl().isBlank())
			throw new IllegalOperationException("The new photo url cannot be null or empty");
		if (photo.getType() == null || photo.getType().isBlank())
			throw new IllegalOperationException("Photo format cannot be null or empty");
		if (!VALID_FORMATS.contains(photo.getType().toUpperCase()))
			throw new IllegalOperationException("Photo format is not valid; use JPG, PNG or another supported format");

		current.setUrl(photo.getUrl());
		current.setType(photo.getType());
		current.setDescription(photo.getDescription());

		log.info("Termina proceso de actualizar la foto con id = {}", photoId);
		return photoRepository.save(current);
	}

	private void removePhoto(Long photoId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar la foto con id = {}", photoId);
		PhotoEntity current = findPhotoOrThrow(photoId);

		if (current.getPet() != null && isMainPhotoOfActivePet(current))
			throw new IllegalOperationException(
					"The main profile photo of a pet active for adoption cannot be deleted without establishing another default image first");

		photoRepository.deleteById(photoId);
		log.info("Termina proceso de borrar la foto con id = {}", photoId);
	}

	private void validatePhotoBelongsToPet(PhotoEntity photo, Long petId) throws IllegalOperationException {
		if (photo.getPet() == null || !petId.equals(photo.getPet().getId()))
			throw new IllegalOperationException("The photo is not associated with the pet");
	}

	private void validatePhotoData(PhotoEntity photo) throws IllegalOperationException {
		if (photo.getUrl() == null || photo.getUrl().isBlank())
			throw new IllegalOperationException("Photo url cannot be null or empty");

		if (photo.getType() == null || photo.getType().isBlank())
			throw new IllegalOperationException("Photo format cannot be null or empty");
		if (!VALID_FORMATS.contains(photo.getType().toUpperCase()))
			throw new IllegalOperationException("Photo format is not valid; use JPG, PNG or another supported format");
	}

	private void attachToPet(PhotoEntity photo) throws EntityNotFoundException, IllegalOperationException {
		if (photo.getPet() == null)
			return;

		if (photo.getPet().getId() == null)
			throw new IllegalOperationException("Photo must be associated with an existing pet");

		PetEntity pet = petRepository.findById(photo.getPet().getId())
				.orElseThrow(() -> new EntityNotFoundException(PET_NOT_FOUND));
		photo.setPet(pet);
		pet.getPhotos().add(photo);
	}

	private void attachToShelter(PhotoEntity photo) throws EntityNotFoundException, IllegalOperationException {
		if (photo.getShelter() == null)
			return;

		if (photo.getShelter().getId() == null)
			throw new IllegalOperationException("Photo must be associated with an existing shelter");

		ShelterEntity shelter = shelterRepository.findById(photo.getShelter().getId())
				.orElseThrow(() -> new EntityNotFoundException("Shelter not found"));
		photo.setShelter(shelter);
		shelter.getPhotos().add(photo);
	}

	@Transactional
	public List<PhotoEntity> getPhotos() {
		log.info("Inicia proceso de consultar todas las fotos");
		List<PhotoEntity> photos = photoRepository.findAll();
		if (photos.isEmpty())
			log.info("No hay fotos registradas");
		return photos;
	}

	@Transactional
	public List<PhotoEntity> getPhotos(Long petId, Long shelterId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar fotos filtradas");
		if (petId != null && (petId <= 0 || petRepository.findById(petId).isEmpty()))
			throw new EntityNotFoundException(PET_NOT_FOUND);

		if (shelterId != null && (shelterId <= 0 || shelterRepository.findById(shelterId).isEmpty()))
			throw new EntityNotFoundException("Shelter not found");

		List<PhotoEntity> photos = photoRepository.findAll().stream()
				.filter(p -> petId == null || (p.getPet() != null && petId.equals(p.getPet().getId())))
				.filter(p -> shelterId == null || (p.getShelter() != null && shelterId.equals(p.getShelter().getId())))
				.toList();
		if (photos.isEmpty())
			log.info("La entidad consultada no tiene fotos asignadas");
		return photos;
	}

	@Transactional
	public PhotoEntity getPhoto(Long photoId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar la foto con id = {}", photoId);
		PhotoEntity photo = findPhotoOrThrow(photoId);
		log.info("Termina proceso de consultar la foto con id = {}", photoId);
		return photo;
	}

	@Transactional
	public PhotoEntity updatePhoto(Long photoId, PhotoEntity photo)
			throws EntityNotFoundException, IllegalOperationException {
		return applyPhotoUpdate(photoId, photo);
	}

	@Transactional
	public void deletePhoto(Long photoId) throws EntityNotFoundException, IllegalOperationException {
		removePhoto(photoId);
	}

	private boolean isMainPhotoOfActivePet(PhotoEntity photo) {
		Long petId = photo.getPet().getId();
		long photoCount = photoRepository.findAll().stream()
				.filter(p -> p.getPet() != null && petId.equals(p.getPet().getId()))
				.count();
		if (photoCount != 1)
			return false;
		boolean hasFinalizedAdoption = adoptionRepository.findAll().stream()
				.anyMatch(a -> a.getPet() != null && petId.equals(a.getPet().getId())
						&& a.getStatus() != null && FINALIZED_STATUS.equalsIgnoreCase(a.getStatus()));
		return !hasFinalizedAdoption;
	}

	/**
	 * Simulated pets and photos for the "integration-tests" profile. Runs after the
	 * base data of AdopterService (shelter with id 1).
	 */
	@EventListener(value = ApplicationReadyEvent.class, condition = "@environment.matchesProfiles('integration-tests')")
	@Order(2)
	@Transactional
	public void seedIntegrationTestData() {
		if (petRepository.count() > 0)
			return;

		ShelterEntity shelter = shelterRepository.findById(1L).orElseThrow();

		PetEntity petOne = new PetEntity();
		petOne.setName("Rocky");
		petOne.setSpecies("Perro");
		petOne.setBreed("Labrador");
		petOne.setAge(3);
		petOne.setSex("M");
		petOne.setSize("Grande");
		petOne.setHealthStatus("Saludable");
		petOne.setDescription("Perro amigable");
		petOne.setAdmissionDate(AdopterService.relativeDate(-400));
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
		petTwo.setAdmissionDate(AdopterService.relativeDate(-300));
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
		petThree.setAdmissionDate(AdopterService.relativeDate(-10));
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
	}
}
