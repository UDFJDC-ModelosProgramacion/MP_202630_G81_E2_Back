package co.edu.udistrital.mdp.pets.controllers;

import java.util.Date;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udistrital.mdp.pets.dto.NotificationDTO;
import co.edu.udistrital.mdp.pets.entities.NotificationEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.NotificationService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final ModelMapper modelMapper;

    
    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<NotificationDTO> findAll(@RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<NotificationEntity> notifications = notificationService.getNotifications(userId, startDate, endDate);
        return modelMapper.map(notifications, new TypeToken<List<NotificationDTO>>() {
        }.getType());
    }

    
    @GetMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public NotificationDTO findOne(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        NotificationEntity entity = notificationService.getNotification(id);
        return modelMapper.map(entity, NotificationDTO.class);
    }

    
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public NotificationDTO create(@RequestBody NotificationDTO dto) throws IllegalOperationException {
        NotificationEntity entity = notificationService.createNotification(modelMapper.map(dto, NotificationEntity.class));
        return modelMapper.map(entity, NotificationDTO.class);
    }

    
    @PutMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public NotificationDTO update(@PathVariable Long id, @RequestBody NotificationDTO dto)
            throws EntityNotFoundException, IllegalOperationException {
        NotificationEntity entity = notificationService.updateNotification(id,
                modelMapper.map(dto, NotificationEntity.class));
        return modelMapper.map(entity, NotificationDTO.class);
    }

    
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        notificationService.deleteNotification(id);
    }
}