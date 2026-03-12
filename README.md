# Daily POD Status Automation

Production-grade web application for creating, tracking, versioning, and exporting Daily POD Status Reports.

## Stack

- Backend: Java 21, Spring Boot, PostgreSQL, JPA/Hibernate, JWT, RBAC, Apache POI, OpenHTMLToPDF (PDFBox)
- Frontend: React + TypeScript, TailwindCSS, ShadCN-style component layer
- Deployment: Docker, docker-compose, GitHub Actions CI

## Project Structure

- `backend/` Spring Boot REST API and document generation
- `frontend/` React UI
- `docker-compose.yml` local full-stack runtime

## Key API Endpoints

- `POST /api/reports`
- `GET /api/reports`
- `GET /api/reports/{id}`
- `PUT /api/reports/{id}`
- `DELETE /api/reports/{id}`
- `POST /api/reports/{id}/generate-docx`
- `POST /api/reports/{id}/generate-pdf`
- `GET /api/reports/{id}/printable-html`

## Auth and Roles

- `SCRUM_MASTER`: create, edit, delete, generate exports
- `TEAM_MEMBER`: update reports, update table sections
- `MANAGER`: view reports, export DOCX/PDF

## Exact Template Requirement

To preserve exact structure/formatting, place your official source template DOCX in:

- `backend/src/main/resources/templates/Daily_Status_Template_1.docx`

The generator performs placeholder replacement in the template and keeps its layout intact.

Expected placeholders include:

- `{{pod_name}}`
- `{{date}}`
- `{{sprint_number}}`
- `{{day_of_sprint}}`
- `{{goal}}`
- `{{prepared_by}}`
- `{{distribution}}`
- `{{warning_text}}`
- `{{legend}}`

## Run Locally (Docker)

```bash
docker compose up --build
```

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080/api`
- PostgreSQL: `localhost:5432`

## Run Without Docker

### Backend

```bash
cd backend
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## Production Notes

- Replace `JWT_SECRET` with a strong secret from secure config management
- Add AWS ALB/Nginx TLS termination for EC2 deployment
- Configure SMTP/Slack webhook values for share endpoints
- Use Flyway/Liquibase for controlled DB migrations

## Tests

Backend unit test is included for analytics warning behavior.
