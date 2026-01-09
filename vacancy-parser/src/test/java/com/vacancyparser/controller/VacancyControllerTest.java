package com.vacancyparser.controller;

import com.vacancyparser.dto.VacancyDto;
import com.vacancyparser.model.Vacancy;
import com.vacancyparser.service.VacancyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VacancyControllerTest {

    @Mock
    private VacancyService vacancyService;

    @InjectMocks
    private VacancyController vacancyController;

    private Vacancy testVacancy;

    @BeforeEach
    void setUp() {
        testVacancy = new Vacancy();
        testVacancy.setId(1L);
        testVacancy.setTitle("Java Developer");
        testVacancy.setCompany("Test Company");
        testVacancy.setSalary("100000-150000");
        testVacancy.setCity("Moscow");
        testVacancy.setSource("hh");
        testVacancy.setSourceUrl("https://hh.ru/vacancy/123");
        testVacancy.setPublishedDate(LocalDateTime.now());
        testVacancy.setParsedAt(LocalDateTime.now());
    }

    @Test
    void testGetAllVacancies() {
        List<Vacancy> vacancies = Arrays.asList(testVacancy);
        when(vacancyService.getAllVacancies()).thenReturn(vacancies);

        ResponseEntity<List<VacancyDto>> response = vacancyController.getAllVacancies(
                null, null, null, null, null, 0, 20
        );

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(vacancyService, times(1)).getAllVacancies();
    }

    @Test
    void testGetVacanciesBySource() {
        List<Vacancy> vacancies = Arrays.asList(testVacancy);
        when(vacancyService.getVacanciesBySource("hh")).thenReturn(vacancies);

        ResponseEntity<List<VacancyDto>> response = vacancyController.getVacanciesBySource("hh");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(vacancyService, times(1)).getVacanciesBySource("hh");
    }
}
