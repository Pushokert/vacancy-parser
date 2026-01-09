# Парсер вакансий

Многопоточное веб-приложение для автоматизированного сбора, обработки и хранения данных о вакансиях с сайтов hh.ru, SuperJob и Habr Career.

## Требования

- Java 17+
- Maven 3.6+

## Запуск

### 1. Запуск приложения

```bash
mvn spring-boot:run
```

Или через IDE: запустить класс `VacancyParserApplication`.

Приложение доступно на `http://localhost:8080`.

### 2. Запуск парсинга

**Через HTML интерфейс:**
- Открыть `запустить_парсинг.html` в браузере
- Нажать кнопку "Запустить парсинг"

**Через API:**
```bash
curl -X POST http://localhost:8080/api/vacancies/parse \
  -H "Content-Type: application/json" \
  -d '{
    "urls": [
      "https://hh.ru/search/vacancy?text=java&area=1",
      "https://www.superjob.ru/vacancy/search/?keywords=java",
      "https://career.habr.com/vacancies?q=java"
    ],
    "maxPages": 5
  }'
```

### 3. Получение результатов

**Через HTML интерфейс:**
- Открыть `просмотр_вакансий.html` в браузере

**Через API:**
```bash
curl http://localhost:8080/api/vacancies/answer?size=1000
```

## API

### Эндпоинты

**Запуск парсинга:**
```
POST http://localhost:8080/api/vacancies/parse
Content-Type: application/json

{
  "urls": ["https://hh.ru/search/vacancy?text=java&area=1", ...],
  "maxPages": 5
}
```

**Получение результатов:**
```
GET http://localhost:8080/api/vacancies/answer
```

**Параметры запроса:**
- `sortBy` - сортировка (date, title, company, city)
- `order` - порядок (asc, desc)
- `source` - фильтр по источнику (hh, superjob, habr)
- `city` - фильтр по городу
- `company` - фильтр по компании
- `page` - номер страницы (default: 0)
- `size` - размер страницы (default: 1000)

**Дополнительные эндпоинты:**
```
GET /api/vacancies/source/{source}
GET /api/vacancies/city/{city}
```

## Конфигурация

Настройки в `application.properties`:

```properties
server.port=8080
spring.datasource.url=jdbc:h2:mem:vacancydb
parser.thread.pool.size=10
parser.schedule.initial.delay=5000
parser.schedule.fixed.delay=300000
```

Автоматический парсинг запускается каждые 5 минут после старта приложения.

## База данных

H2 Console: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:vacancydb`
- User: `sa`
- Password: (пусто)
