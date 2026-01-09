package com.vacancyparser.service;

import com.vacancyparser.model.Vacancy;
import com.vacancyparser.parser.VacancyParser;
import com.vacancyparser.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VacancyService {

    private final VacancyRepository vacancyRepository;
    private final VacancyParser vacancyParser;
    private final LoggingService loggingService;
    
    @Value("${parser.thread.pool.size:10}")
    private int threadPoolSize;
    
    private ExecutorService executorService;
    private final Set<String> processedUrls = ConcurrentHashMap.newKeySet();
    private final BlockingQueue<Vacancy> vacancyQueue = new LinkedBlockingQueue<>();
    
    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    @Transactional
    public void parseVacancies(List<String> urls, Integer maxPages) {
        log.info("Starting parsing for {} URLs with max {} pages", urls.size(), maxPages);
        
        List<Future<List<Vacancy>>> futures = new ArrayList<>();
        
        for (String url : urls) {
            Future<List<Vacancy>> future = executorService.submit(() -> {
                String source = vacancyParser.detectSource(url);
                List<Vacancy> vacancies = new ArrayList<>();
                
                try {
                    switch (source) {
                        case "hh":
                            vacancies = vacancyParser.parseHhRu(url);
                            break;
                        case "superjob":
                            vacancies = vacancyParser.parseSuperJob(url);
                            break;
                        case "habr":
                            vacancies = vacancyParser.parseHabrCareer(url);
                            break;
                        default:
                            log.warn("Unknown source for URL: {}", url);
                    }
                    
                    // Filter duplicates and save
                    List<Vacancy> newVacancies = vacancies.stream()
                            .filter(v -> !processedUrls.contains(v.getSourceUrl()))
                            .collect(Collectors.toList());
                    
                    if (!newVacancies.isEmpty()) {
                        vacancyRepository.saveAll(newVacancies);
                        newVacancies.forEach(v -> processedUrls.add(v.getSourceUrl()));
                        vacancyQueue.addAll(newVacancies);
                        log.info("Saved {} new vacancies from {}", newVacancies.size(), url);
                        loggingService.log(String.format("Saved %d new vacancies from %s", newVacancies.size(), url));
                    } else {
                        log.warn("No new vacancies found from {} (found {} total, but all duplicates)", url, vacancies.size());
                    }
                    
                } catch (Exception e) {
                    log.error("Error parsing URL {}: {}", url, e.getMessage(), e);
                }
                
                return vacancies;
            });
            
            futures.add(future);
        }
        
        // Wait for all tasks to complete
        for (Future<List<Vacancy>> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Error waiting for parsing task: {}", e.getMessage());
            }
        }
        
        log.info("Parsing completed");
    }

    @Transactional(readOnly = true)
    public List<Vacancy> getAllVacancies() {
        return vacancyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Vacancy> getVacanciesBySource(String source) {
        return vacancyRepository.findBySource(source);
    }

    @Transactional(readOnly = true)
    public List<Vacancy> getVacanciesByCity(String city) {
        return vacancyRepository.findByCity(city);
    }

    @Transactional(readOnly = true)
    public List<Vacancy> getVacanciesSorted(String sortBy, String order) {
        List<Vacancy> vacancies = vacancyRepository.findAll();
        
        Comparator<Vacancy> comparator = switch (sortBy.toLowerCase()) {
            case "date" -> Comparator.comparing(Vacancy::getPublishedDate);
            case "title" -> Comparator.comparing(Vacancy::getTitle);
            case "company" -> Comparator.comparing(Vacancy::getCompany);
            case "city" -> Comparator.comparing(Vacancy::getCity);
            default -> Comparator.comparing(Vacancy::getId);
        };
        
        if ("desc".equalsIgnoreCase(order)) {
            comparator = comparator.reversed();
        }
        
        return vacancies.parallelStream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Vacancy> getVacanciesFiltered(String source, String city, String company) {
        List<Vacancy> allVacancies = vacancyRepository.findAll();
        
        return allVacancies.parallelStream()
                .filter(v -> source == null || v.getSource().equals(source))
                .filter(v -> city == null || v.getCity().equals(city))
                .filter(v -> company == null || v.getCompany().contains(company))
                .collect(Collectors.toList());
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
