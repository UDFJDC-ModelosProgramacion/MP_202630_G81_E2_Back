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

	public static final String PENDING_STATUS = "PENDING";
	public static final String FINALIZED_STATUS = "FINALIZED";

	private static final String RETURN_ID_NOT_VALID = "Invalid identifiers are not accepted";
	private static final String RETURN_NOT_FOUND = "Return not found";

	private final ReturnRepository returnRepository;
	private final AdoptionRepository adoptionRepository;

	/**
	 * Registra la devolución de una mascota adoptada. La fecha, el estado inicial
	 * (PENDING) y el refugio (el de la adopción) los asigna el sistema.
	 */
	@Transactional
	public ReturnEntity createReturn(ReturnEntity returnEntity)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación de la devolución");

		if (returnEntity == null)
			throw new IllegalOperationException("Return cannot be null");
		if (returnEntity.getReason() == null || returnEntity.getReason().isBlank())
			throw new IllegalOperationException("The justification for the return must be provided");
		if (returnEntity.getAdoption() == null || returnEntity.getAdoption().getId() == null)
			throw new IllegalOperationException("Return must be associated with an existing adoption");

		AdoptionEntity adoption = adoptionRepository.findById(returnEntity.getAdoption().getId())
				.orElseThrow(() -> new EntityNotFoundException("Adoption not found"));

		if (!FINALIZED_STATUS.equalsIgnoreCase(adoption.getStatus()))
			throw new IllegalOperationException(
					"A return can only be registered for a previously completed adoption");

		validatePetNotAlreadyReturned(adoption);

		returnEntity.setId(null);
		returnEntity.setAdoption(adoption);
		returnEntity.setShelter(adoption.getShelter());
		returnEntity.setDate(new Date());
		returnEntity.setStatus(PENDING_STATUS);

		log.info("Termina proceso de creación de la devolución");
		return returnRepository.save(returnEntity);
	}

	private void validatePetNotAlreadyReturned(AdoptionEntity adoption) throws IllegalOperationException {
		boolean alreadyReturned = returnRepository.existsByAdoption_Id(adoption.getId())
				|| (adoption.getPet() != null && returnRepository.existsByAdoption_Pet_Id(adoption.getPet().getId()));
		if (alreadyReturned)
			throw new IllegalOperationException(
					"A return cannot be registered for a pet that has already been returned");
	}

	/**
	 * Obtiene una devolución a partir de su id.
	 */
	@Transactional(readOnly = true)
	public ReturnEntity readReturn(Long id) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar la devolución con id = {}", id);

		if (id == null || id <= 0)
			throw new IllegalOperationException(RETURN_ID_NOT_VALID);

		ReturnEntity returnEntity = returnRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException(RETURN_NOT_FOUND));

		log.info("Termina proceso de consultar la devolución con id = {}", id);
		return returnEntity;
	}

	/**
	 * Obtiene todas las devoluciones registradas.
	 */
	@Transactional(readOnly = true)
	public List<ReturnEntity> readAllReturns() {
		return readAllReturns(null, null, null);
	}

	/**
	 * Obtiene las devoluciones filtrando, de forma opcional, por refugio, mascota
	 * y usuario (adoptante). Si se combinan varios filtros, todos deben coincidir.
	 */
	@Transactional(readOnly = true)
	public List<ReturnEntity> readAllReturns(Long shelterId, Long petId, Long userId) {
		log.info("Inicia proceso de consultar las devoluciones");

		List<ReturnEntity> returns = returnRepository.findAll().stream()
				.filter(r -> shelterId == null
						|| (r.getShelter() != null && shelterId.equals(r.getShelter().getId())))
				.filter(r -> petId == null
						|| (r.getAdoption() != null && r.getAdoption().getPet() != null
								&& petId.equals(r.getAdoption().getPet().getId())))
				.filter(r -> userId == null
						|| (r.getAdoption() != null && r.getAdoption().getAdopter() != null
								&& userId.equals(r.getAdoption().getAdopter().getId())))
				.toList();

		if (returns.isEmpty())
			log.info("No hay devoluciones que cumplan los filtros");
		return returns;
	}

	/**
	 * Actualiza la justificación y el estado de una devolución. Una devolución
	 * procesada y finalizada no admite modificaciones.
	 */
	@Transactional
	public ReturnEntity updateReturn(Long id, ReturnEntity returnUpdate)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la devolución con id = {}", id);

		if (id == null || id <= 0)
			throw new IllegalOperationException(RETURN_ID_NOT_VALID);
		if (returnUpdate == null || returnUpdate.getReason() == null || returnUpdate.getReason().isBlank()
				|| returnUpdate.getStatus() == null || returnUpdate.getStatus().isBlank())
			throw new IllegalOperationException("Return reason and status cannot be null or empty");

		ReturnEntity current = returnRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException(RETURN_NOT_FOUND));

		if (FINALIZED_STATUS.equalsIgnoreCase(current.getStatus()))
			throw new IllegalOperationException(
					"A processed and finalized return does not allow modifications");

		current.setReason(returnUpdate.getReason());
		current.setStatus(normalizeStatus(returnUpdate.getStatus()));

		log.info("Termina proceso de actualizar la devolución con id = {}", id);
		return returnRepository.save(current);
	}

	private String normalizeStatus(String status) throws IllegalOperationException {
		if (PENDING_STATUS.equalsIgnoreCase(status.trim()))
			return PENDING_STATUS;
		if (FINALIZED_STATUS.equalsIgnoreCase(status.trim()))
			return FINALIZED_STATUS;
		throw new IllegalOperationException("Invalid return status");
	}

	/**
	 * Las devoluciones no se pueden eliminar por trazabilidad histórica. Se valida
	 * primero el id y su existencia, y luego se rechaza la operación.
	 */
	@Transactional
	public void deleteReturn(Long id) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de eliminar la devolución con id = {}", id);

		if (id == null || id <= 0)
			throw new IllegalOperationException(RETURN_ID_NOT_VALID);
		if (!returnRepository.existsById(id))
			throw new EntityNotFoundException(RETURN_NOT_FOUND);

		throw new IllegalOperationException(
				"A return record cannot be deleted for reasons of historical traceability");
	}
}