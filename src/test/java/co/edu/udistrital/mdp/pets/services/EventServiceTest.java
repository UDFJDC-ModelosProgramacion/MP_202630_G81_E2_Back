package co.edu.udistrital.mdp.pets.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.EventEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(EventService.class)
class EventServiceTest {

	@Autowired
	private EventService eventService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<EventEntity> eventList = new ArrayList<>();
	private ShelterEntity shelter = new ShelterEntity();
	private ShelterEntity otherShelter = new ShelterEntity();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from EventEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ShelterEntity").executeUpdate();
	}

	private void insertData() {
		shelter = factory.manufacturePojo(ShelterEntity.class);
		entityManager.persist(shelter);

		otherShelter = factory.manufacturePojo(ShelterEntity.class);
		entityManager.persist(otherShelter);

		for (int i = 0; i < 3; i++) {
			EventEntity eventEntity = factory.manufacturePojo(EventEntity.class);
			eventEntity.setDate(futureDate(10 + i));
			eventEntity.setShelter(shelter);
			entityManager.persist(eventEntity);
			eventList.add(eventEntity);
		}
	}

	private Date futureDate(int daysFromNow) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, daysFromNow);
		return c.getTime();
	}

	private Date pastDate(int daysAgo) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, -daysAgo);
		return c.getTime();
	}

	// createEvent
	@Test
	void testCreateEvent() throws EntityNotFoundException, IllegalOperationException {
		EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
		newEntity.setDate(futureDate(5));

		EventEntity result = eventService.createEvent(shelter.getId(), newEntity);

		assertNotNull(result);
		EventEntity entity = entityManager.find(EventEntity.class, result.getId());
		assertEquals(newEntity.getName(), entity.getName());
		assertEquals(newEntity.getLocation(), entity.getLocation());
		assertEquals(shelter.getId(), entity.getShelter().getId());
	}

	@Test
	void testCreateEventWithTodayDate() throws EntityNotFoundException, IllegalOperationException {
		EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
		newEntity.setDate(new Date());

		EventEntity result = eventService.createEvent(shelter.getId(), newEntity);

		assertNotNull(result);
	}

	@Test
	void testCreateEventWithNullShelterId() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			eventService.createEvent(null, newEntity);
		});
	}

	@Test
	void testCreateEventWithInvalidShelterId() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			eventService.createEvent(0L, newEntity);
		});
	}

	@Test
	void testCreateEventWithNonExistentShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			eventService.createEvent(1000L, newEntity);
		});
	}

	@Test
	void testCreateEventWithNullName() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			newEntity.setName(null);
			eventService.createEvent(shelter.getId(), newEntity);
		});
	}

	@Test
	void testCreateEventWithBlankName() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			newEntity.setName("   ");
			eventService.createEvent(shelter.getId(), newEntity);
		});
	}

	@Test
	void testCreateEventWithNullDate() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(null);
			eventService.createEvent(shelter.getId(), newEntity);
		});
	}

	@Test
	void testCreateEventWithNullTime() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			newEntity.setTime(null);
			eventService.createEvent(shelter.getId(), newEntity);
		});
	}

	@Test
	void testCreateEventWithBlankTime() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			newEntity.setTime("  ");
			eventService.createEvent(shelter.getId(), newEntity);
		});
	}

	@Test
	void testCreateEventWithNullDescription() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			newEntity.setDescription(null);
			eventService.createEvent(shelter.getId(), newEntity);
		});
	}

	@Test
	void testCreateEventWithNullLocation() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			newEntity.setLocation(null);
			eventService.createEvent(shelter.getId(), newEntity);
		});
	}

	@Test
	void testCreateEventWithPastDate() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(pastDate(5));
			eventService.createEvent(shelter.getId(), newEntity);
		});
	}

	@Test
	void testCreateDuplicatedEvent() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity existing = eventList.get(0);
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setName(existing.getName());
			newEntity.setLocation(existing.getLocation());
			newEntity.setDate(existing.getDate());
			eventService.createEvent(shelter.getId(), newEntity);
		});
	}

	// getEvents
	@Test
	void testGetEvents() throws EntityNotFoundException, IllegalOperationException {
		List<EventEntity> list = eventService.getEvents(shelter.getId());
		assertEquals(eventList.size(), list.size());
	}

	@Test
	void testGetEventsEmpty() throws EntityNotFoundException, IllegalOperationException {
		List<EventEntity> list = eventService.getEvents(otherShelter.getId());
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetEventsWithInvalidShelterId() {
		assertThrows(IllegalOperationException.class, () -> {
			eventService.getEvents(0L);
		});
	}

	@Test
	void testGetEventsWithNonExistentShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			eventService.getEvents(1000L);
		});
	}

	// getEvent (the service only receives the eventId, no shelter validation)
	@Test
	void testGetEvent() throws EntityNotFoundException, IllegalOperationException {
		EventEntity entity = eventList.get(0);
		EventEntity result = eventService.getEvent(entity.getId());
		assertNotNull(result);
		assertEquals(entity.getId(), result.getId());
	}

	@Test
	void testGetEventWithNullId() {
		assertThrows(IllegalOperationException.class, () -> {
			eventService.getEvent(null);
		});
	}

	@Test
	void testGetEventWithInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			eventService.getEvent(0L);
		});
	}

	@Test
	void testGetEventWithNegativeId() {
		assertThrows(IllegalOperationException.class, () -> {
			eventService.getEvent(-1L);
		});
	}

	@Test
	void testGetNonExistentEvent() {
		assertThrows(EntityNotFoundException.class, () -> {
			eventService.getEvent(1000L);
		});
	}

	// searchEvents
	@Test
	void testSearchEventsWithoutFilters() throws IllegalOperationException {
		List<EventEntity> list = eventService.searchEvents(null, null, null);
		assertEquals(eventList.size(), list.size());
	}

	@Test
	void testSearchEventsById() throws IllegalOperationException {
		EventEntity entity = eventList.get(0);
		List<EventEntity> list = eventService.searchEvents(entity.getId(), null, null);
		assertEquals(1, list.size());
		assertEquals(entity.getId(), list.get(0).getId());
	}

	@Test
	void testSearchEventsByNameIgnoringCase() throws IllegalOperationException {
		EventEntity entity = eventList.get(1);
		List<EventEntity> list = eventService.searchEvents(null, entity.getName().toUpperCase(), null);
		assertEquals(1, list.size());
		assertEquals(entity.getId(), list.get(0).getId());
	}

	@Test
	void testSearchEventsByDate() throws IllegalOperationException {
		EventEntity entity = eventList.get(2);
		List<EventEntity> list = eventService.searchEvents(null, null, entity.getDate());
		assertEquals(1, list.size());
		assertEquals(entity.getId(), list.get(0).getId());
	}

	@Test
	void testSearchEventsCombinedFilters() throws IllegalOperationException {
		EventEntity entity = eventList.get(0);
		List<EventEntity> list = eventService.searchEvents(entity.getId(), entity.getName(), entity.getDate());
		assertEquals(1, list.size());
		assertEquals(entity.getId(), list.get(0).getId());
	}

	@Test
	void testSearchEventsCombinedFiltersNoMatch() throws IllegalOperationException {
		EventEntity entity = eventList.get(0);
		List<EventEntity> list = eventService.searchEvents(entity.getId(), "nombre-que-no-existe", null);
		assertTrue(list.isEmpty());
	}

	@Test
	void testSearchEventsNoMatch() throws IllegalOperationException {
		List<EventEntity> list = eventService.searchEvents(1000L, null, null);
		assertTrue(list.isEmpty());
	}

	@Test
	void testSearchEventsWithInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			eventService.searchEvents(0L, null, null);
		});
	}

	@Test
	void testSearchEventsWithNegativeId() {
		assertThrows(IllegalOperationException.class, () -> {
			eventService.searchEvents(-5L, null, null);
		});
	}

	// updateEvent
	@Test
	void testUpdateEvent() throws EntityNotFoundException, IllegalOperationException {
		EventEntity entity = eventList.get(0);
		EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setDate(futureDate(20));

		eventService.updateEvent(shelter.getId(), entity.getId(), pojoEntity);

		EventEntity resp = entityManager.find(EventEntity.class, entity.getId());
		assertEquals(pojoEntity.getName(), resp.getName());
		assertEquals(pojoEntity.getLocation(), resp.getLocation());
		assertEquals(shelter.getId(), resp.getShelter().getId());
	}

	@Test
	void testUpdateEventCannotChangeShelter() throws EntityNotFoundException, IllegalOperationException {
		EventEntity entity = eventList.get(0);

		EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setDate(futureDate(20));
		pojoEntity.setShelter(otherShelter);

		eventService.updateEvent(shelter.getId(), entity.getId(), pojoEntity);

		EventEntity resp = entityManager.find(EventEntity.class, entity.getId());
		assertEquals(shelter.getId(), resp.getShelter().getId());
	}

	@Test
	void testUpdateEventWithInvalidShelterId() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity entity = eventList.get(0);
			EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setDate(futureDate(20));
			eventService.updateEvent(0L, entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateEventWithNonExistentShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			EventEntity entity = eventList.get(0);
			EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setDate(futureDate(20));
			eventService.updateEvent(1000L, entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateEventInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
			pojoEntity.setDate(futureDate(5));
			eventService.updateEvent(shelter.getId(), 0L, pojoEntity);
		});
	}

	@Test
	void testUpdateNonExistentEvent() {
		assertThrows(EntityNotFoundException.class, () -> {
			EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
			pojoEntity.setDate(futureDate(5));
			eventService.updateEvent(shelter.getId(), 1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateEventNotBelongingToShelter() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity entity = eventList.get(0);
			EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setDate(futureDate(20));
			eventService.updateEvent(otherShelter.getId(), entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateEventWithNullName() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity entity = eventList.get(0);
			EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setDate(futureDate(20));
			pojoEntity.setName(null);
			eventService.updateEvent(shelter.getId(), entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateEventWithPastDate() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity entity = eventList.get(0);
			EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setDate(pastDate(5));
			eventService.updateEvent(shelter.getId(), entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateEventDuplicated() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity target = eventList.get(1);
			EventEntity other = eventList.get(0);

			EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
			pojoEntity.setId(target.getId());
			pojoEntity.setName(other.getName());
			pojoEntity.setLocation(other.getLocation());
			pojoEntity.setDate(other.getDate());

			eventService.updateEvent(shelter.getId(), target.getId(), pojoEntity);
		});
	}

	// deleteEvent
	@Test
	void testDeleteEvent() throws EntityNotFoundException, IllegalOperationException {
		EventEntity entity = eventList.get(0);
		eventService.deleteEvent(shelter.getId(), entity.getId());
		EventEntity deleted = entityManager.find(EventEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteEventWithInvalidShelterId() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity entity = eventList.get(0);
			eventService.deleteEvent(0L, entity.getId());
		});
	}

	@Test
	void testDeleteEventWithNonExistentShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			EventEntity entity = eventList.get(0);
			eventService.deleteEvent(1000L, entity.getId());
		});
	}

	@Test
	void testDeleteEventInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			eventService.deleteEvent(shelter.getId(), 0L);
		});
	}

	@Test
	void testDeleteInvalidEvent() {
		assertThrows(EntityNotFoundException.class, () -> {
			eventService.deleteEvent(shelter.getId(), 1000L);
		});
	}

	@Test
	void testDeleteEventNotBelongingToShelter() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity entity = eventList.get(0);
			eventService.deleteEvent(otherShelter.getId(), entity.getId());
		});
	}

	@Test
	void testDeletePastEvent() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity entity = factory.manufacturePojo(EventEntity.class);
			entity.setShelter(shelter);
			entity.setDate(pastDate(2));
			entityManager.persist(entity);
			eventService.deleteEvent(shelter.getId(), entity.getId());
		});
	}

	// ------------------------------------------------------------------
	// Additional coverage tests
	// ------------------------------------------------------------------

	private EventEntity persistEvent(ShelterEntity eventShelter, String name, String location, Date date) {
		EventEntity event = factory.manufacturePojo(EventEntity.class);
		event.setShelter(eventShelter);
		event.setName(name);
		event.setLocation(location);
		event.setDate(date);
		entityManager.persist(event);
		return event;
	}

	// getEvent(shelterId, eventId)
	@Test
	void testGetEventOfShelter() throws EntityNotFoundException, IllegalOperationException {
		EventEntity entity = eventList.get(0);
		EventEntity result = eventService.getEvent(shelter.getId(), entity.getId());
		assertNotNull(result);
		assertEquals(entity.getId(), result.getId());
	}

	@Test
	void testGetEventOfShelterWithInvalidShelterId() {
		Long eventId = eventList.get(0).getId();
		assertThrows(IllegalOperationException.class, () -> eventService.getEvent(0L, eventId));
	}

	@Test
	void testGetEventOfShelterWithNonExistentShelter() {
		Long eventId = eventList.get(0).getId();
		assertThrows(EntityNotFoundException.class, () -> eventService.getEvent(1000L, eventId));
	}

	@Test
	void testGetEventOfShelterWithInvalidEventId() {
		Long shelterId = shelter.getId();
		assertThrows(IllegalOperationException.class, () -> eventService.getEvent(shelterId, 0L));
	}

	@Test
	void testGetNonExistentEventOfShelter() {
		Long shelterId = shelter.getId();
		assertThrows(EntityNotFoundException.class, () -> eventService.getEvent(shelterId, 1000L));
	}

	@Test
	void testGetEventOfShelterNotBelongingToShelter() {
		Long eventId = eventList.get(0).getId();
		Long otherShelterId = otherShelter.getId();
		assertThrows(IllegalOperationException.class, () -> eventService.getEvent(otherShelterId, eventId));
	}

	@Test
	void testGetEventOfShelterWhenEventHasNoShelter() {
		EventEntity orphan = persistEvent(null, "Orphan event", "Somewhere", futureDate(5));
		Long shelterId = shelter.getId();
		Long orphanId = orphan.getId();
		assertThrows(IllegalOperationException.class, () -> eventService.getEvent(shelterId, orphanId));
	}

	// getEvents
	@Test
	void testGetEventsIgnoresEventsWithoutShelter() throws EntityNotFoundException, IllegalOperationException {
		persistEvent(null, "Orphan event", "Somewhere", futureDate(5));
		List<EventEntity> list = eventService.getEvents(shelter.getId());
		assertEquals(eventList.size(), list.size());
	}

	// searchEvents
	@Test
	void testSearchEventsByDateIgnoresEventsWithoutDate() throws IllegalOperationException {
		persistEvent(shelter, "Event without date", "Somewhere", null);
		EventEntity entity = eventList.get(0);
		List<EventEntity> list = eventService.searchEvents(null, null, entity.getDate());
		assertEquals(1, list.size());
		assertEquals(entity.getId(), list.get(0).getId());
	}

	// createEvent / updateEvent: mandatory attributes that are blank
	@Test
	void testCreateEventWithBlankDescription() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			newEntity.setDescription("   ");
			eventService.createEvent(shelter.getId(), newEntity);
		});
	}

	@Test
	void testCreateEventWithBlankLocation() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			newEntity.setLocation("   ");
			eventService.createEvent(shelter.getId(), newEntity);
		});
	}

	@Test
	void testUpdateEventWithBlankDescription() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity entity = eventList.get(0);
			EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setDate(futureDate(20));
			pojoEntity.setDescription(" ");
			eventService.updateEvent(shelter.getId(), entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateEventWithBlankLocation() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity entity = eventList.get(0);
			EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setDate(futureDate(20));
			pojoEntity.setLocation(" ");
			eventService.updateEvent(shelter.getId(), entity.getId(), pojoEntity);
		});
	}

	// deleteEvent
	@Test
	void testDeleteEventWithoutDate() throws EntityNotFoundException, IllegalOperationException {
		EventEntity noDate = persistEvent(shelter, "Event without date", "Somewhere", null);
		eventService.deleteEvent(shelter.getId(), noDate.getId());
		assertNull(entityManager.find(EventEntity.class, noDate.getId()));
	}

	@Test
	void testDeleteEventWhenEventHasNoShelter() {
		EventEntity orphan = persistEvent(null, "Orphan event", "Somewhere", futureDate(5));
		Long shelterId = shelter.getId();
		Long orphanId = orphan.getId();
		assertThrows(IllegalOperationException.class, () -> eventService.deleteEvent(shelterId, orphanId));
	}

	// duplicated event detection: events that look similar but are NOT duplicates
	@Test
	void testCreateEventSameNameDifferentLocationIsNotDuplicate()
			throws EntityNotFoundException, IllegalOperationException {
		EventEntity existing = eventList.get(0);
		EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
		newEntity.setName(existing.getName());
		newEntity.setLocation("another-location-" + existing.getLocation());
		newEntity.setDate(existing.getDate());

		assertNotNull(eventService.createEvent(shelter.getId(), newEntity));
	}

	@Test
	void testCreateEventSameNameAndLocationDifferentDateIsNotDuplicate()
			throws EntityNotFoundException, IllegalOperationException {
		EventEntity existing = eventList.get(0);
		EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
		newEntity.setName(existing.getName());
		newEntity.setLocation(existing.getLocation());
		newEntity.setDate(futureDate(60));

		assertNotNull(eventService.createEvent(shelter.getId(), newEntity));
	}

	@Test
	void testCreateEventSameDataInAnotherShelterIsNotDuplicate()
			throws EntityNotFoundException, IllegalOperationException {
		Date date = futureDate(30);
		persistEvent(otherShelter, "Adoption fair", "Main square", date);

		EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
		newEntity.setName("Adoption fair");
		newEntity.setLocation("Main square");
		newEntity.setDate(date);

		assertNotNull(eventService.createEvent(shelter.getId(), newEntity));
	}

	@Test
	void testCreateEventIgnoresExistingEventsWithMissingData()
			throws EntityNotFoundException, IllegalOperationException {
		Date date = futureDate(40);
		persistEvent(null, "Adoption fair", "Main square", date); // no shelter
		persistEvent(shelter, null, "Main square", date); // no name
		persistEvent(shelter, "Adoption fair", null, date); // same name, no location
		persistEvent(shelter, "Adoption fair", "Main square", null); // same name and location, no date

		EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
		newEntity.setName("Adoption fair");
		newEntity.setLocation("Main square");
		newEntity.setDate(date);

		assertNotNull(eventService.createEvent(shelter.getId(), newEntity));
	}
}