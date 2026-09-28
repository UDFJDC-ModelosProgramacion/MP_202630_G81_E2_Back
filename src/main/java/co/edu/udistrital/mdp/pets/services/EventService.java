package co.edu.udistrital.mdp.pets.services;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.EventEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.EventRepository;
import co.edu.udistrital.mdp.pets.repositories.ShelterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

	private final EventRepository eventRepository;
	private final ShelterRepository shelterRepository;

	private static final String EVENT_ID_NOT_VALID = "Event id is not valid";
	private static final String EVENT_NOT_FOUND = "Event not found";
	private static final String SHELTER_NOT_FOUND = "Shelter not found";
	private static final String EVENT_NOT_ASSOCIATED_TO_SHELTER = "The event is not associated with the shelter";

	@Transactional
	public EventEntity createEvent(Long shelterId, EventEntity event)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Starts event creation process for shelter with id = {}", shelterId);

		ShelterEntity shelter = findShelterOrThrow(shelterId);

		validateMandatoryAttributes(event);

		if (event.getDate().before(today()))
			throw new IllegalOperationException("Event date cannot be earlier than the current date");

		event.setShelter(shelter);

		boolean duplicated = eventRepository.findAll().stream().anyMatch(e -> isSameEvent(e, event));
		if (duplicated)
			throw new IllegalOperationException(
					"An event already exists for this shelter with the same name, location and date");

		log.info("Event creation process ends");
		return eventRepository.save(event);
	}

	@Transactional
	public List<EventEntity> getEvents(Long shelterId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Starts the process of consulting events for shelter with id = {}", shelterId);

		findShelterOrThrow(shelterId);

		List<EventEntity> events = eventRepository.findAll().stream()
				.filter(e -> e.getShelter() != null && e.getShelter().getId().equals(shelterId))
				.toList();

		if (events.isEmpty())
			log.info("There are no recorded events for shelter with id = {}", shelterId);

		log.info("Finishes the process of consulting events for shelter with id = {}", shelterId);
		return events;
	}

	@Transactional
	public EventEntity getEvent(Long shelterId, Long eventId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Starts the process of consulting event with id = {} of shelter with id = {}", eventId, shelterId);

		findShelterOrThrow(shelterId);
		EventEntity event = findEventOrThrow(eventId);
		validateBelongsToShelter(event, shelterId);

		log.info("Finishes the process of consulting event with id = {} of shelter with id = {}", eventId, shelterId);
		return event;
	}

	@Transactional
	public EventEntity updateEvent(Long shelterId, Long eventId, EventEntity event)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Starts process of updating event with id = {} of shelter with id = {}", eventId, shelterId);

		findShelterOrThrow(shelterId);
		EventEntity current = findEventOrThrow(eventId);
		validateBelongsToShelter(current, shelterId);

		validateMandatoryAttributes(event);

		if (event.getDate().before(today()))
			throw new IllegalOperationException("Event date cannot be earlier than the current date");

		event.setShelter(current.getShelter());
		event.setId(eventId);

		boolean duplicated = eventRepository.findAll().stream()
				.filter(e -> !e.getId().equals(eventId))
				.anyMatch(e -> isSameEvent(e, event));
		if (duplicated)
			throw new IllegalOperationException(
					"An event already exists for this shelter with the same name, location and date");

		log.info("Finish process of updating event with id = {} of shelter with id = {}", eventId, shelterId);
		return eventRepository.save(event);
	}

	@Transactional
	public void deleteEvent(Long shelterId, Long eventId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Starts process of deleting event with id = {} of shelter with id = {}", eventId, shelterId);

		findShelterOrThrow(shelterId);
		EventEntity event = findEventOrThrow(eventId);
		validateBelongsToShelter(event, shelterId);

		if (event.getDate() != null && event.getDate().before(today()))
			throw new IllegalOperationException(
					"An event whose date has already passed cannot be deleted, in order to preserve the shelter's activity history");

		eventRepository.deleteById(eventId);
		log.info("Finish process of deleting event with id = {} of shelter with id = {}", eventId, shelterId);
	}

	// --- Helpers de validación de existencia / pertenencia ---

	private ShelterEntity findShelterOrThrow(Long shelterId) throws EntityNotFoundException, IllegalOperationException {
		if (shelterId == null || shelterId <= 0)
			throw new IllegalOperationException("Shelter id is not valid");

		Optional<ShelterEntity> shelter = shelterRepository.findById(shelterId);
		if (shelter.isEmpty())
			throw new EntityNotFoundException(SHELTER_NOT_FOUND);

		return shelter.get();
	}

	private EventEntity findEventOrThrow(Long eventId) throws EntityNotFoundException, IllegalOperationException {
		if (eventId == null || eventId <= 0)
			throw new IllegalOperationException(EVENT_ID_NOT_VALID);

		Optional<EventEntity> event = eventRepository.findById(eventId);
		if (event.isEmpty())
			throw new EntityNotFoundException(EVENT_NOT_FOUND);

		return event.get();
	}

	private void validateBelongsToShelter(EventEntity event, Long shelterId) throws IllegalOperationException {
		if (event.getShelter() == null || !event.getShelter().getId().equals(shelterId))
			throw new IllegalOperationException(EVENT_NOT_ASSOCIATED_TO_SHELTER);
	}

	// --- Reglas de negocio existentes (sin cambios) ---

	private void validateMandatoryAttributes(EventEntity event) throws IllegalOperationException {
		if (event.getName() == null || event.getName().isBlank())
			throw new IllegalOperationException("Name cannot be null or empty");
		if (event.getDate() == null)
			throw new IllegalOperationException("Date cannot be null");
		if (event.getTime() == null || event.getTime().isBlank())
			throw new IllegalOperationException("Time cannot be null or empty");
		if (event.getDescription() == null || event.getDescription().isBlank())
			throw new IllegalOperationException("Description cannot be null or empty");
		if (event.getLocation() == null || event.getLocation().isBlank())
			throw new IllegalOperationException("Location cannot be null or empty");
	}

	private boolean isSameEvent(EventEntity e1, EventEntity e2) {
		return e1.getShelter() != null && e2.getShelter() != null
				&& e1.getShelter().getId() != null
				&& e1.getShelter().getId().equals(e2.getShelter().getId())
				&& e1.getName() != null && e1.getName().equalsIgnoreCase(e2.getName())
				&& e1.getLocation() != null && e1.getLocation().equalsIgnoreCase(e2.getLocation())
				&& e1.getDate() != null && e2.getDate() != null && sameDay(e1.getDate(), e2.getDate());
	}

	private boolean sameDay(Date d1, Date d2) {
		return truncate(d1).equals(truncate(d2));
	}

	private Date truncate(Date date) {
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.setTime(date);
		c.set(java.util.Calendar.HOUR_OF_DAY, 0);
		c.set(java.util.Calendar.MINUTE, 0);
		c.set(java.util.Calendar.SECOND, 0);
		c.set(java.util.Calendar.MILLISECOND, 0);
		return c.getTime();
	}

	private Date today() {
		return truncate(new Date());
	}
}