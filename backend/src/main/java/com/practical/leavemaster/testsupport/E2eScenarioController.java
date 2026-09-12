package com.practical.leavemaster.testsupport;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@Profile("e2e")
@RequestMapping("/test/scenarios")
@RequiredArgsConstructor
public class E2eScenarioController {

    private final E2eScenarioBootstrapService bootstrapService;

    @PostMapping("/standard-sg-company")
    @ResponseStatus(HttpStatus.CREATED)
    public E2eScenarioBootstrapService.ScenarioBootstrapResult createStandardSingaporeScenario(
            @RequestParam String scenarioId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate) {
        try {
            return bootstrapService.createStandardSingaporeScenario(
                    scenarioId,
                    referenceDate == null ? LocalDate.now() : referenceDate);
        } catch (E2eScenarioBootstrapService.ScenarioAlreadyExistsException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @DeleteMapping("/{scenarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteScenario(@PathVariable String scenarioId) {
        try {
            bootstrapService.deleteScenario(scenarioId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
