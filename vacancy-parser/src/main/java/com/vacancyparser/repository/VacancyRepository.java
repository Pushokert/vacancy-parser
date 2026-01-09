package com.vacancyparser.repository;

import com.vacancyparser.model.Vacancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VacancyRepository extends JpaRepository<Vacancy, Long> {
    List<Vacancy> findBySource(String source);
    List<Vacancy> findByCity(String city);
    List<Vacancy> findByCompany(String company);
    
    @Query("SELECT v FROM Vacancy v WHERE v.publishedDate >= :fromDate")
    List<Vacancy> findRecentVacancies(LocalDateTime fromDate);
    
    @Query("SELECT DISTINCT v.city FROM Vacancy v")
    List<String> findAllCities();
    
    @Query("SELECT DISTINCT v.source FROM Vacancy v")
    List<String> findAllSources();
}
