package co.edu.udistrital.mdp.pets.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import co.edu.udistrital.mdp.pets.entities.MessageEntity;
import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.MessageRepository;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Import(MessageService.class)
class MessageServiceTest {

    @Autowired
    private MessageService messageService;

    @Autowired 
    private MessageRepository messageRepository;

    @Autowired
    private TestEntityManager entityManager;

    private PodamFactory factory = new PodamFactoryImpl();

    private final List<UserEntity> userList = new ArrayList<>();
    private final List<MessageEntity> messageList = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clearData();
        insertData();
    }

    private void clearData() {
        entityManager.getEntityManager().createQuery("delete from MessageEntity").executeUpdate();
        entityManager.getEntityManager().createQuery("delete from UserEntity").executeUpdate();
    }

    private void insertData() {
        for (int i = 0; i < 3; i++) {
            UserEntity user = factory.manufacturePojo(UserEntity.class);
            entityManager.persist(user);
            userList.add(user);
        }

        for (int i = 0; i < 3; i++) {
            MessageEntity message = factory.manufacturePojo(MessageEntity.class);
            message.setSendUser(userList.get(0));
            message.setReceivesUser(userList.get(1));
            message.setIsRead(false);
            entityManager.persist(message);
            messageList.add(message);
        }
    }

    // ---------- createMessage ----------

    @Test
    void testCreateMessage() throws EntityNotFoundException, IllegalOperationException {
        MessageEntity newMessage = factory.manufacturePojo(MessageEntity.class);
        newMessage.setMessage("Hello, is the pet still available?");
        newMessage.setSendUser(userList.get(0));
        newMessage.setReceivesUser(userList.get(1));

        MessageEntity result = messageService.createMessage(newMessage, userList.get(0).getId());

        assertNotNull(result);
        assertEquals(userList.get(0).getId(), result.getSendUser().getId());
        assertEquals(userList.get(1).getId(), result.getReceivesUser().getId());
        assertFalse(result.getIsRead());
        assertNotNull(result.getDate());
        assertNotNull(result.getTime());
    }

    @Test
    void testCreateMessageWithEmptyContent() {
        MessageEntity newMessage = factory.manufacturePojo(MessageEntity.class);
        newMessage.setMessage("   ");
        newMessage.setSendUser(userList.get(0));
        newMessage.setReceivesUser(userList.get(1));

        assertThrows(IllegalOperationException.class, () -> {
            messageService.createMessage(newMessage, userList.get(0).getId());
        });
    }

    @Test
    void testCreateMessageWithNullSendUser() {
        MessageEntity newMessage = factory.manufacturePojo(MessageEntity.class);
        newMessage.setMessage("Valid content");
        newMessage.setSendUser(null);
        newMessage.setReceivesUser(userList.get(1));

        assertThrows(IllegalOperationException.class, () -> {
            messageService.createMessage(newMessage, userList.get(0).getId());
        });
    }

    @Test
    void testCreateMessageExceedingCharacterLimit() {
        MessageEntity newMessage = factory.manufacturePojo(MessageEntity.class);
        newMessage.setMessage("a".repeat(1001));
        newMessage.setSendUser(userList.get(0));
        newMessage.setReceivesUser(userList.get(1));

        assertThrows(IllegalOperationException.class, () -> {
            messageService.createMessage(newMessage, userList.get(0).getId());
        });
    }

    @Test
    void testCreateMessageWithInvalidSender() {
        MessageEntity newMessage = factory.manufacturePojo(MessageEntity.class);
        newMessage.setMessage("Valid content");

        UserEntity invalidUser = new UserEntity();
        invalidUser.setId(0L);
        newMessage.setSendUser(invalidUser);
        newMessage.setReceivesUser(userList.get(1));

        assertThrows(EntityNotFoundException.class, () -> {
            messageService.createMessage(newMessage, userList.get(0).getId());
        });
    }

    @Test
    void testCreateMessageWithInvalidReceiver() {
        MessageEntity newMessage = factory.manufacturePojo(MessageEntity.class);
        newMessage.setMessage("Valid content");
        newMessage.setSendUser(userList.get(0));

        UserEntity invalidUser = new UserEntity();
        invalidUser.setId(0L);
        newMessage.setReceivesUser(invalidUser);

        assertThrows(EntityNotFoundException.class, () -> {
            messageService.createMessage(newMessage, userList.get(0).getId());
        });
    }

    // ---------- readMessage ----------

    @Test
    void testReadMessage() throws EntityNotFoundException, IllegalOperationException {
        MessageEntity existingMessage = messageList.get(0);

        MessageEntity result = messageService.readMessage(existingMessage.getId(), userList.get(0).getId());

        assertNotNull(result);
        assertEquals(existingMessage.getId(), result.getId());
    }

    @Test
    void testReadMessageWithInvalidId() {
        assertThrows(IllegalOperationException.class, () -> {
            messageService.readMessage(0L, userList.get(0).getId());
        });
    }

    @Test
    void testReadMessageThatDoesNotExist() {
        assertThrows(EntityNotFoundException.class, () -> {
            messageService.readMessage(messageList.get(2).getId() + 10000L, userList.get(0).getId());
        });
    }

    @Test
    void testReadMessageWithoutPermission() {
        MessageEntity existingMessage = messageList.get(0);

        assertThrows(IllegalOperationException.class, () -> {
            messageService.readMessage(existingMessage.getId(), userList.get(2).getId());
        });
    }

    // ---------- readAllMessages ----------

    @Test
    void testReadAllMessages() {
        List<MessageEntity> result = messageService.readAllMessages(userList.get(0).getId());

        assertEquals(messageList.size(), result.size());
    }

    @Test
    void testReadAllMessagesForUserWithNoMessages() {
        List<MessageEntity> result = messageService.readAllMessages(userList.get(2).getId());

        assertTrue(result.isEmpty());
    }

    // ---------- updateMessage ----------

    @Test
    void testUpdateMessage() throws EntityNotFoundException, IllegalOperationException {
        MessageEntity existingMessage = messageList.get(0);

        MessageEntity messageUpdate = new MessageEntity();
        messageUpdate.setIsRead(true);

        MessageEntity result = messageService.updateMessage(
                existingMessage.getId(), messageUpdate, userList.get(1).getId());

        assertNotNull(result);
        assertTrue(result.getIsRead());
    }

    @Test
    void testUpdateMessageWithInvalidId() {
        MessageEntity messageUpdate = new MessageEntity();
        messageUpdate.setIsRead(true);

        assertThrows(IllegalOperationException.class, () -> {
            messageService.updateMessage(0L, messageUpdate, userList.get(1).getId());
        });
    }

    @Test
    void testUpdateMessageThatDoesNotExist() {
        MessageEntity messageUpdate = new MessageEntity();
        messageUpdate.setIsRead(true);

        assertThrows(EntityNotFoundException.class, () -> {
            messageService.updateMessage(messageList.get(2).getId() + 10000L, messageUpdate, userList.get(1).getId());
        });
    }

    @Test
    void testUpdateMessageByNonReceiver() {
        MessageEntity existingMessage = messageList.get(0);

        MessageEntity messageUpdate = new MessageEntity();
        messageUpdate.setIsRead(true);

        assertThrows(IllegalOperationException.class, () -> {
            messageService.updateMessage(existingMessage.getId(), messageUpdate, userList.get(0).getId());
        });
    }

    // ---------- deleteMessage ----------

    @Test
    void testDeleteMessageWithInvalidId() {
        assertThrows(IllegalOperationException.class, () -> {
            messageService.deleteMessage(0L, userList.get(0).getId());
        });
    }

    @Test
    void testDeleteMessageThatDoesNotExist() {
        assertThrows(EntityNotFoundException.class, () -> {
            messageService.deleteMessage(messageList.get(2).getId() + 10000L, userList.get(0).getId());
        });
    }

    @Test
    void testDeleteMessageWithoutPermission() {
        MessageEntity existingMessage = messageList.get(0);

        assertThrows(IllegalOperationException.class, () -> {
            messageService.deleteMessage(existingMessage.getId(), userList.get(2).getId());
        });
    }

    @Test
    void testDeleteMessageHidesForRequestingUser() throws EntityNotFoundException, IllegalOperationException {
        MessageEntity existingMessage = messageList.get(0);
        Long requesterId = userList.get(0).getId();

        boolean isSender = existingMessage.getSendUser().getId().equals(requesterId);
        boolean isReceiver = existingMessage.getReceivesUser().getId().equals(requesterId);
        assertTrue(isSender || isReceiver,
                "This test assumes userList.get(0) is the sender or recipient of messageList.get(0)");

        messageService.deleteMessage(existingMessage.getId(), requesterId);

        MessageEntity updated = messageRepository.findById(existingMessage.getId()).orElseThrow();
        if (isSender) {
            assertTrue(updated.getHiddenForSender());
            assertFalse(updated.getHiddenForReceiver());
        } else {
            assertTrue(updated.getHiddenForReceiver());
            assertFalse(updated.getHiddenForSender());
        }
    }

    @Test
    void testDeleteMessageHidesForOneSide() throws EntityNotFoundException, IllegalOperationException {
        MessageEntity existingMessage = messageList.get(0);
        Long senderId = existingMessage.getSendUser().getId();

        messageService.deleteMessage(existingMessage.getId(), senderId);

        MessageEntity updated = messageRepository.findById(existingMessage.getId()).orElseThrow();
        assertTrue(updated.getHiddenForSender());
        assertFalse(updated.getHiddenForReceiver());
    }

    @Test
    void testDeleteMessagePhysicallyDeletesWhenBothSidesHideIt()
            throws EntityNotFoundException, IllegalOperationException {
        MessageEntity existingMessage = messageList.get(0);
        Long senderId = existingMessage.getSendUser().getId();
        Long receiverId = existingMessage.getReceivesUser().getId();

        messageService.deleteMessage(existingMessage.getId(), senderId);
        messageService.deleteMessage(existingMessage.getId(), receiverId);

        assertTrue(messageRepository.findById(existingMessage.getId()).isEmpty());
    }

    @Test
    void testDeleteMessageByUnrelatedUserThrowsException() {
        MessageEntity existingMessage = messageList.get(0);
        Long unrelatedUserId = -1L;

        assertThrows(IllegalOperationException.class,
                () -> messageService.deleteMessage(existingMessage.getId(), unrelatedUserId));
    }
}