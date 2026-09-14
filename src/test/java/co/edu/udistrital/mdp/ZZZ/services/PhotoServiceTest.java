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
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ShelterEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			PetEntity petEntity = factory.manufacturePojo(PetEntity.class);
			entityManager.persist(petEntity);
			petList.add(petEntity);

			ShelterEntity shelterEntity = factory.manufacturePojo(ShelterEntity.class);
			entityManager.persist(shelterEntity);
			shelterList.add(shelterEntity);

			PhotoEntity photoEntity = factory.manufacturePojo(PhotoEntity.class);
			photoEntity.setUrl("pet-photo-" + i + ".jpg");
			photoEntity.setType("GALLERY");
			photoEntity.setPet(petEntity);
			photoEntity.setShelter(null);
			entityManager.persist(photoEntity);
			photoList.add(photoEntity);
		}
	}

	@Test
	void testCreatePhotoWithPet() throws EntityNotFoundException, IllegalOperationException {
		PhotoEntity newEntity = factory.manufacturePojo(PhotoEntity.class);
		newEntity.setUrl("new-photo.jpg");
		newEntity.setPet(petList.get(0));
		newEntity.setShelter(null);

		PhotoEntity result = photoService.createPhoto(newEntity);

		assertNotNull(result);
		PhotoEntity entity = entityManager.find(PhotoEntity.class, result.getId());
		assertEquals(newEntity.getUrl(), entity.getUrl());
		assertEquals(petList.get(0).getId(), entity.getPet().getId());
	}

	@Test
	void testCreatePhotoWithShelter() throws EntityNotFoundException, IllegalOperationException {
		PhotoEntity newEntity = factory.manufacturePojo(PhotoEntity.class);
		newEntity.setUrl("new-shelter-photo.png");
		newEntity.setPet(null);
		newEntity.setShelter(shelterList.get(0));

		PhotoEntity result = photoService.createPhoto(newEntity);

		assertNotNull(result);
		assertEquals(shelterList.get(0).getId(), result.getShelter().getId());
	}

	@Test
	void testCreatePhotoWithNullUrl() {
		assertThrows(IllegalOperationException.class, () -> {
			PhotoEntity newEntity = factory.manufacturePojo(PhotoEntity.class);
			newEntity.setUrl(null);
			newEntity.setPet(petList.get(0));
			photoService.createPhoto(newEntity);
		});
	}

	@Test
	void testCreatePhotoWithEmptyUrl() {
		assertThrows(IllegalOperationException.class, () -> {
			PhotoEntity newEntity = factory.manufacturePojo(PhotoEntity.class);
			newEntity.setUrl("");
			newEntity.setPet(petList.get(0));
			photoService.createPhoto(newEntity);
		});
	}

	@Test
	void testCreatePhotoWithInvalidFormat() {
		assertThrows(IllegalOperationException.class, () -> {
			PhotoEntity newEntity = factory.manufacturePojo(PhotoEntity.class);
			newEntity.setUrl("photo.gif");
			newEntity.setPet(petList.get(0));
			photoService.createPhoto(newEntity);
		});
	}

	@Test
	void testCreatePhotoWithoutAssociation() {
		assertThrows(IllegalOperationException.class, () -> {
			PhotoEntity newEntity = factory.manufacturePojo(PhotoEntity.class);
			newEntity.setUrl("photo.jpg");
			newEntity.setPet(null);
			newEntity.setShelter(null);
			photoService.createPhoto(newEntity);
		});
	}

	@Test
	void testCreatePhotoWithNonExistentPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			PhotoEntity newEntity = factory.manufacturePojo(PhotoEntity.class);
			newEntity.setUrl("photo.jpg");
			PetEntity fakePet = new PetEntity();
			fakePet.setId(0L);
			newEntity.setPet(fakePet);
			newEntity.setShelter(null);
			photoService.createPhoto(newEntity);
		});
	}

	@Test
	void testCreatePhotoWithNonExistentShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			PhotoEntity newEntity = factory.manufacturePojo(PhotoEntity.class);
			newEntity.setUrl("photo.jpg");
			newEntity.setPet(null);
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
	void testGetPhotosFilteredByPet() throws EntityNotFoundException, IllegalOperationException {
		List<PhotoEntity> list = photoService.getPhotos(petList.get(0).getId(), null);
		assertEquals(1, list.size());
		assertEquals(petList.get(0).getId(), list.get(0).getPet().getId());
	}

	@Test
	void testGetPhotosFilteredByShelter() throws EntityNotFoundException, IllegalOperationException {
		PhotoEntity shelterPhoto = factory.manufacturePojo(PhotoEntity.class);
		shelterPhoto.setUrl("shelter-photo.jpg");
		shelterPhoto.setPet(null);
		shelterPhoto.setShelter(shelterList.get(0));
		entityManager.persist(shelterPhoto);

		List<PhotoEntity> list = photoService.getPhotos(null, shelterList.get(0).getId());
		assertEquals(1, list.size());
		assertEquals(shelterList.get(0).getId(), list.get(0).getShelter().getId());
	}

	@Test
	void testGetPhotosByPetWithoutPhotos() throws EntityNotFoundException, IllegalOperationException {
		PetEntity newPet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(newPet);

		List<PhotoEntity> list = photoService.getPhotos(newPet.getId(), null);
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetPhotosWithNonExistentPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			photoService.getPhotos(1000L, null);
		});
	}

	@Test
	void testGetPhotosWithInvalidPetId() {
		assertThrows(IllegalOperationException.class, () -> {
			photoService.getPhotos(0L, null);
		});
	}

	@Test
	void testGetPhoto() throws EntityNotFoundException, IllegalOperationException {
		PhotoEntity entity = photoList.get(0);
		PhotoEntity resultEntity = photoService.getPhoto(entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
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
		PhotoEntity entity = photoList.get(0);
		PhotoEntity pojoEntity = factory.manufacturePojo(PhotoEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setUrl("updated-photo.jpg");
		pojoEntity.setPet(entity.getPet());

		photoService.updatePhoto(entity.getId(), pojoEntity);

		PhotoEntity resp = entityManager.find(PhotoEntity.class, entity.getId());
		assertEquals(pojoEntity.getUrl(), resp.getUrl());
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
			photoService.updatePhoto(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdatePhotoWithInvalidFormat() {
		assertThrows(IllegalOperationException.class, () -> {
			PhotoEntity entity = photoList.get(0);
			PhotoEntity pojoEntity = factory.manufacturePojo(PhotoEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setUrl("photo.bmp");
			photoService.updatePhoto(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdatePhotoChangePet() {
		assertThrows(IllegalOperationException.class, () -> {
			PhotoEntity entity = photoList.get(0);
			PhotoEntity pojoEntity = factory.manufacturePojo(PhotoEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setUrl("updated-photo.jpg");
			pojoEntity.setPet(petList.get(1));
			photoService.updatePhoto(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdatePhotoRemovePetAssociation() {
		assertThrows(IllegalOperationException.class, () -> {
			PhotoEntity entity = photoList.get(0);
			PhotoEntity pojoEntity = factory.manufacturePojo(PhotoEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setUrl("updated-photo.jpg");
			pojoEntity.setPet(null);
			photoService.updatePhoto(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testDeletePhoto() throws EntityNotFoundException, IllegalOperationException {
		PhotoEntity entity = photoList.get(0);
		photoService.deletePhoto(entity.getId());
		PhotoEntity deleted = entityManager.find(PhotoEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteInvalidPhoto() {
		assertThrows(EntityNotFoundException.class, () -> {
			photoService.deletePhoto(1000L);
		});
	}

	@Test
	void testDeleteProfilePhotoOfActivePet() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newPet = factory.manufacturePojo(PetEntity.class);
			entityManager.persist(newPet);

			PhotoEntity profilePhoto = factory.manufacturePojo(PhotoEntity.class);
			profilePhoto.setUrl("profile.jpg");
			profilePhoto.setType("PROFILE");
			profilePhoto.setPet(newPet);
			profilePhoto.setShelter(null);
			entityManager.persist(profilePhoto);

			photoService.deletePhoto(profilePhoto.getId());
		});
	}
}