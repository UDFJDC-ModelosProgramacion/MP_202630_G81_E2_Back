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
@RequestMapping("/users/{userId}/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final ModelMapper modelMapper;

    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<NotificationDTO> getNotifications(@PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate)
            throws EntityNotFoundException {
        List<NotificationEntity> notifications = notificationService.getNotifications(userId, startDate, endDate);
        return modelMapper.map(notifications, new TypeToken<List<NotificationDTO>>() {
        }.getType());
    }

    @GetMapping(value = "/{notificationId}")
    @ResponseStatus(code = HttpStatus.OK)
    public NotificationDTO getNotification(@PathVariable Long userId, @PathVariable Long notificationId)
            throws EntityNotFoundException, IllegalOperationException {
        NotificationEntity entity = notificationService.getNotification(userId, notificationId);
        return modelMapper.map(entity, NotificationDTO.class);
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public NotificationDTO createNotification(@PathVariable Long userId, @RequestBody NotificationDTO dto)
            throws EntityNotFoundException, IllegalOperationException {
        NotificationEntity entity = notificationService.createNotification(userId,
                modelMapper.map(dto, NotificationEntity.class));
        return modelMapper.map(entity, NotificationDTO.class);
    }

    @PutMapping(value = "/{notificationId}")
    @ResponseStatus(code = HttpStatus.OK)
    public NotificationDTO updateNotification(@PathVariable Long userId, @PathVariable Long notificationId,
            @RequestBody NotificationDTO dto) throws EntityNotFoundException, IllegalOperationException {
        NotificationEntity entity = notificationService.updateNotification(userId, notificationId,
                modelMapper.map(dto, NotificationEntity.class));
        return modelMapper.map(entity, NotificationDTO.class);
    }

    @DeleteMapping(value = "/{notificationId}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void deleteNotification(@PathVariable Long userId, @PathVariable Long notificationId)
            throws EntityNotFoundException, IllegalOperationException {
        notificationService.deleteNotification(userId, notificationId);
    }
}