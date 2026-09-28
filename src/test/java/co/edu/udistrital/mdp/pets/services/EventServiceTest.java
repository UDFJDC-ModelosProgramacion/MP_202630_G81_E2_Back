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

	// getEvent
	@Test
	void testGetEvent() throws EntityNotFoundException, IllegalOperationException {
		EventEntity entity = eventList.get(0);
		EventEntity result = eventService.getEvent(shelter.getId(), entity.getId());
		assertNotNull(result);
		assertEquals(entity.getId(), result.getId());
	}

	@Test
	void testGetEventWithInvalidShelterId() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity entity = eventList.get(0);
			eventService.getEvent(0L, entity.getId());
		});
	}

	@Test
	void testGetEventWithNonExistentShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			EventEntity entity = eventList.get(0);
			eventService.getEvent(1000L, entity.getId());
		});
	}

	@Test
	void testGetEventWithInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			eventService.getEvent(shelter.getId(), 0L);
		});
	}

	@Test
	void testGetNonExistentEvent() {
		assertThrows(EntityNotFoundException.class, () -> {
			eventService.getEvent(shelter.getId(), 1000L);
		});
	}

	@Test
	void testGetEventNotBelongingToShelter() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity entity = eventList.get(0);
			eventService.getEvent(otherShelter.getId(), entity.getId());
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
}