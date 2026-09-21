package co.edu.udistrital.mdp.pets.services;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.MessageEntity;
import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.MessageRepository;
import co.edu.udistrital.mdp.pets.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    private static final String MESSAGE_ID_NOT_VALID = "Invalid identifiers are not accepted.";
    private static final String MESSAGE_NOT_FOUND = "If the message does not exist, an error message is displayed.";

    @Transactional
    public MessageEntity createMessage(MessageEntity message, Long currentUserId)
            throws IllegalOperationException, EntityNotFoundException {

        log.info("Message creation process started");

        if (message.getMessage() == null || message.getMessage().trim().isEmpty() ||
            message.getSendUser() == null || message.getReceivesUser() == null) {
            throw new IllegalOperationException(
                    "Required attributes cannot be null or empty.");
        }

        if (message.getMessage().length() > 1000) {
            throw new IllegalOperationException(
                    "The message content cannot exceed the maximum character limit.");
        }

        UserEntity sender = userRepository.findById(message.getSendUser().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Invalid sender."));

        UserEntity receiver = userRepository.findById(message.getReceivesUser().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Invalid recipient."));

        message.setSendUser(sender);
        message.setReceivesUser(receiver);
        message.setDate(new Date());
        message.setTime(new Date());
        message.setIsRead(false);
        message.setHiddenForSender(false);
        message.setHiddenForReceiver(false);

        MessageEntity savedMessage = messageRepository.save(message);

        log.info("Message creation process completed");
        return savedMessage;
    }

    @Transactional(readOnly = true)
    public MessageEntity readMessage(Long id, Long currentUserId)
            throws EntityNotFoundException, IllegalOperationException {

        log.info("Message query process started for id = {}", id);

        if (id == null || id <= 0) {
            throw new IllegalOperationException(MESSAGE_ID_NOT_VALID);
        }

        MessageEntity message = messageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(MESSAGE_NOT_FOUND));

        if (!message.getSendUser().getId().equals(currentUserId)
                && !message.getReceivesUser().getId().equals(currentUserId)) {
            throw new IllegalOperationException(
                    "Only the sender and recipient have permission to read the message.");
        }

        if (isHiddenFor(message, currentUserId)) {
            throw new EntityNotFoundException(MESSAGE_NOT_FOUND);
        }

        log.info("Message query process completed for id = {}", id);
        return message;
    }

    @Transactional(readOnly = true)
    public List<MessageEntity> readAllMessages(Long currentUserId) {

        log.info(
                "Process of querying all messages for user with id = {} started",
                currentUserId);

        List<MessageEntity> messages = messageRepository
                .findBySendUserIdOrReceivesUserId(currentUserId, currentUserId).stream()
                .filter(m -> !isHiddenFor(m, currentUserId))
                .toList();

        log.info(
                "Process of querying all messages for user with id = {} completed",
                currentUserId);

        return messages;
    }

    private boolean isHiddenFor(MessageEntity message, Long userId) {
        if (message.getSendUser().getId().equals(userId)) {
            return Boolean.TRUE.equals(message.getHiddenForSender());
        }
        return Boolean.TRUE.equals(message.getHiddenForReceiver());
    }

    @Transactional
    public MessageEntity updateMessage(
            Long id,
            MessageEntity messageUpdate,
            Long currentUserId)
            throws EntityNotFoundException, IllegalOperationException {

        log.info("Message update process started for id = {}", id);

        if (id == null || id <= 0) {
            throw new IllegalOperationException(MESSAGE_ID_NOT_VALID);
        }

        MessageEntity existingMessage = messageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Message not found."));

        if (!existingMessage.getReceivesUser().getId().equals(currentUserId)) {
            throw new IllegalOperationException(
                    "Only the recipient can mark the message as read.");
        }

        if (messageUpdate.getIsRead() != null) {
            existingMessage.setIsRead(messageUpdate.getIsRead());
        }

        MessageEntity savedMessage = messageRepository.save(existingMessage);

        log.info("Message update process completed for id = {}", id);
        return savedMessage;
    }

    /**
     * Realiza un borrado lógico del mensaje para el usuario solicitante: el
     * mensaje deja de ser visible para él, pero sigue existiendo para la otra
     * parte de la conversación. Cuando ambos lados lo han ocultado, el
     * registro se elimina físicamente.
     */
    @Transactional
    public void deleteMessage(Long id, Long currentUserId)
            throws EntityNotFoundException, IllegalOperationException {

        log.info("Message deletion process started for id = {}", id);

        if (id == null || id <= 0) {
            throw new IllegalOperationException(MESSAGE_ID_NOT_VALID);
        }

        MessageEntity existingMessage = messageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(MESSAGE_NOT_FOUND));

        boolean isSender = existingMessage.getSendUser().getId().equals(currentUserId);
        boolean isReceiver = existingMessage.getReceivesUser().getId().equals(currentUserId);

        if (!isSender && !isReceiver) {
            throw new IllegalOperationException(
                    "You do not have permission to delete this message.");
        }

        if (isSender) {
            existingMessage.setHiddenForSender(true);
        }
        if (isReceiver) {
            existingMessage.setHiddenForReceiver(true);
        }

        if (Boolean.TRUE.equals(existingMessage.getHiddenForSender())
                && Boolean.TRUE.equals(existingMessage.getHiddenForReceiver())) {
            messageRepository.deleteById(id);
        } else {
            messageRepository.save(existingMessage);
        }

        log.info("Message deletion process completed for id = {}", id);
    }
}