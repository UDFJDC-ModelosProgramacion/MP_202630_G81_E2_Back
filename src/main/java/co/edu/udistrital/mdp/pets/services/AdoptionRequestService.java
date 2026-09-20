package co.edu.udistrital.mdp.pets.services;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionRequestEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdopterRepository;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRequestRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdoptionRequestService {

    private final AdoptionRequestRepository adoptionRequestRepository;
    private final AdopterRepository adopterRepository;
    private final PetRepository petRepository;

    private static final String REQUEST_ID_NOT_VALID = "Invalid identifiers are not accepted.";

    @Transactional
    public AdoptionRequestEntity createAdoptionRequest(
            AdoptionRequestEntity request,
            Long currentUserId,
            String currentUserRole)
            throws EntityNotFoundException, IllegalOperationException {

        log.info("Adoption request creation process started");

        if (request.getAdopter() == null || request.getPet() == null) {
            throw new IllegalOperationException(
                    "Required attributes cannot be null or empty (adopter and pet are required).");
        }

        AdopterEntity adopter = adopterRepository.findById(request.getAdopter().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "The requesting user does not exist or is not valid."));

        PetEntity pet = petRepository.findById(request.getPet().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "The requested pet does not exist."));

        // TODO: Validate pet availability according to its attributes
        // (e.g. pet.getStatus().equals("AVAILABLE"))

        List<AdoptionRequestEntity> activeRequests = adoptionRequestRepository
                .findByAdopterIdAndPetIdAndStatus(
                        adopter.getId(),
                        pet.getId(),
                        "PENDIENTE");

        if (!activeRequests.isEmpty()) {
            throw new IllegalOperationException(
                    "A user cannot have more than one active request for the same pet.");
        }

        request.setAdopter(adopter);
        request.setPet(pet);
        request.setStatus("PENDIENTE");
        request.setDate(new Date());

        AdoptionRequestEntity savedRequest = adoptionRequestRepository.save(request);

        log.info("Adoption request creation process completed");
        return savedRequest;
    }

    @Transactional(readOnly = true)
    public AdoptionRequestEntity readAdoptionRequest(
            Long id,
            Long currentUserId,
            String currentUserRole)
            throws EntityNotFoundException, IllegalOperationException {

        log.info("Adoption request query process started for id = {}", id);

        if (id == null || id <= 0) {
            throw new IllegalOperationException(REQUEST_ID_NOT_VALID);
        }

        AdoptionRequestEntity request = adoptionRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "If the request does not exist, an error message indicating this is displayed."));

        if (!"ADMIN".equalsIgnoreCase(currentUserRole)
                && !request.getAdopter().getId().equals(currentUserId)) {
            throw new IllegalOperationException(
                    "Only the user who created the request and authorized roles can view it.");
        }

        log.info("Adoption request query process completed for id = {}", id);
        return request;
    }

    @Transactional(readOnly = true)
    public List<AdoptionRequestEntity> readAllAdoptionRequests(
            Long currentUserId,
            String currentUserRole) {

        log.info("Process of querying all adoption requests started");

        List<AdoptionRequestEntity> requests;

        if ("ADMIN".equalsIgnoreCase(currentUserRole)) {
            requests = adoptionRequestRepository.findAll();
        } else {
            // TODO: Replace with a findByAdopterId(currentUserId) repository method when available
            requests = adoptionRequestRepository.findAll().stream()
                    .filter(r -> r.getAdopter().getId().equals(currentUserId))
                    .toList();
        }

        log.info("Process of querying all adoption requests completed");
        return requests;
    }

    @Transactional
    public AdoptionRequestEntity updateAdoptionRequest(
            Long id,
            AdoptionRequestEntity requestUpdate,
            Long currentUserId,
            String currentUserRole)
            throws EntityNotFoundException, IllegalOperationException {

        log.info("Adoption request update process started for id = {}", id);

        if (id == null || id <= 0) {
            throw new IllegalOperationException(REQUEST_ID_NOT_VALID);
        }

        AdoptionRequestEntity existingRequest = adoptionRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Adoption request not found."));

        if ("APROBADA".equalsIgnoreCase(existingRequest.getStatus())
                || "RECHAZADA".equalsIgnoreCase(existingRequest.getStatus())) {
            throw new IllegalOperationException(
                    "A request in a final state (approved or rejected) cannot be returned to pending status or modified.");
        }

        if (requestUpdate.getPet() != null
                && !existingRequest.getPet().getId().equals(requestUpdate.getPet().getId())) {
            throw new IllegalOperationException(
                    "The associated pet cannot be modified after creation.");
        }

        if (requestUpdate.getStatus() != null
                && !existingRequest.getStatus().equalsIgnoreCase(requestUpdate.getStatus())) {

            if (!"ADMIN".equalsIgnoreCase(currentUserRole)) {

                if ("CANCELADA".equalsIgnoreCase(requestUpdate.getStatus())
                        && existingRequest.getAdopter().getId().equals(currentUserId)) {

                    existingRequest.setStatus("CANCELADA");

                } else {
                    throw new IllegalOperationException(
                            "The request status can only be modified by administrators, unless the user is canceling their own request.");
                }

            } else {
                existingRequest.setStatus(requestUpdate.getStatus().toUpperCase());
            }
        }

        AdoptionRequestEntity savedRequest = adoptionRequestRepository.save(existingRequest);

        log.info("Adoption request update process completed for id = {}", id);
        return savedRequest;
    }

    @Transactional
    public void deleteAdoptionRequest(
            Long id,
            Long currentUserId,
            String currentUserRole)
            throws EntityNotFoundException, IllegalOperationException {

        log.info("Adoption request deletion process started for id = {}", id);

        if (id == null || id <= 0) {
            throw new IllegalOperationException(REQUEST_ID_NOT_VALID);
        }

        AdoptionRequestEntity existingRequest = adoptionRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "If the identifier does not exist, a message indicating this is displayed."));

        if ("APROBADA".equalsIgnoreCase(existingRequest.getStatus())
                || "RECHAZADA".equalsIgnoreCase(existingRequest.getStatus())) {
            throw new IllegalOperationException(
                    "A request that has already been processed cannot be physically deleted (only pending requests can be canceled).");
        }

        if (!"ADMIN".equalsIgnoreCase(currentUserRole)
                && !existingRequest.getAdopter().getId().equals(currentUserId)) {
            throw new IllegalOperationException(
                    "You do not have permission to delete or cancel this request.");
        }

        adoptionRequestRepository.delete(existingRequest);

        log.info("Adoption request deletion process completed for id = {}", id);
    }
}