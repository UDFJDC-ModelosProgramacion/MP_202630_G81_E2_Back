package co.edu.udistrital.mdp.ZZZ.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.ZZZ.entities.UserEntity;
import co.edu.udistrital.mdp.ZZZ.repositories.UserRepository;
import co.edu.udistrital.mdp.ZZZ.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.ZZZ.exceptions.IllegalOperationException;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public UserEntity createUser(UserEntity user) throws IllegalOperationException {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty() || 
            user.getPassword() == null || user.getPassword().trim().isEmpty() ||
            user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            throw new IllegalOperationException("Ningún atributo obligatorio puede ser nulo o vacío.");
        }
        if (user.getPassword().length() < 8) {
            throw new IllegalOperationException("La contraseña debe cumplir con los requisitos mínimos de seguridad (mínimo 8 caracteres).");
        }
        Optional<UserEntity> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            throw new IllegalOperationException("El correo electrónico debe ser único en el sistema.");
        }
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserEntity readUser(Long id, Long currentUserId, String currentUserRole) throws EntityNotFoundException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("No se aceptan identificadores inválidos.");
        }
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Si el usuario no existe, se muestra un mensaje indicándolo."));
        
        // Remove sensitive data
        user.setPassword(null);
        return user;
    }

    @Transactional(readOnly = true)
    public List<UserEntity> readAllUsers(Long currentUserId, String currentUserRole) throws IllegalOperationException {
        if (!"ADMIN".equalsIgnoreCase(currentUserRole)) {
            throw new IllegalOperationException("El listado completo de usuarios es de acceso exclusivo para roles de administración.");
        }
        List<UserEntity> users = userRepository.findAll();
        users.forEach(u -> u.setPassword(null)); // Hide sensitive data
        return users;
    }

    @Transactional
    public UserEntity updateUser(Long id, UserEntity userUpdate, Long currentUserId, String currentUserRole) throws EntityNotFoundException, IllegalOperationException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("No se aceptan identificadores inválidos.");
        }
        UserEntity existingUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Si el usuario no existe, se muestra un mensaje indicándolo."));

        if (userUpdate.getEmail() == null || userUpdate.getEmail().trim().isEmpty()) {
            throw new IllegalOperationException("No se aceptan atributos nulos.");
        }

        if (!existingUser.getEmail().equals(userUpdate.getEmail())) {
            Optional<UserEntity> userWithEmail = userRepository.findByEmail(userUpdate.getEmail());
            if (userWithEmail.isPresent()) {
                throw new IllegalOperationException("El correo electrónico no puede actualizarse por uno que ya pertenezca a otro usuario.");
            }
        }
        
        // Note: We avoid updating roles or passwords here to keep it simple and restricted as per rule
        existingUser.setFirstName(userUpdate.getFirstName());
        existingUser.setLastName(userUpdate.getLastName());
        existingUser.setEmail(userUpdate.getEmail());
        existingUser.setPhone(userUpdate.getPhone());
        
        return userRepository.save(existingUser);
    }

    @Transactional
    public void deleteUser(Long id, Long currentUserId, String currentUserRole) throws EntityNotFoundException, IllegalOperationException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("No se aceptan identificadores inválidos.");
        }
        UserEntity existingUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Si el identificador no existe, se muestra un mensaje indicándolo."));

        // Validation for active adoptions or return processes would go here.
        // Assuming we check the lists if they are populated:
        if (!existingUser.getSendMessages().isEmpty() || !existingUser.getReceiveMessages().isEmpty()) {
             // simplified logic
             throw new IllegalOperationException("No se puede eliminar un usuario que tenga procesos activos.");
        }
        
        userRepository.delete(existingUser);
    }
}
