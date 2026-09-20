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
                .orElseThrow(() -> new EntityNotFoundException(
                        "If the message does not exist, an error message is displayed."));

        if (!message.getSendUser().getId().equals(currentUserId)
                && !message.getReceivesUser().getId().equals(currentUserId)) {
            throw new IllegalOperationException(
                    "Only the sender and recipient have permission to read the message.");
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
                .findBySendUserIdOrReceivesUserId(currentUserId, currentUserId);

        log.info(
                "Process of querying all messages for user with id = {} completed",
                currentUserId);

        return messages;
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
                .orElseThrow(() -> new EntityNotFoundException(
                        "Message not found."));

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

    @Transactional
    public void deleteMessage(Long id, Long currentUserId)
            throws EntityNotFoundException, IllegalOperationException {

        log.info("Message deletion process started for id = {}", id);

        if (id == null || id <= 0) {
            throw new IllegalOperationException(MESSAGE_ID_NOT_VALID);
        }

        MessageEntity existingMessage = messageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "If the identifier does not exist, a message indicating this is displayed."));

        if (!existingMessage.getSendUser().getId().equals(currentUserId)
                && !existingMessage.getReceivesUser().getId().equals(currentUserId)) {
            throw new IllegalOperationException(
                    "You do not have permission to delete this message.");
        }

        throw new IllegalOperationException(
                "Deletion only hides the message for the user "
                + "(logical deletion is not implemented in the base schema).");
    }
}