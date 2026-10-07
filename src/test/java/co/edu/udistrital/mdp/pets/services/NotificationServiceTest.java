package co.edu.udistrital.mdp.pets.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.NotificationEntity;
import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(NotificationService.class)
class NotificationServiceTest {

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<NotificationEntity> notificationList = new ArrayList<>();
	private List<UserEntity> userList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from NotificationEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from UserEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			UserEntity userEntity = factory.manufacturePojo(UserEntity.class);
			entityManager.persist(userEntity);
			userList.add(userEntity);
		}
		// userList.get(0) es el dueño de las notificaciones de prueba;
		// userList.get(1) se usa como "otro usuario" para probar la propiedad.
		for (int i = 0; i < 3; i++) {
			NotificationEntity notificationEntity = factory.manufacturePojo(NotificationEntity.class);
			notificationEntity.setUser(userList.get(0));
			notificationEntity.setChannel("EMAIL");
			notificationEntity.setSent(false);
			entityManager.persist(notificationEntity);
			notificationList.add(notificationEntity);
		}
	}

	@Test
	void testCreateNotification() throws EntityNotFoundException, IllegalOperationException {
		NotificationEntity newEntity = factory.manufacturePojo(NotificationEntity.class);
		newEntity.setChannel("SMS");
		newEntity.setSent(false);

		NotificationEntity result = notificationService.createNotification(userList.get(1).getId(), newEntity);

		assertNotNull(result);
		NotificationEntity entity = entityManager.find(NotificationEntity.class, result.getId());
		assertEquals(newEntity.getMessage(), entity.getMessage());
		assertEquals(newEntity.getContent(), entity.getContent());
		assertEquals(newEntity.getChannel(), entity.getChannel());
		assertEquals(userList.get(1).getId(), entity.getUser().getId());
	}

	@Test
	void testCreateNotificationWithNullMessage() {
		assertThrows(IllegalOperationException.class, () -> {
			NotificationEntity newEntity = factory.manufacturePojo(NotificationEntity.class);
			newEntity.setChannel("SMS");
			newEntity.setMessage(null);
			notificationService.createNotification(userList.get(1).getId(), newEntity);
		});
	}

	@Test
	void testCreateNotificationWithEmptyContent() {
		assertThrows(IllegalOperationException.class, () -> {
			NotificationEntity newEntity = factory.manufacturePojo(NotificationEntity.class);
			newEntity.setChannel("SMS");
			newEntity.setContent("");
			notificationService.createNotification(userList.get(1).getId(), newEntity);
		});
	}

	@Test
	void testCreateNotificationWithInvalidChannel() {
		assertThrows(IllegalOperationException.class, () -> {
			NotificationEntity newEntity = factory.manufacturePojo(NotificationEntity.class);
			newEntity.setChannel("CARRIER_PIGEON");
			notificationService.createNotification(userList.get(1).getId(), newEntity);
		});
	}

	@Test
	void testCreateNotificationWithNullUserId() {
		assertThrows(EntityNotFoundException.class, () -> {
			NotificationEntity newEntity = factory.manufacturePojo(NotificationEntity.class);
			newEntity.setChannel("EMAIL");
			notificationService.createNotification(null, newEntity);
		});
	}

	@Test
	void testCreateNotificationWithNonExistentUser() {
		assertThrows(EntityNotFoundException.class, () -> {
			NotificationEntity newEntity = factory.manufacturePojo(NotificationEntity.class);
			newEntity.setChannel("EMAIL");
			notificationService.createNotification(0L, newEntity);
		});
	}

	@Test
	void testGetNotifications() throws EntityNotFoundException {
		List<NotificationEntity> list = notificationService.getNotifications(userList.get(0).getId(), null, null);
		assertEquals(notificationList.size(), list.size());
	}

	@Test
	void testGetNotificationsEmptyForOtherUser() throws EntityNotFoundException {
		List<NotificationEntity> list = notificationService.getNotifications(userList.get(1).getId(), null, null);
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetNotificationsNonExistentUser() {
		assertThrows(EntityNotFoundException.class, () -> {
			notificationService.getNotifications(0L, null, null);
		});
	}

	@Test
	void testGetNotificationsFilteredByDateRange() throws EntityNotFoundException {
		Date start = notificationList.get(0).getDate();
		List<NotificationEntity> list = notificationService.getNotifications(userList.get(0).getId(), start, start);
		assertNotNull(list);
	}

	@Test
	void testGetNotification() throws EntityNotFoundException, IllegalOperationException {
		NotificationEntity entity = notificationList.get(0);
		NotificationEntity resultEntity = notificationService.getNotification(userList.get(0).getId(), entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
		assertEquals(entity.getMessage(), resultEntity.getMessage());
	}

	@Test
	void testGetInvalidNotificationId() {
		assertThrows(IllegalOperationException.class, () -> {
			notificationService.getNotification(userList.get(0).getId(), 0L);
		});
	}

	@Test
	void testGetNonExistentNotification() {
		assertThrows(EntityNotFoundException.class, () -> {
			notificationService.getNotification(userList.get(0).getId(), 1000L);
		});
	}

	@Test
	void testGetNotificationNonExistentUser() {
		assertThrows(EntityNotFoundException.class, () -> {
			notificationService.getNotification(0L, notificationList.get(0).getId());
		});
	}

	@Test
	void testGetNotificationNotOwnedByUser() {
		assertThrows(IllegalOperationException.class, () -> {
			notificationService.getNotification(userList.get(1).getId(), notificationList.get(0).getId());
		});
	}

	@Test
	void testUpdateNotification() throws EntityNotFoundException, IllegalOperationException {
		NotificationEntity entity = notificationList.get(0);
		NotificationEntity pojoEntity = factory.manufacturePojo(NotificationEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setChannel("EMAIL");

		notificationService.updateNotification(userList.get(0).getId(), entity.getId(), pojoEntity);

		NotificationEntity resp = entityManager.find(NotificationEntity.class, entity.getId());
		assertEquals(pojoEntity.getMessage(), resp.getMessage());
		assertEquals(pojoEntity.getContent(), resp.getContent());
		assertEquals(userList.get(0).getId(), resp.getUser().getId());
	}

	@Test
	void testUpdateNotificationInvalidId() {
		assertThrows(EntityNotFoundException.class, () -> {
			NotificationEntity pojoEntity = factory.manufacturePojo(NotificationEntity.class);
			pojoEntity.setId(1000L);
			notificationService.updateNotification(userList.get(0).getId(), 1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateNotificationWithNullMessage() {
		assertThrows(IllegalOperationException.class, () -> {
			NotificationEntity entity = notificationList.get(0);
			NotificationEntity pojoEntity = factory.manufacturePojo(NotificationEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setMessage(null);
			notificationService.updateNotification(userList.get(0).getId(), entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateNotificationNotOwnedByUser() {
		assertThrows(IllegalOperationException.class, () -> {
			NotificationEntity entity = notificationList.get(0);
			NotificationEntity pojoEntity = factory.manufacturePojo(NotificationEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setChannel(entity.getChannel());
			notificationService.updateNotification(userList.get(1).getId(), entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateNotificationChangeChannelAfterSent() {
		assertThrows(IllegalOperationException.class, () -> {
			NotificationEntity entity = notificationList.get(0);
			entity.setSent(true);
			entityManager.persist(entity);

			NotificationEntity pojoEntity = factory.manufacturePojo(NotificationEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setChannel("SMS");
			notificationService.updateNotification(userList.get(0).getId(), entity.getId(), pojoEntity);
		});
	}

	@Test
	void testDeleteNotification() throws EntityNotFoundException, IllegalOperationException {
		NotificationEntity entity = notificationList.get(0);
		notificationService.deleteNotification(userList.get(0).getId(), entity.getId());
		NotificationEntity deleted = entityManager.find(NotificationEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteInvalidNotification() {
		assertThrows(EntityNotFoundException.class, () -> {
			notificationService.deleteNotification(userList.get(0).getId(), 1000L);
		});
	}

	@Test
	void testDeleteNotificationNonExistentUser() {
		assertThrows(EntityNotFoundException.class, () -> {
			notificationService.deleteNotification(0L, notificationList.get(0).getId());
		});
	}

	@Test
	void testDeleteNotificationNotOwnedByUser() {
		assertThrows(IllegalOperationException.class, () -> {
			notificationService.deleteNotification(userList.get(1).getId(), notificationList.get(0).getId());
		});
	}

	@Test
	void testDeleteSentNotification() {
		assertThrows(IllegalOperationException.class, () -> {
			NotificationEntity entity = notificationList.get(0);
			entity.setSent(true);
			entityManager.persist(entity);
			notificationService.deleteNotification(userList.get(0).getId(), entity.getId());
		});
	}
}