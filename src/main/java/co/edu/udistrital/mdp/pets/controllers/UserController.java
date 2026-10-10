package co.edu.udistrital.mdp.pets.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import co.edu.udistrital.mdp.pets.dto.UserDTO;
import co.edu.udistrital.mdp.pets.dto.UserDetailDTO;
import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.services.UserService;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDto) throws IllegalOperationException {
        UserEntity userEntity = modelMapper.map(userDto, UserEntity.class);
        UserEntity createdUser = userService.createUser(userEntity);
        return new ResponseEntity<>(modelMapper.map(createdUser, UserDTO.class), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDetailDTO> getUser(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        UserEntity userEntity = userService.readUser(id);
        return new ResponseEntity<>(modelMapper.map(userEntity, UserDetailDTO.class), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers(@RequestParam(required = false, defaultValue = "ADMIN") String role) throws IllegalOperationException {
        List<UserEntity> users = userService.readAllUsers(role);
        List<UserDTO> usersDto = users.stream().map(user -> modelMapper.map(user, UserDTO.class)).collect(Collectors.toList());
        return new ResponseEntity<>(usersDto, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDto) throws EntityNotFoundException, IllegalOperationException {
        UserEntity userEntity = modelMapper.map(userDto, UserEntity.class);
        UserEntity updatedUser = userService.updateUser(id, userEntity);
        return new ResponseEntity<>(modelMapper.map(updatedUser, UserDTO.class), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
