package co.edu.udistrital.mdp.pets.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.AdoptionRequestEntity;
import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRequestRepository;
import co.edu.udistrital.mdp.pets.repositories.AdopterRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;

import java.util.Date;
import java.util.List;

@Service
public class AdoptionRequestService {

    @Autowired
    private AdoptionRequestRepository adoptionRequestRepository;

    @Autowired
    private AdopterRepository adopterRepository;

    @Autowired
    private PetRepository petRepository;

    @Transactional
    public AdoptionRequestEntity createAdoptionRequest(AdoptionRequestEntity request, Long currentUserId, String currentUserRole) throws EntityNotFoundException, IllegalOperationException {
        if (request.getAdopter() == null || request.getPet() == null) {
            throw new IllegalOperationException("Ningún atributo obligatorio puede ser nulo o vacío (adopter y pet son requeridos).");
        }

        AdopterEntity adopter = adopterRepository.findById(request.getAdopter().getId())
                .orElseThrow(() -> new EntityNotFoundException("El usuario solicitante no existe o no es válido."));
        
        PetEntity pet = petRepository.findById(request.getPet().getId())
                .orElseThrow(() -> new EntityNotFoundException("La mascota solicitada no existe."));

        // TODO: Validate if pet is available for adoption based on PetEntity's specific attributes (e.g., pet.getStatus().equals("AVAILABLE"))
        
        List<AdoptionRequestEntity> activeRequests = adoptionRequestRepository
                .findByAdopterIdAndPetIdAndStatus(adopter.getId(), pet.getId(), "PENDIENTE");
        
        if (!activeRequests.isEmpty()) {
            throw new IllegalOperationException("Un usuario no puede tener más de una solicitud activa para la misma mascota.");
        }

        request.setAdopter(adopter);
        request.setPet(pet);
        request.setStatus("PENDIENTE");
        request.setDate(new Date());

        return adoptionRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public AdoptionRequestEntity readAdoptionRequest(Long id, Long currentUserId, String currentUserRole) throws EntityNotFoundException, IllegalOperationException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("No se aceptan identificadores inválidos.");
        }

        AdoptionRequestEntity request = adoptionRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Si la solicitud no existe, se muestra un mensaje de error indicándolo."));

        if (!"ADMIN".equalsIgnoreCase(currentUserRole) && !request.getAdopter().getId().equals(currentUserId)) {
            throw new IllegalOperationException("Solo el usuario creador de la solicitud y los roles autorizados pueden visualizarla.");
        }

        return request;
    }

    @Transactional(readOnly = true)
    public List<AdoptionRequestEntity> readAllAdoptionRequests(Long currentUserId, String currentUserRole) {
        if ("ADMIN".equalsIgnoreCase(currentUserRole)) {
            return adoptionRequestRepository.findAll();
        }
        // In a real scenario we'd create a repository method findByAdopterId(currentUserId)
        return adoptionRequestRepository.findAll().stream()
                .filter(r -> r.getAdopter().getId().equals(currentUserId))
                .toList();
    }

    @Transactional
    public AdoptionRequestEntity updateAdoptionRequest(Long id, AdoptionRequestEntity requestUpdate, Long currentUserId, String currentUserRole) throws EntityNotFoundException, IllegalOperationException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("No se aceptan identificadores inválidos.");
        }

        AdoptionRequestEntity existingRequest = adoptionRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada."));

        if ("APROBADA".equalsIgnoreCase(existingRequest.getStatus()) || "RECHAZADA".equalsIgnoreCase(existingRequest.getStatus())) {
            throw new IllegalOperationException("Una solicitud en estado finalizado (aprobada o rechazada) no puede volver a estado pendiente o modificarse.");
        }

        // We check if the requestUpdate attempts to change the pet or user (not allowed)
        if (requestUpdate.getPet() != null && !existingRequest.getPet().getId().equals(requestUpdate.getPet().getId())) {
             throw new IllegalOperationException("La mascota asociada no puede ser modificada tras la creación.");
        }

        if (requestUpdate.getStatus() != null && !existingRequest.getStatus().equalsIgnoreCase(requestUpdate.getStatus())) {
            // Check roles
            if (!"ADMIN".equalsIgnoreCase(currentUserRole)) {
                if ("CANCELADA".equalsIgnoreCase(requestUpdate.getStatus()) && existingRequest.getAdopter().getId().equals(currentUserId)) {
                    // Valid case: user cancelling their own request
                    existingRequest.setStatus("CANCELADA");
                } else {
                    throw new IllegalOperationException("El estado de la solicitud solo puede ser modificado por administradores, a menos que sea el usuario cancelando su propia solicitud.");
                }
            } else {
                existingRequest.setStatus(requestUpdate.getStatus().toUpperCase());
            }
        }

        return adoptionRequestRepository.save(existingRequest);
    }

    @Transactional
    public void deleteAdoptionRequest(Long id, Long currentUserId, String currentUserRole) throws EntityNotFoundException, IllegalOperationException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("No se aceptan identificadores inválidos.");
        }

        AdoptionRequestEntity existingRequest = adoptionRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Si el identificador no existe, se muestra un mensaje indicándolo."));

        if ("APROBADA".equalsIgnoreCase(existingRequest.getStatus()) || "RECHAZADA".equalsIgnoreCase(existingRequest.getStatus())) {
            throw new IllegalOperationException("No se puede eliminar físicamente una solicitud que ya haya sido procesada (solo se permite cancelación de pendientes).");
        }

        if (!"ADMIN".equalsIgnoreCase(currentUserRole) && !existingRequest.getAdopter().getId().equals(currentUserId)) {
             throw new IllegalOperationException("No tiene permisos para eliminar o cancelar esta solicitud.");
        }

        adoptionRequestRepository.delete(existingRequest);
    }
}
