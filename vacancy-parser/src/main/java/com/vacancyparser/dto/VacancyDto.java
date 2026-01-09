package com.vacancyparser.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VacancyDto {
    private Long id;
    private String title;
    private String company;
    private String salary;
    private String requirements;
    private String city;
    private LocalDateTime publishedDate;
    private String sourceUrl;
    private String source;
    private LocalDateTime parsedAt;
}
