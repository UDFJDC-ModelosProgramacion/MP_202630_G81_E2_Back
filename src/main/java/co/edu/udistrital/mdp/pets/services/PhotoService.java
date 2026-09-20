package co.edu.udistrital.mdp.pets.services;

import java.util.List;
import java.util.Optional;
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

@Slf4j
@Service
@RequiredArgsConstructor 
public class PhotoService {

	private static final Set<String> VALID_FORMATS = Set.of("JPG", "JPEG", "PNG", "GIF", "WEBP", "BMP");
	private static final String FINALIZED_STATUS = "FINALIZED";

	private static final String PHOTO_NOT_FOUND = "Photo not found";
	private static final String PHOTO_ID_NOT_VALID = "Photo id is not valid";

	private final PhotoRepository photoRepository;
	private final PetRepository petRepository;
	private final ShelterRepository shelterRepository;
	private final AdoptionRepository adoptionRepository;

	/**
	 * Crea una nueva foto.
	 */
	@Transactional
	public PhotoEntity createPhoto(PhotoEntity photo) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación de la foto");

		if (photo.getUrl() == null || photo.getUrl().isBlank())
			throw new IllegalOperationException("Photo url cannot be null or empty");

		if (photo.getType() == null || photo.getType().isBlank())
			throw new IllegalOperationException("Photo format cannot be null or empty");
		if (!VALID_FORMATS.contains(photo.getType().toUpperCase()))
			throw new IllegalOperationException("Photo format is not valid; use JPG, PNG or another supported format");

		if (photo.getPet() == null && photo.getShelter() == null)
			throw new IllegalOperationException("Photo must be associated with an existing pet, shelter or user");

		if (photo.getPet() != null) {
			if (photo.getPet().getId() == null)
				throw new IllegalOperationException("Photo must be associated with an existing pet");
			Optional<PetEntity> pet = petRepository.findById(photo.getPet().getId());
			if (pet.isEmpty())
				throw new EntityNotFoundException("Pet not found");
			photo.setPet(pet.get());
			pet.get().getPhotos().add(photo);
		}

		if (photo.getShelter() != null) {
			if (photo.getShelter().getId() == null)
				throw new IllegalOperationException("Photo must be associated with an existing shelter");
			Optional<ShelterEntity> shelter = shelterRepository.findById(photo.getShelter().getId());
			if (shelter.isEmpty())
				throw new EntityNotFoundException("Shelter not found");
			photo.setShelter(shelter.get());
			shelter.get().getPhotos().add(photo);
		}

		log.info("Termina proceso de creación de la foto");
		return photoRepository.save(photo);
	}

	/**
	 * Obtiene todas las fotos registradas.
	 */
	@Transactional
	public List<PhotoEntity> getPhotos() {
		log.info("Inicia proceso de consultar todas las fotos");
		List<PhotoEntity> photos = photoRepository.findAll();
		if (photos.isEmpty())
			log.info("No hay fotos registradas");
		return photos;
	}

	/**
	 * Obtiene las fotos filtrando, de forma opcional, por mascota y/o refugio. El
	 * identificador de la entidad asociada debe existir en el sistema.
	 */
	@Transactional
	public List<PhotoEntity> getPhotos(Long petId, Long shelterId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar fotos filtradas");
		if (petId != null) {
			if (petId <= 0 || petRepository.findById(petId).isEmpty())
				throw new EntityNotFoundException("Pet not found");
		}
		if (shelterId != null) {
			if (shelterId <= 0 || shelterRepository.findById(shelterId).isEmpty())
				throw new EntityNotFoundException("Shelter not found");
		}

		List<PhotoEntity> photos = photoRepository.findAll().stream()
				.filter(p -> petId == null || (p.getPet() != null && petId.equals(p.getPet().getId())))
				.filter(p -> shelterId == null || (p.getShelter() != null && shelterId.equals(p.getShelter().getId())))
				.toList();
		if (photos.isEmpty())
			log.info("La entidad consultada no tiene fotos asignadas");
		return photos;
	}

	/**
	 * Obtiene una foto a partir de su id.
	 */
	@Transactional
	public PhotoEntity getPhoto(Long photoId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar la foto con id = {}", photoId);
		if (photoId == null || photoId <= 0)
			throw new IllegalOperationException(PHOTO_ID_NOT_VALID);

		Optional<PhotoEntity> photo = photoRepository.findById(photoId);
		if (photo.isEmpty())
			throw new EntityNotFoundException(PHOTO_NOT_FOUND);

		log.info("Termina proceso de consultar la foto con id = {}", photoId);
		return photo.get();
	}

	/**
	 * Actualiza una foto existente. La entidad a la que está asociada la foto
	 * original no puede ser modificada.
	 */
	@Transactional
	public PhotoEntity updatePhoto(Long photoId, PhotoEntity photo)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la foto con id = {}", photoId);
		if (photoId == null || photoId <= 0)
			throw new IllegalOperationException(PHOTO_ID_NOT_VALID);

		Optional<PhotoEntity> existing = photoRepository.findById(photoId);
		if (existing.isEmpty())
			throw new EntityNotFoundException(PHOTO_NOT_FOUND);

		if (photo.getUrl() == null || photo.getUrl().isBlank())
			throw new IllegalOperationException("The new photo url cannot be null or empty");
		if (photo.getType() == null || photo.getType().isBlank())
			throw new IllegalOperationException("Photo format cannot be null or empty");
		if (!VALID_FORMATS.contains(photo.getType().toUpperCase()))
			throw new IllegalOperationException("Photo format is not valid; use JPG, PNG or another supported format");

		PhotoEntity current = existing.get();
		current.setUrl(photo.getUrl());
		current.setType(photo.getType());
		current.setDescription(photo.getDescription());

		log.info("Termina proceso de actualizar la foto con id = {}", photoId);
		return photoRepository.save(current);
	}

	/**
	 * Borra una foto a partir de su id.
	 */
	@Transactional
	public void deletePhoto(Long photoId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar la foto con id = {}", photoId);
		if (photoId == null || photoId <= 0)
			throw new IllegalOperationException(PHOTO_ID_NOT_VALID);

		Optional<PhotoEntity> photo = photoRepository.findById(photoId);
		if (photo.isEmpty())
			throw new EntityNotFoundException(PHOTO_NOT_FOUND);

		PhotoEntity current = photo.get();
		if (current.getPet() != null && isMainPhotoOfActivePet(current))
			throw new IllegalOperationException(
					"The main profile photo of a pet active for adoption cannot be deleted without establishing another default image first");

		photoRepository.deleteById(photoId);
		log.info("Termina proceso de borrar la foto con id = {}", photoId);
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
}