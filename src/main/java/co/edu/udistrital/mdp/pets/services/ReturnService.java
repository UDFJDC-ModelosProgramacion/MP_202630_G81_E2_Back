package co.edu.udistrital.mdp.pets.services;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.ReturnEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.ReturnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnService {

    private final ReturnRepository returnRepository;
    private final AdoptionRepository adoptionRepository;

    private static final String RETURN_ID_NOT_VALID = "Invalid identifiers are not accepted.";

    @Transactional
    public ReturnEntity createReturn(
            ReturnEntity returnEntity,
            Long currentUserId,
            String currentUserRole)
            throws EntityNotFoundException, IllegalOperationException {

        log.info("Return creation process started");

        if (returnEntity.getReason() == null
                || returnEntity.getReason().trim().isEmpty()
                || returnEntity.getAdoption() == null) {

            throw new IllegalOperationException(
                    "Required attributes cannot be null or empty. "
                    + "The return justification and adoption must be provided.");
        }

        AdoptionEntity adoption = adoptionRepository.findById(returnEntity.getAdoption().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "The associated adoption does not exist. "
                        + "A return can only be registered for a previously completed adoption."));


        returnEntity.setAdoption(adoption);
        returnEntity.setDate(new Date());
        returnEntity.setStatus("SOLICITADO");

        ReturnEntity savedReturn = returnRepository.save(returnEntity);

        log.info("Return creation process completed");
        return savedReturn;
    }

    @Transactional(readOnly = true)
    public ReturnEntity readReturn(
            Long id,
            Long currentUserId,
            String currentUserRole)
            throws EntityNotFoundException, IllegalOperationException {

        log.info("Return query process started for id = {}", id);

        if (id == null || id <= 0) {
            throw new IllegalOperationException(RETURN_ID_NOT_VALID);
        }

        ReturnEntity returnEntity = returnRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "If the return record does not exist, an error message is displayed."));

        log.info("Return query process completed for id = {}", id);
        return returnEntity;
    }

    @Transactional(readOnly = true)
    public List<ReturnEntity> readAllReturns(
            Long currentUserId,
            String currentUserRole) {

        log.info("Process of querying all returns started");

        List<ReturnEntity> returns;

        if ("ADMIN".equalsIgnoreCase(currentUserRole)) {
            returns = returnRepository.findAll();
        } else {
            returns = returnRepository.findAll();
        }

        log.info("Process of querying all returns completed");
        return returns;
    }

    @Transactional
    public ReturnEntity updateReturn(
            Long id,
            ReturnEntity returnUpdate,
            Long currentUserId,
            String currentUserRole)
            throws EntityNotFoundException, IllegalOperationException {

        log.info("Return update process started for id = {}", id);

        if (id == null || id <= 0) {
            throw new IllegalOperationException(
                    "Invalid identifiers or null attributes are not accepted.");
        }

        ReturnEntity existingReturn = returnRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Return not found."));

        if ("PROCESADO".equalsIgnoreCase(existingReturn.getStatus())
                || "CERRADO".equalsIgnoreCase(existingReturn.getStatus())) {

            throw new IllegalOperationException(
                    "Once the return has been processed by the shelter, "
                    + "the justification and details submitted by the user are immutable.");
        }

        if (returnUpdate.getStatus() != null
                && !existingReturn.getStatus().equalsIgnoreCase(returnUpdate.getStatus())) {

            if (!"ADMIN".equalsIgnoreCase(currentUserRole)) {
                throw new IllegalOperationException(
                        "Only authorized roles can update the return processing status.");
            }

            existingReturn.setStatus(returnUpdate.getStatus().toUpperCase());
        }

        ReturnEntity savedReturn = returnRepository.save(existingReturn);

        log.info("Return update process completed for id = {}", id);
        return savedReturn;
    }

    @Transactional
    public void deleteReturn(
            Long id,
            Long currentUserId,
            String currentUserRole)
            throws IllegalOperationException {

        log.info("Return deletion process started for id = {}", id);

        if (id == null || id <= 0) {
            throw new IllegalOperationException(RETURN_ID_NOT_VALID);
        }

        // The rule states that returns CANNOT be deleted for historical traceability purposes.
        throw new IllegalOperationException(
                "Return records cannot be deleted due to historical and legal traceability requirements.");
    }
}