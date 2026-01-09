package com.vacancyparser.parser;

import com.vacancyparser.model.Vacancy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class VacancyParser {
    
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT = 30000;

    public List<Vacancy> parseHhRu(String url) {
        List<Vacancy> vacancies = new ArrayList<>();
        try {
            log.info("Parsing hh.ru: {}", url);
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .referrer("https://hh.ru")
                    .followRedirects(true)
                    .get();

            Elements vacancyElements = doc.select("div[data-qa='vacancy-serp__vacancy']");
            if (vacancyElements.isEmpty()) {
                vacancyElements = doc.select("div.vacancy-serp-item");
            }
            if (vacancyElements.isEmpty()) {
                vacancyElements = doc.select("div[class*='vacancy']");
            }
            
            log.info("Found {} vacancy elements on hh.ru", vacancyElements.size());
            
            int processedCount = 0;
            for (Element element : vacancyElements) {
                Vacancy vacancy = new Vacancy();
                
                Element titleElement = element.selectFirst("a[data-qa='vacancy-serp__vacancy-title']");
                if (titleElement == null) {
                    titleElement = element.selectFirst("a[data-qa*='title']");
                }
                if (titleElement == null) {
                    titleElement = element.selectFirst("a.bloko-link");
                }
                if (titleElement == null) {
                    titleElement = element.selectFirst("h3 a, h2 a");
                }
                if (titleElement != null) {
                    String title = titleElement.text().trim();
                    if (!title.isEmpty()) {
                        vacancy.setTitle(title);
                        String href = titleElement.attr("abs:href");
                        if (href.isEmpty()) {
                            href = titleElement.attr("href");
                            if (!href.startsWith("http")) {
                                href = "https://hh.ru" + href;
                            }
                        }
                        vacancy.setSourceUrl(href);
                    }
                }
                
                Element companyElement = element.selectFirst("a[data-qa='vacancy-serp__vacancy-employer']");
                if (companyElement == null) {
                    companyElement = element.selectFirst("a[data-qa*='employer']");
                }
                if (companyElement == null) {
                    companyElement = element.selectFirst("span[data-qa*='employer']");
                }
                if (companyElement != null) {
                    vacancy.setCompany(companyElement.text().trim());
                }
                
                Element salaryElement = element.selectFirst("span[data-qa='vacancy-serp__vacancy-compensation']");
                if (salaryElement == null) {
                    salaryElement = element.selectFirst("span[data-qa*='compensation']");
                }
                if (salaryElement == null) {
                    salaryElement = element.selectFirst("span[class*='salary']");
                }
                if (salaryElement != null) {
                    vacancy.setSalary(salaryElement.text().trim());
                }
                
                Element cityElement = element.selectFirst("div[data-qa='vacancy-serp__vacancy-address']");
                if (cityElement == null) {
                    cityElement = element.selectFirst("span[data-qa*='address']");
                }
                if (cityElement == null) {
                    cityElement = element.selectFirst("div[data-qa*='address']");
                }
                if (cityElement != null) {
                    String cityText = cityElement.text().trim();
                    if (!cityText.isEmpty()) {
                        vacancy.setCity(cityText.split(",")[0].split("•")[0].trim());
                    }
                }
                
                // Date
                Element dateElement = element.selectFirst("span[data-qa='vacancy-serp__vacancy-date']");
                if (dateElement == null) {
                    dateElement = element.selectFirst("span[data-qa*='date']");
                }
                if (dateElement != null) {
                    vacancy.setPublishedDate(parseDate(dateElement.text()));
                }
                
                // Requirements
                Element requirementsElement = element.selectFirst("div[data-qa='vacancy-serp__vacancy_snippet_responsibility']");
                if (requirementsElement == null) {
                    requirementsElement = element.selectFirst("div[data-qa*='responsibility']");
                }
                if (requirementsElement != null) {
                    vacancy.setRequirements(requirementsElement.text().trim());
                }
                
                vacancy.setSource("hh");
                if (vacancy.getPublishedDate() == null) {
                    vacancy.setPublishedDate(LocalDateTime.now());
                }
                
                if (vacancy.getTitle() != null && !vacancy.getTitle().isEmpty()) {
                    if (vacancy.getCity() == null || vacancy.getCity().isEmpty()) {
                        vacancy.setCity("Не указан");
                    }
                    if (vacancy.getCompany() == null || vacancy.getCompany().isEmpty()) {
                        vacancy.setCompany("Не указана");
                    }
                    vacancies.add(vacancy);
                    processedCount++;
                } else {
                    log.debug("Skipped element - no title found");
                }
            }
            log.info("Successfully processed {} out of {} elements on hh.ru", processedCount, vacancyElements.size());
            log.info("Parsed {} vacancies from hh.ru", vacancies.size());
        } catch (IOException e) {
            log.error("Error parsing hh.ru: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error parsing hh.ru: {}", e.getMessage(), e);
        }
        return vacancies;
    }

    public List<Vacancy> parseSuperJob(String url) {
        List<Vacancy> vacancies = new ArrayList<>();
        try {
            log.info("Parsing SuperJob: {}", url);
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .referrer("https://www.superjob.ru")
                    .followRedirects(true)
                    .get();

            Elements vacancyElements = doc.select("div.f-test-vacancy-item");
            if (vacancyElements.isEmpty()) {
                vacancyElements = doc.select("div[class*='vacancy-item']");
            }
            if (vacancyElements.isEmpty()) {
                vacancyElements = doc.select("div[class*='_1h3Zg']");
            }
            if (vacancyElements.isEmpty()) {
                vacancyElements = doc.select("div[data-qa*='vacancy']");
            }
            if (vacancyElements.isEmpty()) {
                vacancyElements = doc.select("article, div[class*='item']");
            }
            
            log.info("Found {} vacancy elements on SuperJob", vacancyElements.size());
            
            if (vacancyElements.isEmpty()) {
                log.warn("SuperJob: No vacancy elements found. Page title: {}", doc.title());
                log.debug("SuperJob: First 500 chars of body: {}", doc.body().text().substring(0, Math.min(500, doc.body().text().length())));
            }
            
            int processedCount = 0;
            for (Element element : vacancyElements) {
                Vacancy vacancy = new Vacancy();
                
                Element titleElement = element.selectFirst("a[href*='/vakansii/']");
                if (titleElement == null) {
                    titleElement = element.selectFirst("a[href*='/vacancy/']");
                }
                if (titleElement == null) {
                    titleElement = element.selectFirst("a._1IHWd");
                }
                if (titleElement == null) {
                    titleElement = element.selectFirst("a[class*='_1IHWd']");
                }
                if (titleElement == null) {
                    titleElement = element.selectFirst("h3 a, h2 a, h4 a");
                }
                if (titleElement == null) {
                    titleElement = element.selectFirst("a[class*='title']");
                }
                if (titleElement == null) {
                    titleElement = element.selectFirst("a");
                }
                if (titleElement != null) {
                    String title = titleElement.text().trim();
                    if (!title.isEmpty()) {
                        vacancy.setTitle(title);
                        String href = titleElement.attr("abs:href");
                        if (href.isEmpty()) {
                            href = titleElement.attr("href");
                            if (!href.startsWith("http")) {
                                href = "https://www.superjob.ru" + href;
                            }
                        }
                        vacancy.setSourceUrl(href);
                    }
                }
                
                Element companyElement = element.selectFirst("span[class*='company']");
                if (companyElement == null) {
                    companyElement = element.selectFirst("a[class*='company']");
                }
                if (companyElement == null) {
                    companyElement = element.selectFirst("span._3nMqD");
                }
                if (companyElement == null) {
                    companyElement = element.selectFirst("span[class*='_3nMqD']");
                }
                if (companyElement == null) {
                    companyElement = element.selectFirst("div[class*='company']");
                }
                if (companyElement != null) {
                    vacancy.setCompany(companyElement.text().trim());
                }
                
                Element salaryElement = element.selectFirst("span[class*='salary']");
                if (salaryElement == null) {
                    salaryElement = element.selectFirst("div[class*='salary']");
                }
                if (salaryElement == null) {
                    salaryElement = element.selectFirst("span._1OuF_");
                }
                if (salaryElement == null) {
                    salaryElement = element.selectFirst("span[class*='_1OuF_']");
                }
                if (salaryElement == null) {
                    Elements allSpans = element.select("span");
                    for (Element span : allSpans) {
                        String text = span.text();
                        if (text.contains("руб") || text.contains("₽") || text.contains("USD") || text.contains("EUR")) {
                            salaryElement = span;
                            break;
                        }
                    }
                }
                if (salaryElement != null) {
                    vacancy.setSalary(salaryElement.text().trim());
                }
                
                Element cityElement = element.selectFirst("span[class*='city']");
                if (cityElement == null) {
                    cityElement = element.selectFirst("div[class*='city']");
                }
                if (cityElement == null) {
                    cityElement = element.selectFirst("span._3mfro");
                }
                if (cityElement == null) {
                    cityElement = element.selectFirst("span[class*='_3mfro']");
                }
                if (cityElement == null) {
                    Elements allSpans = element.select("span, div");
                    for (Element span : allSpans) {
                        String text = span.text().toLowerCase();
                        if (text.contains("москва") || text.contains("санкт-петербург") || 
                            text.contains("новосибирск") || text.contains("екатеринбург") ||
                            text.contains("казань") || text.contains("нижний новгород")) {
                            cityElement = span;
                            break;
                        }
                    }
                }
                if (cityElement != null) {
                    String cityText = cityElement.text().trim();
                    if (!cityText.isEmpty()) {
                        vacancy.setCity(cityText.split(",")[0].split("•")[0].trim());
                    }
                }
                
                vacancy.setSource("superjob");
                vacancy.setPublishedDate(LocalDateTime.now());
                
                if (vacancy.getTitle() != null && !vacancy.getTitle().isEmpty()) {
                    if (vacancy.getCity() == null || vacancy.getCity().isEmpty()) {
                        vacancy.setCity("Не указан");
                    }
                    if (vacancy.getCompany() == null || vacancy.getCompany().isEmpty()) {
                        vacancy.setCompany("Не указана");
                    }
                    vacancies.add(vacancy);
                    processedCount++;
                } else {
                    log.debug("Skipped SuperJob element - no title found. Element preview: {}", 
                        element.html().substring(0, Math.min(200, element.html().length())));
                }
            }
            log.info("Successfully processed {} out of {} elements on SuperJob", processedCount, vacancyElements.size());
            log.info("Parsed {} vacancies from SuperJob", vacancies.size());
        } catch (IOException e) {
            log.error("Error parsing SuperJob: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error parsing SuperJob: {}", e.getMessage(), e);
        }
        return vacancies;
    }

    public List<Vacancy> parseHabrCareer(String url) {
        List<Vacancy> vacancies = new ArrayList<>();
        try {
            log.info("Parsing Habr Career: {}", url);
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .referrer("https://career.habr.com")
                    .followRedirects(true)
                    .get();

            Elements vacancyElements = doc.select("div.job-card");
            if (vacancyElements.isEmpty()) {
                vacancyElements = doc.select("div[class*='job']");
            }
            if (vacancyElements.isEmpty()) {
                vacancyElements = doc.select("div.vacancy-card");
            }
            
            log.info("Found {} vacancy elements on Habr Career", vacancyElements.size());
            
            int processedCount = 0;
            for (Element element : vacancyElements) {
                Vacancy vacancy = new Vacancy();
                
                Element titleElement = element.selectFirst("a.job-card__title");
                if (titleElement == null) {
                    titleElement = element.selectFirst("a[class*='title']");
                }
                if (titleElement == null) {
                    titleElement = element.selectFirst("h3 a, h2 a, h4 a");
                }
                if (titleElement == null) {
                    titleElement = element.selectFirst("a[href*='/vacancies/']");
                }
                if (titleElement != null) {
                    String title = titleElement.text().trim();
                    if (!title.isEmpty()) {
                        vacancy.setTitle(title);
                        String href = titleElement.attr("href");
                        if (!href.startsWith("http")) {
                            href = "https://career.habr.com" + href;
                        }
                        vacancy.setSourceUrl(href);
                    }
                }
                
                Element companyElement = element.selectFirst("div.job-card__company-name");
                if (companyElement == null) {
                    companyElement = element.selectFirst("div[class*='company']");
                }
                if (companyElement == null) {
                    companyElement = element.selectFirst("span[class*='company']");
                }
                if (companyElement != null) {
                    vacancy.setCompany(companyElement.text().trim());
                }
                
                Element salaryElement = element.selectFirst("div.job-card__salary");
                if (salaryElement == null) {
                    salaryElement = element.selectFirst("div[class*='salary']");
                }
                if (salaryElement == null) {
                    salaryElement = element.selectFirst("span[class*='salary']");
                }
                if (salaryElement != null) {
                    vacancy.setSalary(salaryElement.text().trim());
                }
                
                Element cityElement = element.selectFirst("div.job-card__meta-item");
                if (cityElement == null) {
                    cityElement = element.selectFirst("div[class*='meta']");
                }
                if (cityElement == null) {
                    Elements metaItems = element.select("div[class*='meta'], span[class*='meta']");
                    if (!metaItems.isEmpty()) {
                        cityElement = metaItems.first();
                    }
                }
                if (cityElement != null) {
                    String cityText = cityElement.text().trim();
                    if (!cityText.isEmpty()) {
                        vacancy.setCity(cityText.split(",")[0].split("•")[0].trim());
                    }
                }
                
                vacancy.setSource("habr");
                vacancy.setPublishedDate(LocalDateTime.now());
                
                if (vacancy.getTitle() != null && !vacancy.getTitle().isEmpty()) {
                    if (vacancy.getCity() == null || vacancy.getCity().isEmpty()) {
                        vacancy.setCity("Не указан");
                    }
                    if (vacancy.getCompany() == null || vacancy.getCompany().isEmpty()) {
                        vacancy.setCompany("Не указана");
                    }
                    vacancies.add(vacancy);
                    processedCount++;
                } else {
                    log.debug("Skipped Habr element - no title found");
                }
            }
            log.info("Successfully processed {} out of {} elements on Habr Career", processedCount, vacancyElements.size());
            log.info("Parsed {} vacancies from Habr Career", vacancies.size());
        } catch (IOException e) {
            log.error("Error parsing Habr Career: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error parsing Habr Career: {}", e.getMessage(), e);
        }
        return vacancies;
    }

    private LocalDateTime parseDate(String dateText) {
        try {
            if (dateText.contains("сегодня")) {
                return LocalDateTime.now();
            } else if (dateText.contains("вчера")) {
                return LocalDateTime.now().minusDays(1);
            } else {
                Pattern pattern = Pattern.compile("(\\d{1,2})\\s+(января|февраля|марта|апреля|мая|июня|июля|августа|сентября|октября|ноября|декабря)");
                Matcher matcher = pattern.matcher(dateText);
                if (matcher.find()) {
                    return LocalDateTime.now();
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return LocalDateTime.now();
    }

    public String detectSource(String url) {
        if (url.contains("hh.ru") || url.contains("hh.")) {
            return "hh";
        } else if (url.contains("superjob.ru") || url.contains("superjob.")) {
            return "superjob";
        } else if (url.contains("habr.com") || url.contains("career.habr")) {
            return "habr";
        }
        return "unknown";
    }
}
