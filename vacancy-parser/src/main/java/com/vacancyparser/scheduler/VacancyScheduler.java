package com.vacancyparser.scheduler;

import com.vacancyparser.service.VacancyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class VacancyScheduler {

    private final VacancyService vacancyService;
    
    @Value("${parser.schedule.initial.delay:5000}")
    private long initialDelay;
    
    @Value("${parser.schedule.fixed.delay:300000}")
    private long fixedDelay;
    
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);
    
    // Default URLs for scheduled parsing
    private static final List<String> DEFAULT_URLS = Arrays.asList(
            "https://hh.ru/search/vacancy?text=java&area=1",
            "https://www.superjob.ru/vacancy/search/?keywords=java",
            "https://career.habr.com/vacancies?q=java"
    );

    @Scheduled(initialDelayString = "${parser.schedule.initial.delay:5000}", 
               fixedDelayString = "${parser.schedule.fixed.delay:300000}")
    public void scheduledParse() {
        log.info("Scheduled parsing started");
        try {
            vacancyService.parseVacancies(DEFAULT_URLS, 5);
            log.info("Scheduled parsing completed");
        } catch (Exception e) {
            log.error("Error in scheduled parsing: {}", e.getMessage(), e);
        }
    }

    // Alternative using ScheduledExecutorService
    public void startScheduledExecutor() {
        scheduledExecutor.scheduleWithFixedDelay(
                () -> {
                    log.info("ScheduledExecutorService parsing started");
                    try {
                        vacancyService.parseVacancies(DEFAULT_URLS, 5);
                    } catch (Exception e) {
                        log.error("Error in ScheduledExecutorService parsing: {}", e.getMessage());
                    }
                },
                initialDelay,
                fixedDelay,
                TimeUnit.MILLISECONDS
        );
    }

    public void shutdown() {
        scheduledExecutor.shutdown();
    }
}
