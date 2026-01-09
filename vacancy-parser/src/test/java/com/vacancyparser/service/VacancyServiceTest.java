package com.vacancyparser.service;

import com.vacancyparser.model.Vacancy;
import com.vacancyparser.parser.VacancyParser;
import com.vacancyparser.repository.VacancyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VacancyServiceTest {

    @Mock
    private VacancyRepository vacancyRepository;

    @Mock
    private VacancyParser vacancyParser;

    @InjectMocks
    private VacancyService vacancyService;

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
    }

    @Test
    void testGetAllVacancies() {
        List<Vacancy> vacancies = Arrays.asList(testVacancy);
        when(vacancyRepository.findAll()).thenReturn(vacancies);

        List<Vacancy> result = vacancyService.getAllVacancies();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());
        verify(vacancyRepository, times(1)).findAll();
    }

    @Test
    void testGetVacanciesBySource() {
        List<Vacancy> vacancies = Arrays.asList(testVacancy);
        when(vacancyRepository.findBySource("hh")).thenReturn(vacancies);

        List<Vacancy> result = vacancyService.getVacanciesBySource("hh");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("hh", result.get(0).getSource());
        verify(vacancyRepository, times(1)).findBySource("hh");
    }

    @Test
    void testGetVacanciesByCity() {
        List<Vacancy> vacancies = Arrays.asList(testVacancy);
        when(vacancyRepository.findByCity("Moscow")).thenReturn(vacancies);

        List<Vacancy> result = vacancyService.getVacanciesByCity("Moscow");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Moscow", result.get(0).getCity());
        verify(vacancyRepository, times(1)).findByCity("Moscow");
    }

    @Test
    void testGetVacanciesSorted() {
        Vacancy vacancy2 = new Vacancy();
        vacancy2.setId(2L);
        vacancy2.setTitle("Python Developer");
        vacancy2.setPublishedDate(LocalDateTime.now().minusDays(1));

        List<Vacancy> vacancies = Arrays.asList(testVacancy, vacancy2);
        when(vacancyRepository.findAll()).thenReturn(vacancies);

        List<Vacancy> result = vacancyService.getVacanciesSorted("title", "asc");

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(vacancyRepository, times(1)).findAll();
    }

    @Test
    void testGetVacanciesFiltered() {
        List<Vacancy> vacancies = Arrays.asList(testVacancy);
        when(vacancyRepository.findAll()).thenReturn(vacancies);

        List<Vacancy> result = vacancyService.getVacanciesFiltered("hh", "Moscow", null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(vacancyRepository, times(1)).findAll();
    }

    @Test
    void testParseVacancies() {
        List<String> urls = Arrays.asList("https://hh.ru/search/vacancy?text=java");
        when(vacancyParser.detectSource(any())).thenReturn("hh");
        when(vacancyParser.parseHhRu(any())).thenReturn(Arrays.asList(testVacancy));
        when(vacancyRepository.saveAll(any())).thenReturn(Arrays.asList(testVacancy));

        vacancyService.parseVacancies(urls, 1);

        verify(vacancyParser, times(1)).detectSource(any());
        verify(vacancyParser, times(1)).parseHhRu(any());
    }
}
