# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`maregistry` is a Spring Boot 4.0.3 service under the `com.malinghan.marpc` group, intended to serve as a registry component in the `marpc` RPC framework. It is currently a skeleton with no business logic implemented yet.

- Java 17
- Spring Boot 4.0.3 (spring-boot-starter-webmvc)
- Lombok for boilerplate reduction
- Maven build system

## Common Commands

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=MaregistryApplicationTests

# Run a single test method
mvn test -Dtest=MaregistryApplicationTests#contextLoads
```

## Architecture

The project lives under `src/main/java/com/malinghan/marpc/maregistry/`. Entry point is `MaregistryApplication.java`. Configuration is in `src/main/resources/application.properties` (currently only sets `spring.application.name=maregistry`).

As a registry service in the `marpc` ecosystem, it is expected to handle service registration and discovery for RPC providers and consumers.