package co.edu.udistrital.mdp.ZZZ.services;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.ZZZ.entities.EventEntity;
import co.edu.udistrital.mdp.ZZZ.entities.ShelterEntity;
import co.edu.udistrital.mdp.ZZZ.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.ZZZ.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.ZZZ.repositories.EventRepository;
import co.edu.udistrital.mdp.ZZZ.repositories.ShelterRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EventService {

	@Autowired
	EventRepository eventRepository;

	@Autowired
	ShelterRepository shelterRepository;

	@Transactional
	public EventEntity createEvent(EventEntity event) throws EntityNotFoundException, IllegalOperationException {
		log.info("Starts event creation process");

		validateMandatoryAttributes(event);

		if (event.getShelter() == null || event.getShelter().getId() == null)
			throw new IllegalOperationException("Event must be associated with an existing shelter");
		Optional<ShelterEntity> shelter = shelterRepository.findById(event.getShelter().getId());
		if (shelter.isEmpty())
			throw new EntityNotFoundException("Shelter not found");

		if (event.getDate().before(today()))
			throw new IllegalOperationException("Event date cannot be earlier than the current date");

		event.setShelter(shelter.get());

		boolean duplicated = eventRepository.findAll().stream().anyMatch(e -> isSameEvent(e, event));
		if (duplicated)
			throw new IllegalOperationException(
					"An event already exists for this shelter with the same name, location and date");

		log.info("Event creation process ends");
		return eventRepository.save(event);
	}

	@Transactional
	public List<EventEntity> getEvents() {
		log.info("Start the process of consulting all events");
		List<EventEntity> events = eventRepository.findAll();
		if (events.isEmpty())
			log.info("There are no recorded events");
		return events;
	}

	@Transactional
	public EventEntity getEvent(Long eventId) throws EntityNotFoundException, IllegalOperationException {
		return getEvent(eventId, null, null);
	}

	@Transactional
	public EventEntity getEvent(Long eventId, String name, Date date)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Starts the process of consulting an event by filters");

		if (eventId != null && eventId <= 0)
			throw new IllegalOperationException("Event id is not valid");

		Optional<EventEntity> event = eventRepository.findAll().stream()
				.filter(e -> eventId == null || eventId.equals(e.getId()))
				.filter(e -> name == null || name.equalsIgnoreCase(e.getName()))
				.filter(e -> date == null || (e.getDate() != null && sameDay(e.getDate(), date)))
				.findFirst();

		if (event.isEmpty())
			throw new EntityNotFoundException("Event not found");

		log.info("Finishes the process of consulting an event by filters");
		return event.get();
	}

	@Transactional
	public EventEntity updateEvent(Long eventId, EventEntity event)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Starts process of updating the event with id = {}", eventId);
		if (eventId == null || eventId <= 0)
			throw new IllegalOperationException("Event id is not valid");

		Optional<EventEntity> existing = eventRepository.findById(eventId);
		if (existing.isEmpty())
			throw new EntityNotFoundException("Event not found");

		validateMandatoryAttributes(event);

		if (event.getDate().before(today()))
			throw new IllegalOperationException("Event date cannot be earlier than the current date");

		EventEntity current = existing.get();
		event.setShelter(current.getShelter());

		boolean duplicated = eventRepository.findAll().stream()
				.filter(e -> !e.getId().equals(eventId))
				.anyMatch(e -> isSameEvent(e, event));
		if (duplicated)
			throw new IllegalOperationException(
					"An event already exists for this shelter with the same name, location and date");

		event.setId(eventId);

		log.info("Finish process of updating event with id = {}", eventId);
		return eventRepository.save(event);
	}

	
	@Transactional
	public void deleteEvent(Long eventId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Starts process of deleting the event with id = {}", eventId);
		if (eventId == null || eventId <= 0)
			throw new IllegalOperationException("Event id is not valid");

		Optional<EventEntity> event = eventRepository.findById(eventId);
		if (event.isEmpty())
			throw new EntityNotFoundException("Event not found");

		if (event.get().getDate() != null && event.get().getDate().before(today()))
			throw new IllegalOperationException(
					"An event whose date has already passed cannot be deleted, in order to preserve the shelter's activity history");

		eventRepository.deleteById(eventId);
		log.info("Finish process of deleting the event with id = {}", eventId);
	}

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