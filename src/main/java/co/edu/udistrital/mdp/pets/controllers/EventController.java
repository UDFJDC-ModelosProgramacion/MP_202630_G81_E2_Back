package co.edu.udistrital.mdp.pets.controllers;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udistrital.mdp.pets.dto.EventDTO;
import co.edu.udistrital.mdp.pets.entities.EventEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.EventService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor 
@RestController 
@RequestMapping("/shelters")
public class EventController {
    
    private final EventService eventService;
    private final ModelMapper modelMapper;

    // create method
    @PostMapping(value = "/{shelterId}/events")
    @ResponseStatus(code = HttpStatus.CREATED)
    public EventDTO createEvent(@PathVariable Long shelterId, @RequestBody EventDTO eventDTO) throws EntityNotFoundException, IllegalOperationException {
        EventEntity eventEntity = modelMapper.map(eventDTO, EventEntity.class);
        EventEntity newEvent = eventService.createEvent(shelterId, eventEntity);
        return modelMapper.map(newEvent, EventDTO.class);
    }

    // getAll method
    @GetMapping(value = "/{shelterId}/events")
    @ResponseStatus(code = HttpStatus.OK)
    public List<EventDTO> getEvents(@PathVariable Long shelterId) throws EntityNotFoundException, IllegalOperationException {
        List<EventEntity> events = eventService.getEvents(shelterId);
        return modelMapper.map(events, new TypeToken<List<EventDTO>>() {     
        }.getType());
    }

    // get method
    @GetMapping(value = "/{shelterId}/events/{eventId}")
    @ResponseStatus(code = HttpStatus.OK)
    public EventDTO getEvent(@PathVariable Long shelterId, @PathVariable Long eventId) throws EntityNotFoundException, IllegalOperationException {
        EventEntity eventEntity = eventService.getEvent(shelterId, eventId);
        return modelMapper.map(eventEntity, EventDTO.class);
    }

    // update method
    @PutMapping(value = "/{shelterId}/events/{eventId}")
    @ResponseStatus(code = HttpStatus.OK)
    public EventDTO updateEvent(@PathVariable Long shelterId, @PathVariable Long eventId, @RequestBody EventDTO eventDTO) throws EntityNotFoundException, IllegalOperationException {
        EventEntity eventEntity = modelMapper.map(eventDTO, EventEntity.class);
        EventEntity newEventEntity = eventService.updateEvent(shelterId, eventId, eventEntity);
        return modelMapper.map(newEventEntity, EventDTO.class);
    }

    // delete method
    @DeleteMapping(value = "/{shelterId}/events/{eventId}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable Long shelterId, @PathVariable Long eventId) throws EntityNotFoundException, IllegalOperationException {
        eventService.deleteEvent(shelterId, eventId);
    }
}