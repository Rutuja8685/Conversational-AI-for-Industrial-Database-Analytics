# Conversational AI for Industrial Database Analytics

A full-stack Spring Boot application that translates natural-language questions into optimized MySQL queries and presents the results through an analytics workflow.

## Why this project

Database users should be able to explore operational data without hand-writing every query. This project focuses on making that interaction useful while keeping execution controlled and auditable.

## Core capabilities

- Natural-language database questions
- SQL generation and optimization workflow
- Dynamic schema discovery
- Execution guardrails for safer database access
- Role-based access control
- Multi-session chat history
- Dashboard-oriented analytics

## Technology

- Java 17
- Spring Boot 3.2.3
- Spring Web and Spring Data JPA
- MySQL for persistent data
- H2 for runtime/test support
- Maven and Docker
- HTML, CSS, and JavaScript for the interface

## Project structure

- src/main — application code and resources
- src/test — automated test sources
- Database_Integration — database integration assets
- Dockerfile — container build configuration
- pom.xml — Maven dependencies and build configuration

## Run locally

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL, or the configured H2 profile
- Docker (optional)

### Start the application

1. Clone the repository.
2. Configure the database connection in the application's configuration and keep credentials in environment variables or local, ignored files.
3. Start the service:

~~~bash
mvn spring-boot:run
~~~

### Run tests

~~~bash
mvn test
~~~

## Engineering focus

This project demonstrates backend API design, persistence, database-aware application logic, authentication boundaries, and deployment preparation in a single full-stack system.

> Portfolio project. Do not use real production credentials or sensitive industrial data in local development.
