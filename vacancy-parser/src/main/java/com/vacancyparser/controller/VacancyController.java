package com.vacancyparser.controller;

import com.vacancyparser.dto.ParseRequest;
import com.vacancyparser.dto.VacancyDto;
import com.vacancyparser.model.Vacancy;
import com.vacancyparser.service.VacancyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vacancies")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class VacancyController {

    private final VacancyService vacancyService;

    @PostMapping("/parse")
    public ResponseEntity<String> parseVacancies(@RequestBody ParseRequest request) {
        try {
            vacancyService.parseVacancies(
                    request.getUrls(),
                    request.getMaxPages() != null ? request.getMaxPages() : 10
            );
            return ResponseEntity.ok("Parsing started successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error starting parsing: " + e.getMessage());
        }
    }

    @GetMapping("/answer")
    public ResponseEntity<List<VacancyDto>> getAllVacancies(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String company,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "1000") int size
    ) {
        List<Vacancy> vacancies;
        
        if (source != null || city != null || company != null) {
            vacancies = vacancyService.getVacanciesFiltered(source, city, company);
        } else if (sortBy != null) {
            vacancies = vacancyService.getVacanciesSorted(
                    sortBy,
                    order != null ? order : "asc"
            );
        } else {
            vacancies = vacancyService.getAllVacancies();
        }
        
        // Pagination
        int start = page * size;
        int end = Math.min(start + size, vacancies.size());
        List<Vacancy> paginated = vacancies.subList(
                Math.min(start, vacancies.size()),
                end
        );
        
        List<VacancyDto> dtos = paginated.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/source/{source}")
    public ResponseEntity<List<VacancyDto>> getVacanciesBySource(@PathVariable String source) {
        List<VacancyDto> dtos = vacancyService.getVacanciesBySource(source).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<List<VacancyDto>> getVacanciesByCity(@PathVariable String city) {
        List<VacancyDto> dtos = vacancyService.getVacanciesByCity(city).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private VacancyDto toDto(Vacancy vacancy) {
        return new VacancyDto(
                vacancy.getId(),
                vacancy.getTitle(),
                vacancy.getCompany(),
                vacancy.getSalary(),
                vacancy.getRequirements(),
                vacancy.getCity(),
                vacancy.getPublishedDate(),
                vacancy.getSourceUrl(),
                vacancy.getSource(),
                vacancy.getParsedAt()
        );
    }
}
