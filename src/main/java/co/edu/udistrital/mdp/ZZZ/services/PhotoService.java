package co.edu.udistrital.mdp.ZZZ.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.ZZZ.entities.PetEntity;
import co.edu.udistrital.mdp.ZZZ.entities.PhotoEntity;
import co.edu.udistrital.mdp.ZZZ.entities.ShelterEntity;
import co.edu.udistrital.mdp.ZZZ.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.ZZZ.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.ZZZ.repositories.PetRepository;
import co.edu.udistrital.mdp.ZZZ.repositories.PhotoRepository;
import co.edu.udistrital.mdp.ZZZ.repositories.ShelterRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PhotoService {

	private static final String PROFILE_TYPE = "PROFILE";
	private static final String PRIMARY_TYPE = "PRIMARY";
	private static final String FINALIZED_STATUS = "FINALIZED";

	@Autowired
	PhotoRepository photoRepository;

	@Autowired
	PetRepository petRepository;

	@Autowired
	ShelterRepository shelterRepository;

	/**
	 * Crea una nueva foto asociada a una mascota o a un refugio.
	 */
	@Transactional
	public PhotoEntity createPhoto(PhotoEntity photo) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación de la foto");

		if (photo.getUrl() == null || photo.getUrl().isBlank())
			throw new IllegalOperationException("Photo url cannot be null or empty");
		if (!isValidImageFormat(photo.getUrl()))
			throw new IllegalOperationException("Photo format must be valid (JPG, PNG)");

		boolean hasPet = photo.getPet() != null && photo.getPet().getId() != null;
		boolean hasShelter = photo.getShelter() != null && photo.getShelter().getId() != null;
		if (!hasPet && !hasShelter)
			throw new IllegalOperationException("Photo must be associated with an existing pet or shelter");

		if (hasPet) {
			Optional<PetEntity> pet = petRepository.findById(photo.getPet().getId());
			if (pet.isEmpty())
				throw new EntityNotFoundException("Pet not found");
			photo.setPet(pet.get());
		}
		if (hasShelter) {
			Optional<ShelterEntity> shelter = shelterRepository.findById(photo.getShelter().getId());
			if (shelter.isEmpty())
				throw new EntityNotFoundException("Shelter not found");
			photo.setShelter(shelter.get());
		}

		log.info("Termina proceso de creación de la foto");
		return photoRepository.save(photo);
	}

	/**
	 * Obtiene una foto a partir de su id.
	 */
	@Transactional
	public PhotoEntity getPhoto(Long photoId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar la foto con id = {}", photoId);
		if (photoId == null || photoId <= 0)
			throw new IllegalOperationException("Photo id is not valid");

		Optional<PhotoEntity> photo = photoRepository.findById(photoId);
		if (photo.isEmpty())
			throw new EntityNotFoundException("Photo not found");

		log.info("Termina proceso de consultar la foto con id = {}", photoId);
		return photo.get();
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
	 * Obtiene las fotos asociadas a una mascota y/o a un refugio, verificando
	 * previamente que las entidades asociadas existan en el sistema.
	 */
	@Transactional
	public List<PhotoEntity> getPhotos(Long petId, Long shelterId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar fotos por entidad asociada");

		if (petId != null) {
			if (petId <= 0)
				throw new IllegalOperationException("Pet id is not valid");
			if (petRepository.findById(petId).isEmpty())
				throw new EntityNotFoundException("Pet not found");
		}
		if (shelterId != null) {
			if (shelterId <= 0)
				throw new IllegalOperationException("Shelter id is not valid");
			if (shelterRepository.findById(shelterId).isEmpty())
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
	 * Actualiza una foto existente. No se puede modificar la entidad a la que está
	 * asociada la foto original.
	 */
	@Transactional
	public PhotoEntity updatePhoto(Long photoId, PhotoEntity photo)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la foto con id = {}", photoId);
		if (photoId == null || photoId <= 0)
			throw new IllegalOperationException("Photo id is not valid");

		Optional<PhotoEntity> existing = photoRepository.findById(photoId);
		if (existing.isEmpty())
			throw new EntityNotFoundException("Photo not found");

		if (photo.getUrl() == null || photo.getUrl().isBlank())
			throw new IllegalOperationException("Photo url cannot be null or empty");
		if (!isValidImageFormat(photo.getUrl()))
			throw new IllegalOperationException("Photo format must be valid (JPG, PNG)");

		PhotoEntity current = existing.get();
		boolean petChanged = photo.getPet() != null && current.getPet() != null
				&& !photo.getPet().getId().equals(current.getPet().getId());
		boolean shelterChanged = photo.getShelter() != null && current.getShelter() != null
				&& !photo.getShelter().getId().equals(current.getShelter().getId());
		boolean associationRemoved = (photo.getPet() == null && current.getPet() != null)
				|| (photo.getShelter() == null && current.getShelter() != null);
		if (petChanged || shelterChanged || associationRemoved)
			throw new IllegalOperationException("The entity associated with the photo cannot be modified");

		current.setUrl(photo.getUrl());
		current.setType(photo.getType());
		current.setDescription(photo.getDescription());

		log.info("Termina proceso de actualizar la foto con id = {}", photoId);
		return photoRepository.save(current);
	}

	/**
	 * Borra una foto a partir de su id. No se puede eliminar la foto de perfil
	 * principal de una mascota activa para adopción sin establecer otra por
	 * defecto antes.
	 */
	@Transactional
	public void deletePhoto(Long photoId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar la foto con id = {}", photoId);
		if (photoId == null || photoId <= 0)
			throw new IllegalOperationException("Photo id is not valid");

		Optional<PhotoEntity> photo = photoRepository.findById(photoId);
		if (photo.isEmpty())
			throw new EntityNotFoundException("Photo not found");

		PhotoEntity photoEntity = photo.get();
		if (isProfilePhoto(photoEntity) && photoEntity.getPet() != null && isPetActiveForAdoption(photoEntity.getPet()))
			throw new IllegalOperationException(
					"The main profile photo of a pet active for adoption cannot be deleted without setting another default image first");

		photoRepository.deleteById(photoId);
		log.info("Termina proceso de borrar la foto con id = {}", photoId);
	}

	private boolean isProfilePhoto(PhotoEntity photo) {
		String type = photo.getType();
		return type != null && (type.equalsIgnoreCase(PROFILE_TYPE) || type.equalsIgnoreCase(PRIMARY_TYPE));
	}

	private boolean isPetActiveForAdoption(PetEntity pet) {
		if (pet.getAdoptions() == null)
			return true;
		return pet.getAdoptions().stream()
				.noneMatch(adoption -> FINALIZED_STATUS.equalsIgnoreCase(adoption.getStatus()));
	}

	private boolean isValidImageFormat(String url) {
		String lower = url.toLowerCase();
		return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
	}
}