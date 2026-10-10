package co.edu.udistrital.mdp.pets.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import co.edu.udistrital.mdp.pets.dto.ReturnDTO;
import co.edu.udistrital.mdp.pets.dto.ReturnDetailDTO;
import co.edu.udistrital.mdp.pets.entities.ReturnEntity;
import co.edu.udistrital.mdp.pets.services.ReturnService;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;

@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    @Autowired
    private ReturnService returnService;

    @Autowired
    private ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<ReturnDTO> createReturn(@RequestBody ReturnDTO returnDto) throws EntityNotFoundException, IllegalOperationException {
        ReturnEntity entity = modelMapper.map(returnDto, ReturnEntity.class);
        ReturnEntity created = returnService.createReturn(entity);
        return new ResponseEntity<>(modelMapper.map(created, ReturnDTO.class), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReturnDetailDTO> getReturn(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        ReturnEntity entity = returnService.readReturn(id);
        return new ResponseEntity<>(modelMapper.map(entity, ReturnDetailDTO.class), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<ReturnDTO>> getAllReturns(
            @RequestParam(required = false) Long shelterId,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long userId) {
        List<ReturnEntity> list = returnService.readAllReturns(shelterId, petId, userId);
        List<ReturnDTO> dtoList = list.stream().map(e -> modelMapper.map(e, ReturnDTO.class)).collect(Collectors.toList());
        return new ResponseEntity<>(dtoList, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReturnDTO> updateReturn(@PathVariable Long id, @RequestBody ReturnDTO returnDto) throws EntityNotFoundException, IllegalOperationException {
        ReturnEntity entity = modelMapper.map(returnDto, ReturnEntity.class);
        ReturnEntity updated = returnService.updateReturn(id, entity);
        return new ResponseEntity<>(modelMapper.map(updated, ReturnDTO.class), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReturn(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        returnService.deleteReturn(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
