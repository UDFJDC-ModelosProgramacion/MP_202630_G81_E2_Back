package co.edu.udistrital.mdp.pets.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.MessageEntity;
import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.repositories.MessageRepository;
import co.edu.udistrital.mdp.pets.repositories.UserRepository;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;

import java.util.Date;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public MessageEntity createMessage(MessageEntity message, Long currentUserId) throws IllegalOperationException, EntityNotFoundException {
        if (message.getMessage() == null || message.getMessage().trim().isEmpty() ||
            message.getSendUser() == null || message.getReceivesUser() == null) {
            throw new IllegalOperationException("Ningún atributo obligatorio puede ser nulo o vacío.");
        }
        if (message.getMessage().length() > 1000) {
            throw new IllegalOperationException("El contenido del mensaje no puede exceder el límite máximo de caracteres.");
        }

        UserEntity sender = userRepository.findById(message.getSendUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("Remitente no válido."));
        UserEntity receiver = userRepository.findById(message.getReceivesUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("Destinatario no válido."));

        message.setSendUser(sender);
        message.setReceivesUser(receiver);
        message.setDate(new Date());
        message.setTime(new Date());
        message.setIsRead(false);

        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public MessageEntity readMessage(Long id, Long currentUserId) throws EntityNotFoundException, IllegalOperationException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("No se aceptan identificadores inválidos.");
        }
        MessageEntity message = messageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Si el mensaje no existe, se muestra un mensaje de error."));

        if (!message.getSendUser().getId().equals(currentUserId) && !message.getReceivesUser().getId().equals(currentUserId)) {
            throw new IllegalOperationException("Solo el remitente y el destinatario tienen permiso para leer el mensaje.");
        }

        return message;
    }

    @Transactional(readOnly = true)
    public List<MessageEntity> readAllMessages(Long currentUserId) {
        // Solo se retornan los mensajes donde el usuario que consulta es el remitente o el destinatario
        return messageRepository.findBySendUserIdOrReceivesUserId(currentUserId, currentUserId);
    }

    @Transactional
    public MessageEntity updateMessage(Long id, MessageEntity messageUpdate, Long currentUserId) throws EntityNotFoundException, IllegalOperationException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("No se aceptan identificadores inválidos.");
        }
        MessageEntity existingMessage = messageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mensaje no encontrado."));

        if (!existingMessage.getReceivesUser().getId().equals(currentUserId)) {
            throw new IllegalOperationException("Solo el destinatario puede marcar el mensaje como leído.");
        }

        // El contenido es inmutable
        if (messageUpdate.getIsRead() != null) {
            existingMessage.setIsRead(messageUpdate.getIsRead());
        }
        return messageRepository.save(existingMessage);
    }

    @Transactional
    public void deleteMessage(Long id, Long currentUserId) throws EntityNotFoundException, IllegalOperationException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("No se aceptan identificadores inválidos.");
        }
        MessageEntity existingMessage = messageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Si el identificador no existe, se muestra un mensaje indicándolo."));

        if (!existingMessage.getSendUser().getId().equals(currentUserId) && !existingMessage.getReceivesUser().getId().equals(currentUserId)) {
            throw new IllegalOperationException("No tiene permisos para eliminar este mensaje.");
        }

        // Logical delete implementation would go here (e.g. setting deletedBySender or deletedByReceiver flags)
        // For now, we simulate logical delete by leaving it in DB but maybe just clearing association for that user in a more advanced schema.
        throw new IllegalOperationException("La eliminación oculta el mensaje únicamente para el usuario (Eliminación Lógica no implementada en el esquema base).");
    }
}
