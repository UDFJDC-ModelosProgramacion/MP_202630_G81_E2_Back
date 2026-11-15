package co.edu.udistrital.mdp.pets.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class DefaultController {
    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public HealthResponse health() {
        return new HealthResponse("OK", "REST API for pets is running");
    }

    public record HealthResponse(String status, String message) {
    }
}
