package co.edu.udistrital.mdp.pets.services;

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

import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ReviewEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(ReviewService.class)
class ReviewServiceTest {

	@Autowired
	private ReviewService reviewService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<ReviewEntity> reviewList = new ArrayList<>();
	private List<PetEntity> petList = new ArrayList<>();
	private List<AdopterEntity> adopterList = new ArrayList<>();
	private List<AdoptionEntity> adoptionList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from ReviewEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			PetEntity petEntity = factory.manufacturePojo(PetEntity.class);
			entityManager.persist(petEntity);
			petList.add(petEntity);

			AdopterEntity adopterEntity = factory.manufacturePojo(AdopterEntity.class);
			entityManager.persist(adopterEntity);
			adopterList.add(adopterEntity);

			AdoptionEntity adoptionEntity = factory.manufacturePojo(AdoptionEntity.class);
			adoptionEntity.setPet(petEntity);
			adoptionEntity.setAdopter(adopterEntity);
			adoptionEntity.setStatus("FINALIZED");
			entityManager.persist(adoptionEntity);
			adoptionList.add(adoptionEntity);
		}
		for (int i = 0; i < 3; i++) {
			ReviewEntity reviewEntity = factory.manufacturePojo(ReviewEntity.class);
			reviewEntity.setRating(4);
			reviewEntity.setPet(petList.get(i));
			reviewEntity.setAdoption(adoptionList.get(i));
			entityManager.persist(reviewEntity);
			reviewList.add(reviewEntity);
		}
	}

	@Test
	void testCreateReview() throws EntityNotFoundException, IllegalOperationException {
		PetEntity newPet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(newPet);
		AdopterEntity newAdopter = factory.manufacturePojo(AdopterEntity.class);
		entityManager.persist(newAdopter);
		AdoptionEntity newAdoption = factory.manufacturePojo(AdoptionEntity.class);
		newAdoption.setPet(newPet);
		newAdoption.setAdopter(newAdopter);
		newAdoption.setStatus("FINALIZED");
		entityManager.persist(newAdoption);

		ReviewEntity newEntity = factory.manufacturePojo(ReviewEntity.class);
		newEntity.setRating(5);
		newEntity.setPet(newPet);
		newEntity.setAdoption(newAdoption);

		ReviewEntity result = reviewService.createReview(newEntity);

		assertNotNull(result);
		ReviewEntity entity = entityManager.find(ReviewEntity.class, result.getId());
		assertEquals(newEntity.getComment(), entity.getComment());
		assertEquals(newEntity.getRating(), entity.getRating());
		assertEquals(newPet.getId(), entity.getPet().getId());
	}

	@Test
	void testCreateReviewWithNullRating() {
		assertThrows(IllegalOperationException.class, () -> {
			ReviewEntity newEntity = factory.manufacturePojo(ReviewEntity.class);
			newEntity.setRating(null);
			newEntity.setPet(petList.get(0));
			newEntity.setAdoption(adoptionList.get(0));
			reviewService.createReview(newEntity);
		});
	}

	@Test
	void testCreateReviewWithInvalidRatingRange() {
		assertThrows(IllegalOperationException.class, () -> {
			ReviewEntity newEntity = factory.manufacturePojo(ReviewEntity.class);
			newEntity.setRating(10);
			newEntity.setPet(petList.get(0));
			newEntity.setAdoption(adoptionList.get(0));
			reviewService.createReview(newEntity);
		});
	}

	@Test
	void testCreateReviewWithNonExistentPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			ReviewEntity newEntity = factory.manufacturePojo(ReviewEntity.class);
			newEntity.setRating(4);
			PetEntity fakePet = new PetEntity();
			fakePet.setId(0L);
			newEntity.setPet(fakePet);
			newEntity.setAdoption(adoptionList.get(0));
			reviewService.createReview(newEntity);
		});
	}

	@Test
	void testCreateReviewWithoutFinalizedAdoption() {
		assertThrows(IllegalOperationException.class, () -> {
			AdoptionEntity notFinalized = factory.manufacturePojo(AdoptionEntity.class);
			notFinalized.setPet(petList.get(0));
			notFinalized.setAdopter(adopterList.get(0));
			notFinalized.setStatus("IN_PROGRESS");
			entityManager.persist(notFinalized);

			ReviewEntity newEntity = factory.manufacturePojo(ReviewEntity.class);
			newEntity.setRating(4);
			newEntity.setPet(petList.get(0));
			newEntity.setAdoption(notFinalized);
			reviewService.createReview(newEntity);
		});
	}

	@Test
	void testCreateReviewDuplicatedByAdopter() {
		assertThrows(IllegalOperationException.class, () -> {
			ReviewEntity newEntity = factory.manufacturePojo(ReviewEntity.class);
			newEntity.setRating(4);
			newEntity.setPet(petList.get(0));
			newEntity.setAdoption(adoptionList.get(0));
			reviewService.createReview(newEntity);
		});
	}

	@Test
	void testGetReviews() {
		List<ReviewEntity> list = reviewService.getReviews();
		assertEquals(reviewList.size(), list.size());
	}

	@Test
	void testGetReviewsEmpty() {
		entityManager.getEntityManager().createQuery("delete from ReviewEntity").executeUpdate();
		List<ReviewEntity> list = reviewService.getReviews();
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetReviewsFilteredByPetAndAdopter() {
		List<ReviewEntity> list = reviewService.getReviews(petList.get(0).getId(), adopterList.get(0).getId());
		assertEquals(1, list.size());
		List<ReviewEntity> emptyList = reviewService.getReviews(petList.get(0).getId(), adopterList.get(1).getId());
		assertTrue(emptyList.isEmpty());
	}

	@Test
	void testGetReview() throws EntityNotFoundException, IllegalOperationException {
		ReviewEntity entity = reviewList.get(0);
		ReviewEntity resultEntity = reviewService.getReview(entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
	}

	@Test
	void testGetInvalidReviewId() {
		assertThrows(IllegalOperationException.class, () -> {
			reviewService.getReview(0L);
		});
	}

	@Test
	void testGetNonExistentReview() {
		assertThrows(EntityNotFoundException.class, () -> {
			reviewService.getReview(1000L);
		});
	}

	@Test
	void testUpdateReview() throws EntityNotFoundException, IllegalOperationException {
		ReviewEntity entity = reviewList.get(0);
		ReviewEntity pojoEntity = factory.manufacturePojo(ReviewEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setRating(3);

		reviewService.updateReview(entity.getId(), pojoEntity);

		ReviewEntity resp = entityManager.find(ReviewEntity.class, entity.getId());
		assertEquals(pojoEntity.getComment(), resp.getComment());
		assertEquals(pojoEntity.getRating(), resp.getRating());
		assertEquals(entity.getPet().getId(), resp.getPet().getId());
	}

	@Test
	void testUpdateReviewInvalidId() {
		assertThrows(EntityNotFoundException.class, () -> {
			ReviewEntity pojoEntity = factory.manufacturePojo(ReviewEntity.class);
			pojoEntity.setId(1000L);
			pojoEntity.setRating(3);
			reviewService.updateReview(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateReviewWithInvalidRating() {
		assertThrows(IllegalOperationException.class, () -> {
			ReviewEntity entity = reviewList.get(0);
			ReviewEntity pojoEntity = factory.manufacturePojo(ReviewEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setRating(0);
			reviewService.updateReview(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testDeleteReview() throws EntityNotFoundException, IllegalOperationException {
		ReviewEntity entity = reviewList.get(0);
		Long adopterId = entity.getAdoption().getAdopter().getId();
		reviewService.deleteReview(entity.getId(), adopterId);
		ReviewEntity deleted = entityManager.find(ReviewEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteInvalidReview() {
		assertThrows(EntityNotFoundException.class, () -> {
			reviewService.deleteReview(1000L, adopterList.get(0).getId());
		});
	}

	@Test
	void testDeleteReviewByNonAuthorAdopter() {
		assertThrows(IllegalOperationException.class, () -> {
			ReviewEntity entity = reviewList.get(0);
			reviewService.deleteReview(entity.getId(), adopterList.get(1).getId());
		});
	}
}
