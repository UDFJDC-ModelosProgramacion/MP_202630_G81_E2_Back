package co.edu.udistrital.mdp.ZZZ.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.ZZZ.entities.AdopterEntity;
import co.edu.udistrital.mdp.ZZZ.entities.AdoptionEntity;
import co.edu.udistrital.mdp.ZZZ.entities.PetEntity;
import co.edu.udistrital.mdp.ZZZ.entities.PhotoEntity;
import co.edu.udistrital.mdp.ZZZ.entities.ShelterEntity;
import co.edu.udistrital.mdp.ZZZ.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.ZZZ.exceptions.IllegalOperationException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(PhotoService.class)
class PhotoServiceTest {

	@Autowired
	private PhotoService photoService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<PhotoEntity> photoList = new ArrayList<>();
	private List<PetEntity> petList = new ArrayList<>();
	private List<ShelterEntity> shelterList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from PhotoEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ShelterEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			PetEntity petEntity = factory.manufacturePojo(PetEntity.class);
			entityManager.persist(petEntity);
			petList.add(petEntity);

			PhotoEntity photoEntity = buildValidPhoto();
			photoEntity.setPet(petEntity);
			petEntity.getPhotos().add(photoEntity);
			entityManager.persist(photoEntity);
			photoList.add(photoEntity);
		}
		for (int i = 0; i < 2; i++) {
			ShelterEntity shelterEntity = factory.manufacturePojo(ShelterEntity.class);
			entityManager.persist(shelterEntity);
			shelterList.add(shelterEntity);
		}
		PhotoEntity extraPhoto = buildValidPhoto();
		extraPhoto.setPet(petList.get(0));
		petList.get(0).getPhotos().add(extraPhoto);
		entityManager.persist(extraPhoto);
		photoList.add(extraPhoto);
	}

	private PhotoEntity buildValidPhoto() {
		PhotoEntity photo = factory.manufacturePojo(PhotoEntity.class);
		photo.setUrl("https://shelter.example.com/photos/" + System.nanoTime());
		photo.setType("JPG");
		return photo;
	}

	@Test
	void testCreatePhoto() throws EntityNotFoundException, IllegalOperationException {
		PetEntity pet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(pet);

		PhotoEntity newEntity = buildValidPhoto();
		newEntity.setPet(pet);

		PhotoEntity result = photoService.createPhoto(newEntity);

		assertNotNull(result);
		PhotoEntity persisted = entityManager.find(PhotoEntity.class, result.getId());
		assertNotNull(persisted);
		assertEquals(newEntity.getUrl(), persisted.getUrl());
		assertEquals(newEntity.getType(), persisted.getType());
		assertEquals(pet.getId(), persisted.getPet().getId());
	}

	@Test
	void testCreatePhotoWithShelter() throws EntityNotFoundException, IllegalOperationException {
		ShelterEntity shelter = factory.manufacturePojo(ShelterEntity.class);
		entityManager.persist(shelter);

		PhotoEntity newEntity = buildValidPhoto();
		newEntity.setShelter(shelter);

		PhotoEntity result = photoService.createPhoto(newEntity);

		assertNotNull(result);
		PhotoEntity persisted = entityManager.find(PhotoEntity.class, result.getId());
		assertEquals(shelter.getId(), persisted.getShelter().getId());
	}

	@Test
	void testCreatePhotoWithNullUrl() {
		assertThrows(IllegalOperationException.class, () -> {
			PhotoEntity newEntity = buildValidPhoto();
			newEntity.setPet(petList.get(0));
			newEntity.setUrl(null);
			photoService.createPhoto(newEntity);
		});
	}

	@Test
	void testCreatePhotoWithEmptyUrl() {
		assertThrows(IllegalOperationException.class, () -> {
			PhotoEntity newEntity = buildValidPhoto();
			newEntity.setPet(petList.get(0));
			newEntity.setUrl("");
			photoService.createPhoto(newEntity);
		});
	}

	@Test
	void testCreatePhotoWithInvalidFormat() {
		assertThrows(IllegalOperationException.class, () -> {
			PhotoEntity newEntity = buildValidPhoto();
			newEntity.setPet(petList.get(0));
			newEntity.setType("TIFF");
			photoService.createPhoto(newEntity);
		});
	}

	@Test
	void testCreatePhotoWithNoAssociatedEntity() {
		assertThrows(IllegalOperationException.class, () -> {
			PhotoEntity newEntity = buildValidPhoto();
			newEntity.setPet(null);
			newEntity.setShelter(null);
			photoService.createPhoto(newEntity);
		});
	}

	@Test
	void testCreatePhotoWithNonExistentPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			PhotoEntity newEntity = buildValidPhoto();
			PetEntity fakePet = new PetEntity();
			fakePet.setId(0L);
			newEntity.setPet(fakePet);
			photoService.createPhoto(newEntity);
		});
	}

	@Test
	void testCreatePhotoWithNonExistentShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			PhotoEntity newEntity = buildValidPhoto();
			ShelterEntity fakeShelter = new ShelterEntity();
			fakeShelter.setId(0L);
			newEntity.setShelter(fakeShelter);
			photoService.createPhoto(newEntity);
		});
	}

	@Test
	void testGetPhotos() {
		List<PhotoEntity> list = photoService.getPhotos();
		assertEquals(photoList.size(), list.size());
	}

	@Test
	void testGetPhotosEmpty() {
		entityManager.getEntityManager().createQuery("delete from PhotoEntity").executeUpdate();
		List<PhotoEntity> list = photoService.getPhotos();
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetPhotosByPet() throws EntityNotFoundException, IllegalOperationException {
		List<PhotoEntity> list = photoService.getPhotos(petList.get(0).getId(), null);
		// la primera mascota tiene dos fotos asignadas
		assertEquals(2, list.size());
		List<PhotoEntity> emptyList = photoService.getPhotos(petList.get(2).getId(), null);
		assertEquals(1, emptyList.size());
	}

	@Test
	void testGetPhotosByNonExistentPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			photoService.getPhotos(1000L, null);
		});
	}

	@Test
	void testGetPhotosByInvalidShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			photoService.getPhotos(null, 0L);
		});
	}

	@Test
	void testGetPhoto() throws EntityNotFoundException, IllegalOperationException {
		PhotoEntity entity = photoList.get(0);
		PhotoEntity resultEntity = photoService.getPhoto(entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
		assertEquals(entity.getUrl(), resultEntity.getUrl());
	}

	@Test
	void testGetInvalidPhotoId() {
		assertThrows(IllegalOperationException.class, () -> {
			photoService.getPhoto(0L);
		});
	}

	@Test
	void testGetNonExistentPhoto() {
		assertThrows(EntityNotFoundException.class, () -> {
			photoService.getPhoto(1000L);
		});
	}

	@Test
	void testUpdatePhoto() throws EntityNotFoundException, IllegalOperationException {
		PhotoEntity entity = photoList.get(1);
		PhotoEntity pojoEntity = factory.manufacturePojo(PhotoEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setUrl("https://shelter.example.com/photos/updated");
		pojoEntity.setType("PNG");
		pojoEntity.setDescription("Nueva descripcion");
		pojoEntity.setPet(petList.get(2));

		photoService.updatePhoto(entity.getId(), pojoEntity);

		PhotoEntity resp = entityManager.find(PhotoEntity.class, entity.getId());
		assertEquals("https://shelter.example.com/photos/updated", resp.getUrl());
		assertEquals("PNG", resp.getType());
		// la entidad asociada original no se modifica
		assertEquals(entity.getPet().getId(), resp.getPet().getId());
	}

	@Test
	void testUpdatePhotoInvalidId() {
		assertThrows(EntityNotFoundException.class, () -> {
			PhotoEntity pojoEntity = factory.manufacturePojo(PhotoEntity.class);
			pojoEntity.setId(1000L);
			photoService.updatePhoto(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdatePhotoWithNullUrl() {
		assertThrows(IllegalOperationException.class, () -> {
			PhotoEntity entity = photoList.get(0);
			PhotoEntity pojoEntity = factory.manufacturePojo(PhotoEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setUrl(null);
			pojoEntity.setType("JPG");
			photoService.updatePhoto(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testDeletePhotoOfActivePetWithOnlyPhoto() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity pet = factory.manufacturePojo(PetEntity.class);
			entityManager.persist(pet);

			PhotoEntity photo = buildValidPhoto();
			photo.setPet(pet);
			pet.getPhotos().add(photo);
			entityManager.persist(photo);

			photoService.deletePhoto(photo.getId());
		});
	}

	@Test
	void testDeletePhotoWhenPetHasMorePhotos() throws EntityNotFoundException, IllegalOperationException {
		// petList.get(0) tiene dos fotos, por lo que ninguna es la foto de perfil
		// principal y la eliminación está permitida.
		PhotoEntity entity = photoList.get(0);
		photoService.deletePhoto(entity.getId());
		PhotoEntity deleted = entityManager.find(PhotoEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeletePhotoOfAdoptedPet() throws EntityNotFoundException, IllegalOperationException {
		PetEntity pet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(pet);

		PhotoEntity photo = buildValidPhoto();
		photo.setPet(pet);
		pet.getPhotos().add(photo);
		entityManager.persist(photo);

		AdopterEntity adopter = factory.manufacturePojo(AdopterEntity.class);
		entityManager.persist(adopter);
		AdoptionEntity adoption = factory.manufacturePojo(AdoptionEntity.class);
		adoption.setPet(pet);
		adoption.setAdopter(adopter);
		adoption.setStatus("FINALIZED");
		entityManager.persist(adoption);
		pet.getAdoptions().add(adoption);

		photoService.deletePhoto(photo.getId());
		PhotoEntity deleted = entityManager.find(PhotoEntity.class, photo.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteInvalidPhoto() {
		assertThrows(EntityNotFoundException.class, () -> {
			photoService.deletePhoto(1000L);
		});
	}
}