# Repository Guidelines

## Project Structure & Module Organization

This repository is a Spring Boot service for local CAE job orchestration. Main code lives in `src/main/java/com/local/caejobservice/`:

- `adapter/`: CAE adapter interfaces and implementations such as `AnsaAdapter`
- `job/`: HTTP API, application services, persistence, process execution, and WebSocket log streaming
- `common/`: shared configuration, exception handling, and utility classes

Configuration and static assets live in `src/main/resources/`, including `application.yml` and `static/index.html`. Runtime artifacts are written to `data/` and `workspaces/` and should remain untracked.

## Build, Test, and Development Commands

- `mvn spring-boot:run`: start the service locally on `127.0.0.1:8765`
- `mvn clean package`: compile and build the runnable jar in `target/`
- `mvn test`: run the test suite
- `git status --short`: review local changes before committing

Use `dummy_solver` jobs for local verification before testing against a real ANSA installation.

## Coding Style & Naming Conventions

Use 4-space indentation and standard Java style. Keep classes small and focused. Prefer constructor injection, `Path` over raw file strings where practical, and explicit validation for request and adapter inputs.

Format Java code with `google-java-format` before submitting changes. Example:

- `google-java-format -i src/main/java/com/local/caejobservice/job/api/JobController.java`

- Packages: lowercase, grouped by feature, e.g. `job.application`
- Classes: `PascalCase`
- Methods and fields: `camelCase`
- Constants: `UPPER_SNAKE_CASE`

There is no build-integrated formatter yet, so run `google-java-format` manually and keep imports tidy.

## Testing Guidelines

Use JUnit via `spring-boot-starter-test`. Add or update tests when changing API behavior, job lifecycle handling, persistence logic, or adapter command construction. Name test classes after the target type, for example `JobControllerTest` or `AnsaAdapterTest`. Run `mvn test` before opening a PR.

## Commit & Pull Request Guidelines

There is no commit history yet, so use concise Conventional Commit messages such as `feat: add ANSA path fallback` or `fix: handle cancelled jobs before process start`.

PRs should include:

- a short summary of the change
- any config or API impact
- linked issues when available
- screenshots only for frontend changes

## Security & Configuration Tips

Do not commit machine-specific ANSA paths, database files, generated logs, or workspace outputs. Keep local path overrides in `src/main/resources/application.yml` for development only, and review CORS/WebSocket origins before exposing the service beyond localhost.
