# kbd-pm-system-backend

Spring Boot 3 + Java 21 + Spring Data JPA backend for `kbd_pm_system`.

## Run

```bash
cd backend
mvn spring-boot:run
```

## Database configuration

Defaults are in `src/main/resources/application.yml` and can be overridden via env vars:

- `DB_HOST` (default `localhost`)
- `DB_PORT` (default `3306`)
- `DB_NAME` (default `kbd_pm_system`)
- `DB_USER` (default `root`)
- `DB_PASSWORD` (default `root`)

## JPA mapping notes

- `spring.jpa.hibernate.ddl-auto=validate` expects your MySQL schema to already exist (matches `db/ddl_mysql8.sql`).
- If your database was created from an older DDL copy **without** `project.process_oversight_dept_id`, apply:
  - `../db/alter_project_add_process_oversight_dept.sql`
- MySQL **generated columns** are mapped as read-only fields:
  - `project_budget_plan.total_amount`
  - `project_budget_snapshot.total_spent`
  - `project_budget_snapshot.utilization_ratio`

## API (scaffold)

- `POST /api/projects` — 创建项目（立项），自动初始化 G0–G9 `project_milestone` 行，并写入默认预算阈值策略
- `GET /api/projects/{id}` — 项目详情（DTO，不直接暴露表结构）
