package co.edu.udistrital.mdp.pets.controllers;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udistrital.mdp.pets.dto.EventDTO;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.EventService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/events")
public class EventQueryController {

    private final EventService eventService;
    private final ModelMapper modelMapper;

    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<EventDTO> findAll(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date)
            throws IllegalOperationException {
                
        Date day = date == null ? null : Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return modelMapper.map(eventService.searchEvents(id, name, day),
                new TypeToken<List<EventDTO>>() {}.getType());
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(code = HttpStatus.OK)
    public EventDTO findOne(@PathVariable Long eventId)
            throws EntityNotFoundException, IllegalOperationException {
        return modelMapper.map(eventService.getEvent(eventId), EventDTO.class);
    }
}