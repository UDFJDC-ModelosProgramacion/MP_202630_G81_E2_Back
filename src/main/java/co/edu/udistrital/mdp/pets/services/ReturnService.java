package co.edu.udistrital.mdp.pets.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.ReturnEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.repositories.ReturnRepository;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;

import java.util.Date;
import java.util.List;

@Service

public class ReturnService {

    @Autowired
    private ReturnRepository returnRepository;

    @Autowired
    private AdoptionRepository adoptionRepository;

    @Transactional
    public ReturnEntity createReturn(ReturnEntity returnEntity, Long currentUserId, String currentUserRole) throws EntityNotFoundException, IllegalOperationException {
        if (returnEntity.getJustification() == null || returnEntity.getJustification().trim().isEmpty() ||
            returnEntity.getAdoption() == null) {
            throw new IllegalOperationException("Ningún atributo obligatorio puede ser nulo o vacío. Se debe proporcionar obligatoriamente la justificación del retorno y la adopción.");
        }

        AdoptionEntity adoption = adoptionRepository.findById(returnEntity.getAdoption().getId())
                .orElseThrow(() -> new EntityNotFoundException("La adopción asociada no existe. Solo se permite registrar un retorno para una adopción previamente completada."));
        
        // TODO: Verifica que la mascota pertenezca actualmente al usuario (ej. verificando adoption.getAdopter().getId() == currentUserId si es un adopter normal)
        
        // TODO: Verifica que la mascota no se encuentre ya en el refugio o devuelta (ej. adoption.getPet().getStatus() != "EN_REFUGIO")

        returnEntity.setAdoption(adoption);
        returnEntity.setDate(new Date());
        returnEntity.setStatus("SOLICITADO");
        
        return returnRepository.save(returnEntity);
    }

    @Transactional(readOnly = true)
    public ReturnEntity readReturn(Long id, Long currentUserId, String currentUserRole) throws EntityNotFoundException, IllegalOperationException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("No se aceptan identificadores inválidos.");
        }

        ReturnEntity returnEntity = returnRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Si el registro de retorno no existe, se muestra un mensaje indicándolo."));

        // TODO: Enlazar validación de usuario cuando los campos estén completamente mapeados (ej. returnEntity.getAdoption().getAdopter().getId())
        // if (!"ADMIN".equalsIgnoreCase(currentUserRole) && !returnEntity.getAdoption().getAdopter().getId().equals(currentUserId)) {
        //    throw new IllegalOperationException("Solo el usuario asociado al retorno y los administradores pueden visualizar los detalles.");
        // }

        return returnEntity;
    }

    @Transactional(readOnly = true)
    public List<ReturnEntity> readAllReturns(Long currentUserId, String currentUserRole) {
        if ("ADMIN".equalsIgnoreCase(currentUserRole)) {
            return returnRepository.findAll();
        }
        
        // Implement filter by user for non-admins
        return returnRepository.findAll(); // Simplified for now
    }

    @Transactional
    public ReturnEntity updateReturn(Long id, ReturnEntity returnUpdate, Long currentUserId, String currentUserRole) throws EntityNotFoundException, IllegalOperationException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("No se aceptan identificadores inválidos ni atributos nulos.");
        }

        ReturnEntity existingReturn = returnRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Retorno no encontrado."));

        if ("PROCESADO".equalsIgnoreCase(existingReturn.getStatus()) || "CERRADO".equalsIgnoreCase(existingReturn.getStatus())) {
            throw new IllegalOperationException("Una vez que el retorno ha sido procesado por el refugio, la justificación y los detalles enviados por el usuario son inmutables.");
        }

        if (returnUpdate.getStatus() != null && !existingReturn.getStatus().equalsIgnoreCase(returnUpdate.getStatus())) {
             if (!"ADMIN".equalsIgnoreCase(currentUserRole)) {
                 throw new IllegalOperationException("Solo los roles autorizados pueden actualizar el estado de procesamiento del retorno.");
             }
             existingReturn.setStatus(returnUpdate.getStatus().toUpperCase());
        }

        return returnRepository.save(existingReturn);
    }

    @Transactional
    public void deleteReturn(Long id, Long currentUserId, String currentUserRole) throws IllegalOperationException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("No se aceptan identificadores inválidos.");
        }
        
        // La regla dice que NO se puede eliminar por motivos de trazabilidad histórica
        throw new IllegalOperationException("No se permite eliminar un registro de retorno por motivos de trazabilidad histórica y legal.");
    }
}
