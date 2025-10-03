## Development Workflow

Recommended workflow to contribute safely and efficiently.

### Branching
- Create feature branches from `main`
- Keep PRs focused and small

### Build & Run
- Use `docker-compose.yml` and scripts for local env
- Backend Java services build with Maven: `mvn -q -DskipTests package`
- Frontend: `npm start` with hot reload

### Testing
- Unit tests in each module
- Validate Avro schema compatibility when changing contracts

### Code Quality
- Follow the style of existing code
- Add logs and metrics for new operators or components

### PR Checklist
- Docs updated (especially schema and runbook changes)
- Observability added (metrics/logs/dashboards) where relevant
- Backward compatibility considered for data contracts


