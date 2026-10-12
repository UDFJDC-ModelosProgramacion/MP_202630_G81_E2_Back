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
	private static final String REVIEW_ID_NOT_VALID = "Review id is not valid";
	private static final String REVIEW_NOT_FOUND = "Review not found";
	private static final String NOT_AUTHOR = "Only the adopter who authored the review can perform this operation";

	private final ReviewRepository reviewRepository;
	private final PetRepository petRepository;
	private final AdoptionRepository adoptionRepository;

	
	
	
	@Transactional
	public ReviewEntity createReview(Long petId, ReviewEntity review)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación de la reseña para la mascota con id = {}", petId);

		validateMandatoryAttributes(review);

		PetEntity pet = resolvePet(petId);
		AdoptionEntity adoption = resolveFinalizedAdoption(review, pet);

		validateNotAlreadyReviewed(pet, adoption);

		review.setPet(pet);
		review.setAdoption(adoption);

		log.info("Termina proceso de creación de la reseña");
		return reviewRepository.save(review);
	}

	private void validateMandatoryAttributes(ReviewEntity review) throws IllegalOperationException {
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
	}

	private PetEntity resolvePet(Long petId) throws EntityNotFoundException, IllegalOperationException {
		if (petId == null)
			throw new IllegalOperationException("Pet id is not valid");
		Optional<PetEntity> pet = petRepository.findById(petId);
		if (pet.isEmpty())
			throw new EntityNotFoundException("Pet not found");
		return pet.get();
	}

	private AdoptionEntity resolveFinalizedAdoption(ReviewEntity review, PetEntity pet)
			throws EntityNotFoundException, IllegalOperationException {
		if (review.getAdoption() == null || review.getAdoption().getId() == null)
			throw new IllegalOperationException("Review must be associated with an existing adopter");
		Optional<AdoptionEntity> adoption = adoptionRepository.findById(review.getAdoption().getId());
		if (adoption.isEmpty())
			throw new EntityNotFoundException("Adoption not found");

		AdoptionEntity adoptionEntity = adoption.get();
		if (adoptionEntity.getAdopter() == null)
			throw new IllegalOperationException("Review must be associated with an existing adopter");

		if (adoptionEntity.getPet() == null || !adoptionEntity.getPet().getId().equals(pet.getId())
				|| !FINALIZED_STATUS.equalsIgnoreCase(adoptionEntity.getStatus()))
			throw new IllegalOperationException(
					"The adopter can only review a pet with which it has had a finalized adoption process");

		return adoptionEntity;
	}

	private void validateNotAlreadyReviewed(PetEntity pet, AdoptionEntity adoption) throws IllegalOperationException {
		boolean alreadyReviewed = reviewRepository.existsByPetIdAndAdoptionAdopterId(pet.getId(),
				adoption.getAdopter().getId());
		if (alreadyReviewed)
			throw new IllegalOperationException("This adopter has already reviewed this pet");
	}

	
	
	
	@Transactional
	public List<ReviewEntity> getReviews() {
		log.info("Inicia proceso de consultar todas las reseñas");
		List<ReviewEntity> reviews = reviewRepository.findAll();
		if (reviews.isEmpty())
			log.info("No hay reseñas registradas");
		return reviews;
	}

	
	
	
	
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

	
	
	
	
	
	@Transactional
	public ReviewEntity getReview(Long petId, Long reviewId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar la reseña con id = {}", reviewId);
		ReviewEntity review = resolveReview(reviewId);
		validateBelongsToPet(review, petId);

		log.info("Termina proceso de consultar la reseña con id = {}", reviewId);
		return review;
	}

	
	
	
	
	@Transactional
	public ReviewEntity updateReview(Long petId, Long reviewId, Long adopterId, ReviewEntity review)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la reseña con id = {}", reviewId);

		ReviewEntity current = resolveReview(reviewId);
		validateBelongsToPet(current, petId);
		validateOwnership(current, adopterId);

		validateMandatoryAttributes(review);

		
		current.setRating(review.getRating());
		current.setComment(review.getComment());
		current.setDate(review.getDate());
		current.setTime(review.getTime());

		log.info("Termina proceso de actualizar la reseña con id = {}", reviewId);
		return reviewRepository.save(current);
	}

	
	
	
	
	@Transactional
	public void deleteReview(Long petId, Long reviewId, Long adopterId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar la reseña con id = {}", reviewId);

		ReviewEntity review = resolveReview(reviewId);
		validateBelongsToPet(review, petId);
		validateOwnership(review, adopterId);

		reviewRepository.deleteById(reviewId);
		log.info("Termina proceso de borrar la reseña con id = {}", reviewId);
	}

	private ReviewEntity resolveReview(Long reviewId) throws EntityNotFoundException, IllegalOperationException {
		if (reviewId == null || reviewId <= 0)
			throw new IllegalOperationException(REVIEW_ID_NOT_VALID);
		Optional<ReviewEntity> review = reviewRepository.findById(reviewId);
		if (review.isEmpty())
			throw new EntityNotFoundException(REVIEW_NOT_FOUND);
		return review.get();
	}

	private void validateBelongsToPet(ReviewEntity review, Long petId) throws EntityNotFoundException {
		if (petId == null || review.getPet() == null || !review.getPet().getId().equals(petId))
			throw new EntityNotFoundException(REVIEW_NOT_FOUND);
	}

	private void validateOwnership(ReviewEntity review, Long adopterId) throws IllegalOperationException {
		if (review.getAdoption() == null || review.getAdoption().getAdopter() == null
				|| adopterId == null || !review.getAdoption().getAdopter().getId().equals(adopterId))
			throw new IllegalOperationException(NOT_AUTHOR);
	}
}