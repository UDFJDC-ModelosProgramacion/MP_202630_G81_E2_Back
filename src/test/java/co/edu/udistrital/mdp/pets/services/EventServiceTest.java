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

	// Test for Create Event

	@Test
	void testCreateEvent() throws EntityNotFoundException, IllegalOperationException {
		EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
		newEntity.setDate(futureDate(5));
		newEntity.setShelter(shelter);

		EventEntity result = eventService.createEvent(newEntity);

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
		newEntity.setShelter(shelter);

		EventEntity result = eventService.createEvent(newEntity);

		assertNotNull(result);
	}

	@Test
	void testCreateEventWithNullName() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			newEntity.setShelter(shelter);
			newEntity.setName(null);
			eventService.createEvent(newEntity);
		});
	}

	@Test
	void testCreateEventWithBlankName() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			newEntity.setShelter(shelter);
			newEntity.setName("   ");
			eventService.createEvent(newEntity);
		});
	}

	@Test
	void testCreateEventWithNullDate() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setShelter(shelter);
			newEntity.setDate(null);
			eventService.createEvent(newEntity);
		});
	}

	@Test
	void testCreateEventWithNullTime() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			newEntity.setShelter(shelter);
			newEntity.setTime(null);
			eventService.createEvent(newEntity);
		});
	}

	@Test
	void testCreateEventWithBlankTime() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			newEntity.setShelter(shelter);
			newEntity.setTime("  ");
			eventService.createEvent(newEntity);
		});
	}

	@Test
	void testCreateEventWithNullDescription() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			newEntity.setShelter(shelter);
			newEntity.setDescription(null);
			eventService.createEvent(newEntity);
		});
	}

	@Test
	void testCreateEventWithNullLocation() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			newEntity.setShelter(shelter);
			newEntity.setLocation(null);
			eventService.createEvent(newEntity);
		});
	}

	@Test
	void testCreateEventWithNullShelter() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			newEntity.setShelter(null);
			eventService.createEvent(newEntity);
		});
	}

	@Test
	void testCreateEventWithShelterWithoutId() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			ShelterEntity shelterNoId = new ShelterEntity();
			// id intentionally left null
			newEntity.setShelter(shelterNoId);
			eventService.createEvent(newEntity);
		});
	}

	@Test
	void testCreateEventWithNonExistentShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(futureDate(5));
			ShelterEntity fakeShelter = new ShelterEntity();
			fakeShelter.setId(0L);
			newEntity.setShelter(fakeShelter);
			eventService.createEvent(newEntity);
		});
	}

	@Test
	void testCreateEventWithPastDate() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity newEntity = factory.manufacturePojo(EventEntity.class);
			newEntity.setDate(pastDate(5));
			newEntity.setShelter(shelter);
			eventService.createEvent(newEntity);
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
			newEntity.setShelter(shelter);
			eventService.createEvent(newEntity);
		});
	}

	// Tests for getEvents

	@Test
	void testGetEvents() {
		List<EventEntity> list = eventService.getEvents();
		assertEquals(eventList.size(), list.size());
	}

	@Test
	void testGetEventsEmpty() {
		entityManager.getEntityManager().createQuery("delete from EventEntity").executeUpdate();
		List<EventEntity> list = eventService.getEvents();
		assertTrue(list.isEmpty());
	}

	// Test for getEvet

	@Test
	void testGetEvent() throws EntityNotFoundException, IllegalOperationException {
		EventEntity entity = eventList.get(0);
		EventEntity result = eventService.getEvent(entity.getId());
		assertNotNull(result);
		assertEquals(entity.getId(), result.getId());
	}

	@Test
	void testGetEventInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			eventService.getEvent(0L);
		});
	}

	@Test
	void testGetEventNegativeId() {
		assertThrows(IllegalOperationException.class, () -> {
			eventService.getEvent(-1L);
		});
	}

	@Test
	void testGetEventCombinedFiltersMatch() throws EntityNotFoundException, IllegalOperationException {
		EventEntity entity = eventList.get(0);
		EventEntity result = eventService.getEvent(entity.getId(), entity.getName(), entity.getDate());
		assertNotNull(result);
		assertEquals(entity.getId(), result.getId());
	}

	@Test
	void testGetEventCombinedFiltersNoMatch() {
		assertThrows(EntityNotFoundException.class, () -> {
			EventEntity entity = eventList.get(0);
			eventService.getEvent(entity.getId(), "unmatched-name-xyz", entity.getDate());
		});
	}

	@Test
	void testGetEventCombinedFiltersDateMismatch() {
		assertThrows(EntityNotFoundException.class, () -> {
			EventEntity entity = eventList.get(0);
			eventService.getEvent(entity.getId(), entity.getName(), futureDate(500));
		});
	}

	@Test
	void testGetEventCombinedFiltersInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity entity = eventList.get(0);
			eventService.getEvent(0L, entity.getName(), entity.getDate());
		});
	}

	@Test
	void testGetNonExistentEvent() {
		assertThrows(EntityNotFoundException.class, () -> {
			eventService.getEvent(1000L);
		});
	}

	// Test for updateEvent

	@Test
	void testUpdateEvent() throws EntityNotFoundException, IllegalOperationException {
		EventEntity entity = eventList.get(0);
		EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setDate(futureDate(20));

		eventService.updateEvent(entity.getId(), pojoEntity);

		EventEntity resp = entityManager.find(EventEntity.class, entity.getId());
		assertEquals(pojoEntity.getName(), resp.getName());
		assertEquals(pojoEntity.getLocation(), resp.getLocation());
		assertEquals(entity.getShelter().getId(), resp.getShelter().getId());
	}

	@Test
	void testUpdateEventCannotChangeShelter() throws EntityNotFoundException, IllegalOperationException {
		EventEntity entity = eventList.get(0);

		ShelterEntity anotherShelter = factory.manufacturePojo(ShelterEntity.class);
		entityManager.persist(anotherShelter);

		EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setDate(futureDate(20));
		pojoEntity.setShelter(anotherShelter);

		eventService.updateEvent(entity.getId(), pojoEntity);

		EventEntity resp = entityManager.find(EventEntity.class, entity.getId());
		assertEquals(shelter.getId(), resp.getShelter().getId());
	}

	@Test
	void testUpdateEventInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
			pojoEntity.setDate(futureDate(5));
			eventService.updateEvent(0L, pojoEntity);
		});
	}

	@Test
	void testUpdateNonExistentEvent() {
		assertThrows(EntityNotFoundException.class, () -> {
			EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
			pojoEntity.setDate(futureDate(5));
			eventService.updateEvent(1000L, pojoEntity);
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
			eventService.updateEvent(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateEventWithPastDate() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity entity = eventList.get(0);
			EventEntity pojoEntity = factory.manufacturePojo(EventEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setDate(pastDate(5));
			eventService.updateEvent(entity.getId(), pojoEntity);
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

			eventService.updateEvent(target.getId(), pojoEntity);
		});
	}

	// Test dor feleteEvent

	@Test
	void testDeleteEvent() throws EntityNotFoundException, IllegalOperationException {
		EventEntity entity = eventList.get(0);
		eventService.deleteEvent(entity.getId());
		EventEntity deleted = entityManager.find(EventEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteEventInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			eventService.deleteEvent(0L);
		});
	}

	@Test
	void testDeleteInvalidEvent() {
		assertThrows(EntityNotFoundException.class, () -> {
			eventService.deleteEvent(1000L);
		});
	}

	@Test
	void testDeletePastEvent() {
		assertThrows(IllegalOperationException.class, () -> {
			EventEntity entity = factory.manufacturePojo(EventEntity.class);
			entity.setShelter(shelter);
			entity.setDate(pastDate(2));
			entityManager.persist(entity);
			eventService.deleteEvent(entity.getId());
		});
	}
}