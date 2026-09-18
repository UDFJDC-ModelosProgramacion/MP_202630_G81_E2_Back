package co.edu.udistrital.mdp.pets.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ReviewEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor 
public class ReviewService {

	private static final String FINALIZED_STATUS = "FINALIZED";
	private static final int MIN_RATING = 1;
	private static final int MAX_RATING = 5;

	private final ReviewRepository reviewRepository;
	private final PetRepository petRepository;
	private final AdoptionRepository adoptionRepository;

	/**
	 * Crea una nueva reseña.
	 */
	@Transactional
	public ReviewEntity createReview(ReviewEntity review) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación de la reseña");

		if (review.getRating() == null)
			throw new IllegalOperationException("Rating cannot be null");
		if (review.getComment() == null || review.getComment().isBlank())
			throw new IllegalOperationException("Comment cannot be null or empty");
		if (review.getDate() == null)
			throw new IllegalOperationException("Date cannot be null");
		if (review.getTime() == null)
			throw new IllegalOperationException("Time cannot be null");
		if (review.getRating() < MIN_RATING || review.getRating() > MAX_RATING)
			throw new IllegalOperationException("Rating must be between " + MIN_RATING + " and " + MAX_RATING);

		if (review.getPet() == null || review.getPet().getId() == null)
			throw new IllegalOperationException("Review must be associated with an existing pet");
		Optional<PetEntity> pet = petRepository.findById(review.getPet().getId());
		if (pet.isEmpty())
			throw new EntityNotFoundException("Pet not found");

		if (review.getAdoption() == null || review.getAdoption().getId() == null)
			throw new IllegalOperationException("Review must be associated with an existing adopter");
		Optional<AdoptionEntity> adoption = adoptionRepository.findById(review.getAdoption().getId());
		if (adoption.isEmpty())
			throw new EntityNotFoundException("Adoption not found");

		AdoptionEntity adoptionEntity = adoption.get();
		if (adoptionEntity.getAdopter() == null)
			throw new IllegalOperationException("Review must be associated with an existing adopter");

		if (adoptionEntity.getPet() == null || !adoptionEntity.getPet().getId().equals(pet.get().getId())
				|| !FINALIZED_STATUS.equalsIgnoreCase(adoptionEntity.getStatus()))
			throw new IllegalOperationException(
					"The adopter can only review a pet with which it has had a finalized adoption process");

		boolean alreadyReviewed = reviewRepository.findAll().stream()
				.anyMatch(r -> r.getPet() != null && r.getPet().getId().equals(pet.get().getId())
						&& r.getAdoption() != null && r.getAdoption().getAdopter() != null
						&& r.getAdoption().getAdopter().getId().equals(adoptionEntity.getAdopter().getId()));
		if (alreadyReviewed)
			throw new IllegalOperationException("This adopter has already reviewed this pet");

		review.setPet(pet.get());
		review.setAdoption(adoptionEntity);

		log.info("Termina proceso de creación de la reseña");
		return reviewRepository.save(review);
	}

	/**
	 * Obtiene todas las reseñas registradas.
	 */
	@Transactional
	public List<ReviewEntity> getReviews() {
		log.info("Inicia proceso de consultar todas las reseñas");
		List<ReviewEntity> reviews = reviewRepository.findAll();
		if (reviews.isEmpty())
			log.info("No hay reseñas registradas");
		return reviews;
	}

	/**
	 * Obtiene las reseñas filtrando, de forma opcional, por mascota y/o adoptante.
	 * Si se proveen varios filtros, todos deben cumplirse.
	 */
	@Transactional
	public List<ReviewEntity> getReviews(Long petId, Long adopterId) {
		log.info("Inicia proceso de consultar reseñas filtradas");
		List<ReviewEntity> reviews = reviewRepository.findAll().stream()
				.filter(r -> petId == null || (r.getPet() != null && petId.equals(r.getPet().getId())))
				.filter(r -> adopterId == null || (r.getAdoption() != null && r.getAdoption().getAdopter() != null
						&& adopterId.equals(r.getAdoption().getAdopter().getId())))
				.toList();
		if (reviews.isEmpty())
			log.info("No hay reseñas registradas que cumplan los filtros");
		return reviews;
	}

	/**
	 * Obtiene una reseña a partir de su id.
	 */
	@Transactional
	public ReviewEntity getReview(Long reviewId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar la reseña con id = {}", reviewId);
		if (reviewId == null || reviewId <= 0)
			throw new IllegalOperationException("Review id is not valid");

		Optional<ReviewEntity> review = reviewRepository.findById(reviewId);
		if (review.isEmpty())
			throw new EntityNotFoundException("Review not found");

		log.info("Termina proceso de consultar la reseña con id = {}", reviewId);
		return review.get();
	}

	/**
	 * Actualiza una reseña existente.
	 */
	@Transactional
	public ReviewEntity updateReview(Long reviewId, ReviewEntity review)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la reseña con id = {}", reviewId);
		if (reviewId == null || reviewId <= 0)
			throw new IllegalOperationException("Review id is not valid");

		Optional<ReviewEntity> existing = reviewRepository.findById(reviewId);
		if (existing.isEmpty())
			throw new EntityNotFoundException("Review not found");

		if (review.getRating() == null)
			throw new IllegalOperationException("Rating cannot be null");
		if (review.getComment() == null || review.getComment().isBlank())
			throw new IllegalOperationException("Comment cannot be null or empty");
		if (review.getDate() == null)
			throw new IllegalOperationException("Date cannot be null");
		if (review.getTime() == null)
			throw new IllegalOperationException("Time cannot be null");
		if (review.getRating() < MIN_RATING || review.getRating() > MAX_RATING)
			throw new IllegalOperationException("Rating must be between " + MIN_RATING + " and " + MAX_RATING);

		ReviewEntity current = existing.get();
		review.setId(reviewId);
		review.setPet(current.getPet());
		review.setAdoption(current.getAdoption());

		log.info("Termina proceso de actualizar la reseña con id = {}", reviewId);
		return reviewRepository.save(review);
	}

	/**
	 * Borra una reseña a partir de su id. Solo el adoptante autor de la reseña
	 * puede solicitar su eliminación, por lo cual se recibe adicionalmente su id.
	 */
	@Transactional
	public void deleteReview(Long reviewId, Long adopterId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar la reseña con id = {}", reviewId);
		if (reviewId == null || reviewId <= 0)
			throw new IllegalOperationException("Review id is not valid");

		Optional<ReviewEntity> review = reviewRepository.findById(reviewId);
		if (review.isEmpty())
			throw new EntityNotFoundException("Review not found");

		ReviewEntity reviewEntity = review.get();
		if (reviewEntity.getAdoption() == null || reviewEntity.getAdoption().getAdopter() == null
				|| adopterId == null || !reviewEntity.getAdoption().getAdopter().getId().equals(adopterId))
			throw new IllegalOperationException("Only the adopter who authored the review can request its deletion");

		reviewRepository.deleteById(reviewId);
		log.info("Termina proceso de borrar la reseña con id = {}", reviewId);
	}
}
