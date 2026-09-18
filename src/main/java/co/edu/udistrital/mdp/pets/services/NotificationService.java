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

	// Canales válidos para el envío de una notificación
	public static final Set<String> VALID_CHANNELS = Set.of("EMAIL", "SMS", "PUSH");

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;

	/**
	 * Crea una nueva notificación.
	 */
	@Transactional
	public NotificationEntity createNotification(NotificationEntity notification)
			throws IllegalOperationException {
		log.info("Inicia proceso de creación de la notificación");

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

		if (notification.getUser() == null || notification.getUser().getId() == null)
			throw new IllegalOperationException("Notification must be associated with a recipient user");

		Optional<UserEntity> user = userRepository.findById(notification.getUser().getId());
		if (user.isEmpty())
			throw new IllegalOperationException("Notification recipient user does not exist");

		notification.setUser(user.get());
		if (notification.getSent() == null)
			notification.setSent(false);

		log.info("Termina proceso de creación de la notificación");
		return notificationRepository.save(notification);
	}

	/**
	 * Obtiene todas las notificaciones registradas.
	 */
	@Transactional
	public List<NotificationEntity> getNotifications() {
		log.info("Inicia proceso de consultar todas las notificaciones");
		List<NotificationEntity> notifications = notificationRepository.findAll();
		if (notifications.isEmpty())
			log.info("No hay notificaciones registradas");
		return notifications;
	}

	/**
	 * Obtiene las notificaciones filtrando, de forma opcional, por usuario
	 * destinatario y por un rango de fechas.
	 */
	@Transactional
	public List<NotificationEntity> getNotifications(Long userId, Date startDate, Date endDate) {
		log.info("Inicia proceso de consultar notificaciones filtradas");
		List<NotificationEntity> notifications = notificationRepository.findAll().stream()
				.filter(n -> userId == null || (n.getUser() != null && userId.equals(n.getUser().getId())))
				.filter(n -> startDate == null || (n.getDate() != null && !n.getDate().before(startDate)))
				.filter(n -> endDate == null || (n.getDate() != null && !n.getDate().after(endDate)))
				.toList();
		if (notifications.isEmpty())
			log.info("No hay notificaciones registradas que cumplan los filtros");
		return notifications;
	}

	/**
	 * Obtiene una notificación a partir de su id.
	 */
	@Transactional
	public NotificationEntity getNotification(Long notificationId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar la notificación con id = {}", notificationId);
		if (notificationId == null || notificationId <= 0)
			throw new IllegalOperationException("Notification id is not valid");

		Optional<NotificationEntity> notification = notificationRepository.findById(notificationId);
		if (notification.isEmpty())
			throw new EntityNotFoundException("Notification not found");

		log.info("Termina proceso de consultar la notificación con id = {}", notificationId);
		return notification.get();
	}

	/**
	 * Actualiza una notificación existente.
	 */
	@Transactional
	public NotificationEntity updateNotification(Long notificationId, NotificationEntity notification)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la notificación con id = {}", notificationId);
		if (notificationId == null || notificationId <= 0)
			throw new IllegalOperationException("Notification id is not valid");

		Optional<NotificationEntity> existing = notificationRepository.findById(notificationId);
		if (existing.isEmpty())
			throw new EntityNotFoundException("Notification not found");

		if (notification.getMessage() == null || notification.getMessage().isBlank())
			throw new IllegalOperationException("Notification message cannot be null or empty");

		if (notification.getContent() == null || notification.getContent().isBlank())
			throw new IllegalOperationException("Notification content cannot be null or empty");

		NotificationEntity current = existing.get();
		if (Boolean.TRUE.equals(current.getSent())) {
			boolean userChanged = notification.getUser() == null
					|| current.getUser() == null
					|| !notification.getUser().getId().equals(current.getUser().getId());
			boolean channelChanged = notification.getChannel() == null
					|| !notification.getChannel().equalsIgnoreCase(current.getChannel());
			if (userChanged || channelChanged)
				throw new IllegalOperationException(
						"The recipient user and the channel cannot be modified once the notification has been sent");
		}

		notification.setId(notificationId);
		notification.setUser(current.getUser());
		notification.setChannel(current.getChannel());
		notification.setSent(current.getSent());

		log.info("Termina proceso de actualizar la notificación con id = {}", notificationId);
		return notificationRepository.save(notification);
	}

	/**
	 * Borra una notificación a partir de su id.
	 */
	@Transactional
	public void deleteNotification(Long notificationId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar la notificación con id = {}", notificationId);
		if (notificationId == null || notificationId <= 0)
			throw new IllegalOperationException("Notification id is not valid");

		Optional<NotificationEntity> notification = notificationRepository.findById(notificationId);
		if (notification.isEmpty())
			throw new EntityNotFoundException("Notification not found");

		if (Boolean.TRUE.equals(notification.get().getSent()))
			throw new IllegalOperationException(
					"A notification that has already been sent cannot be deleted; mark it as archived instead");

		notificationRepository.deleteById(notificationId);
		log.info("Termina proceso de borrar la notificación con id = {}", notificationId);
	}
}
