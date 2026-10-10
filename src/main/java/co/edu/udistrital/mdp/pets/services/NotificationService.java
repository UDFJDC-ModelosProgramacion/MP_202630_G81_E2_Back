package co.edu.udistrital.mdp.pets.services;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.NotificationEntity;
import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.NotificationRepository;
import co.edu.udistrital.mdp.pets.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;






@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

	public static final Set<String> VALID_CHANNELS = Set.of("EMAIL", "SMS", "PUSH");

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;

	private static final String NOTIFICATION_ID_NOT_VALID = "Notification id is not valid";
	private static final String NOTIFICATION_NOT_FOUND = "Notification not found";
	private static final String USER_NOT_FOUND = "User not found";
	private static final String NOTIFICATION_NOT_OWNED = "The notification does not belong to the given user";

	@Transactional
	public NotificationEntity createNotification(Long userId, NotificationEntity notification)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación de la notificación para el usuario con id = {}", userId);

		UserEntity user = resolveUser(userId);

		if (notification.getMessage() == null || notification.getMessage().isBlank())
			throw new IllegalOperationException("Notification message cannot be null or empty");

		if (notification.getContent() == null || notification.getContent().isBlank())
			throw new IllegalOperationException("Notification content cannot be null or empty");

		if (notification.getDate() == null)
			throw new IllegalOperationException("Notification date cannot be null");

		if (notification.getTime() == null)
			throw new IllegalOperationException("Notification time cannot be null");

		if (notification.getChannel() == null || notification.getChannel().isBlank())
			throw new IllegalOperationException("Notification channel cannot be null or empty");

		if (!VALID_CHANNELS.contains(notification.getChannel().toUpperCase()))
			throw new IllegalOperationException("Notification channel is not valid");

		
		notification.setUser(user);
		if (notification.getSent() == null)
			notification.setSent(false);

		log.info("Termina proceso de creación de la notificación");
		return notificationRepository.save(notification);
	}

	
	
	
	
	@Transactional
	public List<NotificationEntity> getNotifications(Long userId, Date startDate, Date endDate)
			throws EntityNotFoundException {
		log.info("Inicia proceso de consultar notificaciones del usuario con id = {}", userId);
		resolveUser(userId);

		List<NotificationEntity> notifications = notificationRepository.findAll().stream()
				.filter(n -> n.getUser() != null && userId.equals(n.getUser().getId()))
				.filter(n -> startDate == null || (n.getDate() != null && !n.getDate().before(startDate)))
				.filter(n -> endDate == null || (n.getDate() != null && !n.getDate().after(endDate)))
				.toList();
		if (notifications.isEmpty())
			log.info("No hay notificaciones registradas que cumplan los filtros");
		return notifications;
	}

	
	
	
	@Transactional
	public NotificationEntity getNotification(Long userId, Long notificationId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar la notificación con id = {}", notificationId);
		resolveUser(userId);
		NotificationEntity notification = resolveNotification(notificationId);
		validateOwnership(notification, userId);

		log.info("Termina proceso de consultar la notificación con id = {}", notificationId);
		return notification;
	}

	
	
	
	@Transactional
	public NotificationEntity updateNotification(Long userId, Long notificationId, NotificationEntity notification)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la notificación con id = {}", notificationId);
		resolveUser(userId);
		NotificationEntity current = resolveNotification(notificationId);
		validateOwnership(current, userId);

		if (notification.getMessage() == null || notification.getMessage().isBlank())
			throw new IllegalOperationException("Notification message cannot be null or empty");

		if (notification.getContent() == null || notification.getContent().isBlank())
			throw new IllegalOperationException("Notification content cannot be null or empty");

		if (notification.getDate() == null)
			throw new IllegalOperationException("Notification date cannot be null");

		if (notification.getTime() == null)
			throw new IllegalOperationException("Notification time cannot be null");

		// Si el body no trae canal se conserva el actual; si lo trae, debe ser valido.
		String newChannel = notification.getChannel() == null || notification.getChannel().isBlank()
				? current.getChannel()
				: notification.getChannel();
		if (newChannel == null || !VALID_CHANNELS.contains(newChannel.toUpperCase()))
			throw new IllegalOperationException("Notification channel is not valid");

		if (Boolean.TRUE.equals(current.getSent()) && !newChannel.equalsIgnoreCase(current.getChannel()))
			throw new IllegalOperationException(
					"The channel cannot be modified once the notification has been sent");

		
		current.setMessage(notification.getMessage());
		current.setContent(notification.getContent());
		current.setDate(notification.getDate());
		current.setTime(notification.getTime());
		current.setChannel(newChannel);
		// Una notificacion ya enviada no puede volver a "no enviada".
		if (notification.getSent() != null && !Boolean.TRUE.equals(current.getSent()))
			current.setSent(notification.getSent());

		log.info("Termina proceso de actualizar la notificación con id = {}", notificationId);
		return notificationRepository.save(current);
	}

	
	
	
	@Transactional
	public void deleteNotification(Long userId, Long notificationId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar la notificación con id = {}", notificationId);
		resolveUser(userId);
		NotificationEntity notification = resolveNotification(notificationId);
		validateOwnership(notification, userId);

		if (Boolean.TRUE.equals(notification.getSent()))
			throw new IllegalOperationException(
					"A notification that has already been sent cannot be deleted; mark it as archived instead");

		notificationRepository.deleteById(notificationId);
		log.info("Termina proceso de borrar la notificación con id = {}", notificationId);
	}

	private UserEntity resolveUser(Long userId) throws EntityNotFoundException {
		if (userId == null)
			throw new EntityNotFoundException(USER_NOT_FOUND);
		Optional<UserEntity> user = userRepository.findById(userId);
		if (user.isEmpty())
			throw new EntityNotFoundException(USER_NOT_FOUND);
		return user.get();
	}

	private NotificationEntity resolveNotification(Long notificationId)
			throws EntityNotFoundException, IllegalOperationException {
		if (notificationId == null || notificationId <= 0)
			throw new IllegalOperationException(NOTIFICATION_ID_NOT_VALID);
		Optional<NotificationEntity> notification = notificationRepository.findById(notificationId);
		if (notification.isEmpty())
			throw new EntityNotFoundException(NOTIFICATION_NOT_FOUND);
		return notification.get();
	}

	private void validateOwnership(NotificationEntity notification, Long userId) throws IllegalOperationException {
		if (notification.getUser() == null || !notification.getUser().getId().equals(userId))
			throw new IllegalOperationException(NOTIFICATION_NOT_OWNED);
	}
}