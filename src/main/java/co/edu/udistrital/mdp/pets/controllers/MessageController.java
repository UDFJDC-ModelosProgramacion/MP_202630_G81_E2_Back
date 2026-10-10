package co.edu.udistrital.mdp.pets.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import co.edu.udistrital.mdp.pets.dto.MessageDTO;
import co.edu.udistrital.mdp.pets.dto.MessageDetailDTO;
import co.edu.udistrital.mdp.pets.entities.MessageEntity;
import co.edu.udistrital.mdp.pets.services.MessageService;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<MessageDTO> createMessage(@RequestBody MessageDTO messageDto,
            @RequestParam(required = false, defaultValue = "1") Long currentUserId) throws EntityNotFoundException, IllegalOperationException {
        MessageEntity entity = modelMapper.map(messageDto, MessageEntity.class);
        MessageEntity created = messageService.createMessage(entity, currentUserId);
        return new ResponseEntity<>(modelMapper.map(created, MessageDTO.class), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageDetailDTO> getMessage(@PathVariable Long id,
            @RequestParam(required = false, defaultValue = "1") Long currentUserId) throws EntityNotFoundException, IllegalOperationException {
        MessageEntity entity = messageService.readMessage(id, currentUserId);
        return new ResponseEntity<>(modelMapper.map(entity, MessageDetailDTO.class), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<MessageDTO>> getAllMessages(
            @RequestParam(required = false, defaultValue = "1") Long currentUserId) {
        List<MessageEntity> list = messageService.readAllMessages(currentUserId);
        List<MessageDTO> dtoList = list.stream().map(e -> modelMapper.map(e, MessageDTO.class)).collect(Collectors.toList());
        return new ResponseEntity<>(dtoList, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MessageDTO> updateMessage(@PathVariable Long id, @RequestBody MessageDTO messageDto,
            @RequestParam(required = false, defaultValue = "1") Long currentUserId) throws EntityNotFoundException, IllegalOperationException {
        MessageEntity entity = modelMapper.map(messageDto, MessageEntity.class);
        MessageEntity updated = messageService.updateMessage(id, entity, currentUserId);
        return new ResponseEntity<>(modelMapper.map(updated, MessageDTO.class), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id,
            @RequestParam(required = false, defaultValue = "1") Long currentUserId) throws EntityNotFoundException, IllegalOperationException {
        messageService.deleteMessage(id, currentUserId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
