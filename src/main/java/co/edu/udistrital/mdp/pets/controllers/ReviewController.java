package co.edu.udistrital.mdp.pets.controllers;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udistrital.mdp.pets.dto.ReviewDTO;
import co.edu.udistrital.mdp.pets.entities.ReviewEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.ReviewService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final ModelMapper modelMapper;

    
    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<ReviewDTO> findAll(@RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long adopterId) {
        List<ReviewEntity> reviews = reviewService.getReviews(petId, adopterId);
        return modelMapper.map(reviews, new TypeToken<List<ReviewDTO>>() {
        }.getType());
    }

    
    @GetMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public ReviewDTO findOne(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        ReviewEntity reviewEntity = reviewService.getReview(id);
        return modelMapper.map(reviewEntity, ReviewDTO.class);
    }

    
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public ReviewDTO create(@RequestBody ReviewDTO reviewDTO) throws EntityNotFoundException, IllegalOperationException {
        ReviewEntity reviewEntity = reviewService.createReview(modelMapper.map(reviewDTO, ReviewEntity.class));
        return modelMapper.map(reviewEntity, ReviewDTO.class);
    }

    
    @PutMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public ReviewDTO update(@PathVariable Long id, @RequestBody ReviewDTO reviewDTO)
            throws EntityNotFoundException, IllegalOperationException {
        ReviewEntity reviewEntity = reviewService.updateReview(id, modelMapper.map(reviewDTO, ReviewEntity.class));
        return modelMapper.map(reviewEntity, ReviewDTO.class);
    }

    
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestParam Long adopterId)
            throws EntityNotFoundException, IllegalOperationException {
        reviewService.deleteReview(id, adopterId);
    }
}