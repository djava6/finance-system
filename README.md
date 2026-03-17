# Finance System

Sistema de controle financeiro pessoal com backend Spring Boot e frontend Flutter.

## Tecnologias

- **Backend**: Java 21, Spring Boot 3.3.6, Spring Security 6, Firebase Admin SDK, Flyway, PostgreSQL
- **Frontend**: Flutter (Dart), Firebase Auth, fl_chart, provider
- **Infra**: Docker, GitHub Actions CI/CD, GCP Cloud Run, Secret Manager, Cloud Logging

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- Flutter 3.19+
- PostgreSQL 16 (ou Docker)

## Configuração local

### 1. Variáveis de ambiente

Crie um arquivo `.env` na raiz (nunca versionar):

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/finance
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
```

O backend usa Firebase Admin SDK com Application Default Credentials (ADC). Para desenvolvimento local, autentique-se com:

```bash
gcloud auth application-default login
```

### 2. Subir banco com Docker

```bash
docker compose up db -d
```

### 3. Rodar o backend

```bash
mvn spring-boot:run
```

O servidor inicia em `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### 4. Rodar testes

```bash
mvn test
```

Os testes de integração sobem um PostgreSQL real via Testcontainers (requer Docker rodando).

### 5. Rodar o frontend Flutter

```bash
cd frontend
flutter pub get
flutter run
```

## Rodar tudo com Docker Compose

```bash
docker compose up --build
```

## Variáveis de ambiente

| Variável | Descrição | Obrigatório |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL | Sim |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco | Sim |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco | Sim |
| `SPRING_PROFILES_ACTIVE` | `prod` para produção | Não |

Autenticação usa Firebase Admin SDK com ADC — sem secrets adicionais.

## Endpoints principais

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/transactions` | Listar transações (filtro: `?inicio=yyyy-MM-dd&fim=yyyy-MM-dd`) |
| POST | `/transactions` | Criar transação |
| PUT | `/transactions/{id}` | Editar transação |
| DELETE | `/transactions/{id}` | Excluir transação |
| GET | `/transactions/export/csv` | Exportar CSV |
| GET | `/dashboard` | Resumo financeiro + evolução mensal |
| GET/POST/PUT/DELETE | `/categories` | CRUD categorias |
| GET/POST/PUT/DELETE | `/contas` | CRUD contas bancárias |
| GET/PUT | `/users/me` | Perfil do usuário |

## Deploy (GCP Cloud Run)

O deploy é automatizado via GitHub Actions (`ci.yml`). Push na branch `main` dispara build, push para Artifact Registry e deploy no Cloud Run.

Secrets necessários no GitHub:
- `GCP_CREDENTIALS` — JSON da service account
- `GCP_PROJECT_ID`
- `GCP_REGION`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

## Estrutura do projeto

```
finance-system/
├── src/
│   ├── main/java/br/com/useinet/finance/
│   │   ├── config/          # SecurityConfig, FirebaseConfig, OpenApiConfig, GlobalExceptionHandler
│   │   ├── controller/      # TransacaoController, DashboardController...
│   │   ├── dto/             # Request/Response DTOs
│   │   ├── model/           # Entidades JPA
│   │   ├── repository/      # Spring Data JPA repositories
│   │   ├── security/        # FirebaseAuthenticationFilter
│   │   └── service/         # Regras de negócio (incl. FirebaseUserService)
│   └── resources/
│       ├── db/migration/    # Scripts Flyway V1–V11
│       ├── application.properties
│       ├── application-prod.properties
│       └── logback-spring.xml
├── frontend/                # Flutter app
├── monitoring/              # dashboard.json, alerts.json
├── Dockerfile
├── docker-compose.yml
└── .github/workflows/       # CI/CD pipelines
```
