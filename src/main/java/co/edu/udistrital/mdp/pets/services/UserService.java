package co.edu.udistrital.mdp.pets.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class UserService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    private static final String USER_ID_NOT_VALID = "Invalid identifiers are not accepted.";
    private static final String USER_NOT_FOUND = "If the user does not exist, a message indicating this is displayed.";

    @Transactional
    public UserEntity createUser(UserEntity user) throws IllegalOperationException {
        log.info("User creation process started");

        if (user.getEmail() == null || user.getEmail().trim().isEmpty() ||
            user.getPassword() == null || user.getPassword().trim().isEmpty() ||
            user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            throw new IllegalOperationException("Required attributes cannot be null or empty.");
        }

        if (user.getPassword().length() < 8) {
            throw new IllegalOperationException("The password must meet the minimum security requirements (at least 8 characters).");
        }

        Optional<UserEntity> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            throw new IllegalOperationException("The email address must be unique in the system.");
        }

        UserEntity savedUser = userRepository.save(user);
        log.info("User creation process completed");
        return savedUser;
    }

    @Transactional(readOnly = true)
    public UserEntity readUser(Long id, Long currentUserId, String currentUserRole)
            throws EntityNotFoundException, IllegalOperationException {

        log.info("User query process started for id = {}", id);

        if (id == null || id <= 0) {
            throw new IllegalOperationException(USER_ID_NOT_VALID);
        }

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        user.setPassword(null);

        log.info("User query process completed for id = {}", id);
        return user;
    }

    @Transactional(readOnly = true)
    public List<UserEntity> readAllUsers(Long currentUserId, String currentUserRole)
            throws IllegalOperationException {

        log.info("Process of querying all users started");

        if (!"ADMIN".equalsIgnoreCase(currentUserRole)) {
            throw new IllegalOperationException(
                    "The complete list of users is exclusively accessible to administrator roles.");
        }

        List<UserEntity> users = userRepository.findAll();
        users.forEach(u -> u.setPassword(null));

        log.info("Process of querying all users completed");
        return users;
    }

    @Transactional
    public UserEntity updateUser(Long id, UserEntity userUpdate, Long currentUserId, String currentUserRole)
            throws EntityNotFoundException, IllegalOperationException {

        log.info("User update process started for id = {}", id);

        if (id == null || id <= 0) {
            throw new IllegalOperationException(USER_ID_NOT_VALID);
        }

        UserEntity existingUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        if (userUpdate.getEmail() == null || userUpdate.getEmail().trim().isEmpty()) {
            throw new IllegalOperationException("Null attributes are not accepted.");
        }

        if (!existingUser.getEmail().equals(userUpdate.getEmail())) {
            Optional<UserEntity> userWithEmail = userRepository.findByEmail(userUpdate.getEmail());
            if (userWithEmail.isPresent()) {
                throw new IllegalOperationException(
                        "The email address cannot be updated to one that already belongs to another user.");
            }
        }

        existingUser.setFirstName(userUpdate.getFirstName());
        existingUser.setLastName(userUpdate.getLastName());
        existingUser.setEmail(userUpdate.getEmail());
        existingUser.setPhone(userUpdate.getPhone());

        UserEntity savedUser = userRepository.save(existingUser);

        log.info("User update process completed for id = {}", id);
        return savedUser;
    }

    @Transactional
    public void deleteUser(Long id, Long currentUserId, String currentUserRole)
            throws EntityNotFoundException, IllegalOperationException {

        log.info("User deletion process started for id = {}", id);

        if (id == null || id <= 0) {
            throw new IllegalOperationException(USER_ID_NOT_VALID);
        }

        UserEntity existingUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        boolean hasMessages = !messageRepository.findBySendUserIdOrReceivesUserId(id, id).isEmpty();

        if (hasMessages) {
            throw new IllegalOperationException(
                    "A user who has active processes cannot be deleted.");
        }

        userRepository.delete(existingUser);

        log.info("User deletion process completed for id = {}", id);
    }
}