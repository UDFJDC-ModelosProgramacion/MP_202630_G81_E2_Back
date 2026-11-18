package co.edu.udistrital.mdp.pets.controllers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udistrital.mdp.pets.dto.ReviewAuthorDTO;
import co.edu.udistrital.mdp.pets.dto.ReviewDTO;
import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.ReviewEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.ReviewService;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@RestController
public class ReviewController {

    private final ReviewService reviewService;
    private final ModelMapper modelMapper;

    @GetMapping("/reviews")
    @ResponseStatus(code = HttpStatus.OK)
    public List<ReviewDTO> findAll(@RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long adopterId) {
        List<ReviewEntity> reviews = reviewService.getReviews(petId, adopterId);
        return reviews.stream().map(this::toReviewDTO).toList();
    }

    @GetMapping("/pets/{petId}/reviews/{reviewId}")
    @ResponseStatus(code = HttpStatus.OK)
    public ReviewDTO findOne(@PathVariable Long petId, @PathVariable Long reviewId)
            throws EntityNotFoundException, IllegalOperationException {
        ReviewEntity reviewEntity = reviewService.getReview(petId, reviewId);
        return toReviewDTO(reviewEntity);
    }

    @PostMapping("/pets/{petId}/reviews")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.canCreateReview(authentication, #petId, #reviewDTO)")
    @ResponseStatus(code = HttpStatus.CREATED)
    public ReviewDTO create(@PathVariable Long petId, @RequestBody ReviewDTO reviewDTO)
            throws EntityNotFoundException, IllegalOperationException {
        ReviewEntity reviewEntity = reviewService.createReview(petId, modelMapper.map(reviewDTO, ReviewEntity.class));
        return toReviewDTO(reviewEntity);
    }

    @PutMapping("/pets/{petId}/reviews/{reviewId}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.canManageReview(authentication, #petId, #reviewId, #adopterId)")
    @ResponseStatus(code = HttpStatus.OK)
    public ReviewDTO update(@PathVariable Long petId, @PathVariable Long reviewId,
            @RequestParam Long adopterId, @RequestBody ReviewDTO reviewDTO)
            throws EntityNotFoundException, IllegalOperationException {
        ReviewEntity reviewEntity = reviewService.updateReview(petId, reviewId, adopterId,
                modelMapper.map(reviewDTO, ReviewEntity.class));
        return toReviewDTO(reviewEntity);
    }

    @DeleteMapping("/pets/{petId}/reviews/{reviewId}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.canManageReview(authentication, #petId, #reviewId, #adopterId)")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long petId, @PathVariable Long reviewId, @RequestParam Long adopterId)
            throws EntityNotFoundException, IllegalOperationException {
        reviewService.deleteReview(petId, reviewId, adopterId);
    }

    private ReviewDTO toReviewDTO(ReviewEntity reviewEntity) {
        ReviewDTO dto = modelMapper.map(reviewEntity, ReviewDTO.class);
        if (reviewEntity.getAdoption() != null && reviewEntity.getAdoption().getAdopter() != null) {
            AdopterEntity adopter = reviewEntity.getAdoption().getAdopter();
            ReviewAuthorDTO author = new ReviewAuthorDTO();
            author.setId(adopter.getId());
            author.setDisplayName(Stream.of(adopter.getFirstName(), adopter.getLastName())
                    .filter(name -> name != null && !name.isBlank())
                    .map(String::trim)
                    .collect(Collectors.joining(" ")));
            dto.setAuthor(author);
        }
        return dto;
    }
}