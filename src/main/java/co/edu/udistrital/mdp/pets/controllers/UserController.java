package co.edu.udistrital.mdp.pets.controllers;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udistrital.mdp.pets.dto.UserCreateDTO;
import co.edu.udistrital.mdp.pets.dto.UserDTO;
import co.edu.udistrital.mdp.pets.dto.UserRoleUpdateDTO;
import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.UserService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {

	private final UserService userService;
	private final ModelMapper modelMapper;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UserDTO create(@RequestBody UserCreateDTO dto) throws IllegalOperationException {
		UserEntity user = userService.createUser(modelMapper.map(dto, UserEntity.class));
		return modelMapper.map(user, UserDTO.class);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessUser(authentication, #id)")
	@ResponseStatus(HttpStatus.OK)
	public UserDTO findOne(@PathVariable Long id)
			throws EntityNotFoundException, IllegalOperationException {
		return modelMapper.map(userService.readUser(id), UserDTO.class);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessUser(authentication, #id)")
	@ResponseStatus(HttpStatus.OK)
	public UserDTO update(@PathVariable Long id, @RequestBody UserDTO dto)
			throws EntityNotFoundException, IllegalOperationException {
		UserEntity user = userService.updateUser(id, modelMapper.map(dto, UserEntity.class));
		return modelMapper.map(user, UserDTO.class);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessUser(authentication, #id)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
		userService.deleteUser(id);
	}

	@PutMapping("/{id}/role")
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.OK)
	public UserDTO assignRole(@PathVariable Long id, @RequestBody UserRoleUpdateDTO dto)
			throws EntityNotFoundException, IllegalOperationException {
		UserEntity user = userService.assignRole(id, dto.getRole(), dto.getShelterId(), dto.getPassword());
		return modelMapper.map(user, UserDTO.class);
	}
}
