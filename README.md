# КосмоСкан 🛸

Информационная система для приёма и автоматической технической проверки студенческих работ.

---

## Старт

```bash

docker compose up --build
```


После запуска доступны:

| Адрес | Описание |
|---|---|
| http://localhost:8000/swagger-ui.html | **API Gateway — главный Swagger** |
| http://localhost:8001/swagger-ui.html | File Storing Service Swagger |
| http://localhost:8002/swagger-ui.html | File Analysis Service Swagger |

---

## Архитектура системы

```
┌──────────────────────────────────────────────────────┐
│                   Клиент (браузер/Postman)           │
└───────────────────────┬──────────────────────────────┘
                        │ HTTP
                        ▼
        ┌───────────────────────────┐
        │      API Gateway          │
        │      :8000                │
        │  Принимает ВСЕ запросы,   │
        │  маршрутизирует их        │
        └──────┬──────────┬─────────┘
               │          │
         ┌─────▼──┐  ┌────▼────────┐
         │Storing │  │ Analysis    │
         │Service │  │ Service     │
         │:8001   │  │ :8002       │
         └──┬─────┘  └────┬────────┘
            │              │
       ┌────▼───┐    ┌─────▼────┐
       │Postgres│    │ Postgres  │
       │storing │    │ analysis  │
       │_db     │    │ _db       │
       └────────┘    └──────────┘
```

### Микросервисы

| Сервис | Порт | Ответственность | База данных |
|---|---|---|---|
| API Gateway | 8000 | Единая точка входа, маршрутизация, обработка ошибок | — |
| File Storing Service | 8001 | Приём файлов, хранение на диске, метаданные в БД | storing_db |
| File Analysis Service | 8002 | Проверка файлов, генерация отчётов и облака слов | analysis_db |

---

## Сценарии взаимодействия

### Сценарий 1: Студент сдаёт работу (успешный путь)

```
POST /works  { student_name="Иванов", file=work.pdf }
       │
       ├──► POST http://file-storing-service:8001/files
       │        Проверяет формат (pdf ✓) и размер (400 КБ ✓)
       │        Сохраняет файл на диск: /app/uploads/20250101_120000_work.pdf
       │        Записывает в БД: works (id=1, studentName="Иванов", ...)
       │        ◄── { id: 1, studentName: "Иванов", fileFormat: "pdf", ... }
       │
       └──► POST http://file-analysis-service:8002/analyze/1
                GET /files/1 → метаданные работы
                GET /files/1/content → байты файла
                Проверяет: формат "pdf" ∈ {pdf,docx,txt} → OK
                Проверяет: размер 409600 ≤ 1048576 → OK
                Извлекает текст через PDFBox
                Генерирует WordCloud → /app/reports/wordcloud_1.png
                Записывает в БД: reports (status="accepted", issues=[])
                ◄── { status: "accepted", formatValid: "ok", sizeValid: "ok" }

Ответ клиенту:
{
  "work": { "id": 1, "studentName": "Иванов", "fileFormat": "pdf", ... },
  "report": { "status": "accepted", "hasWordcloud": true, "issues": [] },
  "message": "Работа успешно принята. Статус проверки: accepted"
}
```

### Сценарий 2: Работа с ошибками (неверный формат)

```
POST /works  { student_name="Петров", file=work.zip }
       │
       └──► POST http://file-storing-service:8001/files
                ext = "zip" ∉ {pdf, docx, txt}
                ◄── 400 Bad Request "Недопустимый формат 'zip'"

Ответ клиенту: 400 Bad Request
```

### Сценарий 3: Преподаватель запрашивает отчёт

```
GET /works/1/reports
       │
       └──► GET http://file-analysis-service:8002/reports/1
                SELECT * FROM reports WHERE work_id = 1
                ◄── { status, checks, issues, hasWordcloud, ... }

Ответ клиенту: JSON с полным отчётом
```

### Сценарий 4: File Analysis Service недоступен (обработка ошибки)

```
POST /works  { student_name="Козлов", file=work.txt }
       │
       ├──► POST http://file-storing-service:8001/files → 200 OK (work_id=5)
       │
       └──► POST http://file-analysis-service:8002/analyze/5
                ResourceAccessException (соединение отклонено)
                Gateway перехватывает исключение — НЕ падает!

Ответ клиенту: 200 OK
{
  "work": { "id": 5, ... },
  "report": null,
  "warning": "Файл сохранён, анализ недоступен. GET /works/5/reports"
}
```

---

## API Reference

| Метод | URL | Описание |
|---|---|---|
| POST | /works | Сдать работу (студент) |
| GET | /works | Список всех работ |
| GET | /works/{id} | Метаданные работы |
| GET | /works/{id}/reports | Отчёт о проверке (преподаватель) |
| GET | /works/{id}/wordcloud | Облако слов PNG |
| GET | /reports | Все отчёты |
| GET | /health | Статус всех сервисов |

---

## Модели данных

### Таблица `works` (storing_db)

| Поле | Тип | Описание |
|---|---|---|
| id | BIGINT PK | Идентификатор |
| student_name | VARCHAR(255) | ФИО студента |
| original_filename | VARCHAR(255) | Исходное имя файла |
| stored_filename | VARCHAR(255) | Имя на диске (с timestamp) |
| file_path | VARCHAR(500) | Путь к файлу |
| file_size | BIGINT | Размер в байтах |
| file_format | VARCHAR(10) | pdf / docx / txt |
| uploaded_at | TIMESTAMPTZ | Время загрузки |

### Таблица `reports` (analysis_db)

| Поле | Тип | Описание |
|---|---|---|
| id | BIGINT PK | Идентификатор |
| work_id | BIGINT UNIQUE | ID работы из storing |
| student_name | VARCHAR(255) | ФИО студента |
| filename | VARCHAR(255) | Имя файла |
| status | VARCHAR(50) | accepted / needs_revision |
| file_format | VARCHAR(10) | Формат файла |
| file_size | BIGINT | Размер в байтах |
| format_valid | VARCHAR(10) | ok / error |
| size_valid | VARCHAR(10) | ok / error |
| issues | TEXT (JSON) | Список замечаний |
| wordcloud_path | VARCHAR(500) | Путь к PNG |
| created_at | TIMESTAMPTZ | Время создания |

---

## Технологии

| Технология | Версия | Назначение |
|---|--------|---|
| Java | 17     | Язык программирования |
| Spring Boot | 3.2.0  | Веб-фреймворк |
| Spring Data JPA | 3.2.0  | ORM для работы с БД |
| PostgreSQL | 15     | База данных |
| Apache PDFBox | 3.0.1  | Извлечение текста из PDF |
| Apache POI | 5.2.5  | Чтение DOCX файлов |
| Kumo | 1.28   | Генерация облака слов |
| springdoc-openapi | 2.3.0  | Swagger документация |
| JUnit 5 | 5.x    | Тестирование |
| JaCoCo | 0.8.11 | Покрытие кода тестами |
| Docker | 24+    | Контейнеризация |
| docker-compose | v2     | Оркестрация контейнеров |

---

## Тесты и покрытие

```bash
# File Storing Service
cd file-storing-service
mvn test
# Отчёт: target/site/jacoco/index.html

# File Analysis Service
cd file-analysis-service
mvn test
# Отчёт: target/site/jacoco/index.html

# API Gateway
cd api-gateway
mvn test
# Отчёт: target/site/jacoco/index.html
```

<img width="1101" height="229" alt="image" src="https://github.com/user-attachments/assets/124a005c-1341-47fb-b947-11346239fa57" />


---

## Скринкаст

[Ссылка на YouTube]
