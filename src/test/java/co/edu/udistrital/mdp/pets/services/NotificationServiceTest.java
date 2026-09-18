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
import co.edu.udistrital.mdp.pets.services.NotificationService;

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
	void testCreateNotification() throws IllegalOperationException {
		NotificationEntity newEntity = factory.manufacturePojo(NotificationEntity.class);
		newEntity.setUser(userList.get(1));
		newEntity.setChannel("SMS");
		newEntity.setSent(false);

		NotificationEntity result = notificationService.createNotification(newEntity);

		assertNotNull(result);
		NotificationEntity entity = entityManager.find(NotificationEntity.class, result.getId());
		assertEquals(newEntity.getMessage(), entity.getMessage());
		assertEquals(newEntity.getContent(), entity.getContent());
		assertEquals(newEntity.getChannel(), entity.getChannel());
		assertEquals(newEntity.getUser().getId(), entity.getUser().getId());
	}

	@Test
	void testCreateNotificationWithNullMessage() {
		assertThrows(IllegalOperationException.class, () -> {
			NotificationEntity newEntity = factory.manufacturePojo(NotificationEntity.class);
			newEntity.setUser(userList.get(1));
			newEntity.setChannel("SMS");
			newEntity.setMessage(null);
			notificationService.createNotification(newEntity);
		});
	}

	@Test
	void testCreateNotificationWithEmptyContent() {
		assertThrows(IllegalOperationException.class, () -> {
			NotificationEntity newEntity = factory.manufacturePojo(NotificationEntity.class);
			newEntity.setUser(userList.get(1));
			newEntity.setChannel("SMS");
			newEntity.setContent("");
			notificationService.createNotification(newEntity);
		});
	}

	@Test
	void testCreateNotificationWithInvalidChannel() {
		assertThrows(IllegalOperationException.class, () -> {
			NotificationEntity newEntity = factory.manufacturePojo(NotificationEntity.class);
			newEntity.setUser(userList.get(1));
			newEntity.setChannel("CARRIER_PIGEON");
			notificationService.createNotification(newEntity);
		});
	}

	@Test
	void testCreateNotificationWithNullUser() {
		assertThrows(IllegalOperationException.class, () -> {
			NotificationEntity newEntity = factory.manufacturePojo(NotificationEntity.class);
			newEntity.setUser(null);
			newEntity.setChannel("EMAIL");
			notificationService.createNotification(newEntity);
		});
	}

	@Test
	void testCreateNotificationWithNonExistentUser() {
		assertThrows(IllegalOperationException.class, () -> {
			NotificationEntity newEntity = factory.manufacturePojo(NotificationEntity.class);
			UserEntity nonExistentUser = new UserEntity();
			nonExistentUser.setId(0L);
			newEntity.setUser(nonExistentUser);
			newEntity.setChannel("EMAIL");
			notificationService.createNotification(newEntity);
		});
	}

	@Test
	void testGetNotifications() {
		List<NotificationEntity> list = notificationService.getNotifications();
		assertEquals(notificationList.size(), list.size());
	}

	@Test
	void testGetNotificationsEmpty() {
		entityManager.getEntityManager().createQuery("delete from NotificationEntity").executeUpdate();
		List<NotificationEntity> list = notificationService.getNotifications();
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetNotificationsFilteredByUser() {
		List<NotificationEntity> list = notificationService.getNotifications(userList.get(0).getId(), null, null);
		assertEquals(notificationList.size(), list.size());
		List<NotificationEntity> emptyList = notificationService.getNotifications(userList.get(1).getId(), null, null);
		assertTrue(emptyList.isEmpty());
	}

	@Test
	void testGetNotificationsFilteredByDateRange() {
		Date start = notificationList.get(0).getDate();
		List<NotificationEntity> list = notificationService.getNotifications(null, start, start);
		assertNotNull(list);
	}

	@Test
	void testGetNotification() throws EntityNotFoundException, IllegalOperationException {
		NotificationEntity entity = notificationList.get(0);
		NotificationEntity resultEntity = notificationService.getNotification(entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
		assertEquals(entity.getMessage(), resultEntity.getMessage());
	}

	@Test
	void testGetInvalidNotificationId() {
		assertThrows(IllegalOperationException.class, () -> {
			notificationService.getNotification(0L);
		});
	}

	@Test
	void testGetNonExistentNotification() {
		assertThrows(EntityNotFoundException.class, () -> {
			notificationService.getNotification(1000L);
		});
	}

	@Test
	void testUpdateNotification() throws EntityNotFoundException, IllegalOperationException {
		NotificationEntity entity = notificationList.get(0);
		NotificationEntity pojoEntity = factory.manufacturePojo(NotificationEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setUser(userList.get(0));
		pojoEntity.setChannel("EMAIL");

		notificationService.updateNotification(entity.getId(), pojoEntity);

		NotificationEntity resp = entityManager.find(NotificationEntity.class, entity.getId());
		assertEquals(pojoEntity.getMessage(), resp.getMessage());
		assertEquals(pojoEntity.getContent(), resp.getContent());
	}

	@Test
	void testUpdateNotificationInvalidId() {
		assertThrows(EntityNotFoundException.class, () -> {
			NotificationEntity pojoEntity = factory.manufacturePojo(NotificationEntity.class);
			pojoEntity.setId(1000L);
			notificationService.updateNotification(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateNotificationWithNullMessage() {
		assertThrows(IllegalOperationException.class, () -> {
			NotificationEntity entity = notificationList.get(0);
			NotificationEntity pojoEntity = factory.manufacturePojo(NotificationEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setMessage(null);
			notificationService.updateNotification(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateNotificationChangeUserAfterSent() {
		assertThrows(IllegalOperationException.class, () -> {
			NotificationEntity entity = notificationList.get(0);
			entity.setSent(true);
			entityManager.persist(entity);

			NotificationEntity pojoEntity = factory.manufacturePojo(NotificationEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setUser(userList.get(1));
			pojoEntity.setChannel(entity.getChannel());
			notificationService.updateNotification(entity.getId(), pojoEntity);
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
			pojoEntity.setUser(entity.getUser());
			pojoEntity.setChannel("SMS");
			notificationService.updateNotification(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testDeleteNotification() throws EntityNotFoundException, IllegalOperationException {
		NotificationEntity entity = notificationList.get(0);
		notificationService.deleteNotification(entity.getId());
		NotificationEntity deleted = entityManager.find(NotificationEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteInvalidNotification() {
		assertThrows(EntityNotFoundException.class, () -> {
			notificationService.deleteNotification(1000L);
		});
	}

	@Test
	void testDeleteSentNotification() {
		assertThrows(IllegalOperationException.class, () -> {
			NotificationEntity entity = notificationList.get(0);
			entity.setSent(true);
			entityManager.persist(entity);
			notificationService.deleteNotification(entity.getId());
		});
	}
}
