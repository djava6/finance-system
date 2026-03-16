# Runbook — Finance System

## Visão geral

Serviço: `finance-system`
Plataforma: GCP Cloud Run
Banco: Cloud SQL PostgreSQL 16
Logs: GCP Cloud Logging (projeto `finance-system`)
Alertas: Cloud Monitoring

## Severidade de incidentes

| Nível | Critério | SLA de resposta |
|---|---|---|
| P1 | Serviço completamente indisponível | 15 min |
| P2 | Funcionalidade crítica degradada (auth/transações) | 1 hora |
| P3 | Funcionalidade secundária com falha | 4 horas |
| P4 | Bug não crítico / melhoria | Próximo sprint |

---

## Alertas e respostas

### ALERTA: Taxa de erro > 5% (5xx)

**Sintoma**: Cloud Monitoring dispara `high-error-rate`

**Diagnóstico**:
```bash
# Ver últimos erros no Cloud Logging
gcloud logging read 'resource.type="cloud_run_revision" severity>=ERROR' \
  --project=$PROJECT_ID --limit=50 --format=json

# Ver revisão atual
gcloud run revisions list --service=finance-system --region=$REGION
```

**Resposta**:
1. Verificar logs para stack traces
2. Se erro de banco: checar Cloud SQL (`gcloud sql instances describe finance-db`)
3. Se erro de código: fazer rollback para revisão anterior:
```bash
gcloud run services update-traffic finance-system \
  --to-revisions=REVISAO_ANTERIOR=100 --region=$REGION
```

---

### ALERTA: Latência P99 > 2s

**Sintoma**: Cloud Monitoring dispara `high-latency`

**Diagnóstico**:
```bash
# Verificar queries lentas no PostgreSQL
gcloud sql connect finance-db --user=postgres
SELECT query, mean_exec_time, calls FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;
```

**Resposta**:
1. Verificar se índices estão presentes (`transacoes.usuario_id`, `transacoes.data`)
2. Verificar CPU/memória da instância Cloud SQL
3. Considerar aumentar `min-instances` no Cloud Run para reduzir cold starts:
```bash
gcloud run services update finance-system --min-instances=1 --region=$REGION
```

---

### ALERTA: Conexões de banco esgotadas

**Sintoma**: `HikariPool - Connection is not available`, `too many clients`

**Diagnóstico**:
```bash
# Ver conexões ativas
gcloud sql connect finance-db --user=postgres
SELECT count(*) FROM pg_stat_activity;
SELECT client_addr, state, count(*) FROM pg_stat_activity GROUP BY client_addr, state;
```

**Resposta**:
1. Reduzir `spring.datasource.hikari.maximum-pool-size` via Secret Manager e reimplantar
2. Ou aumentar `max_connections` no Cloud SQL (requer reinicialização da instância)
3. Em emergência, matar conexões ociosas:
```sql
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle' AND query_start < now() - interval '10 minutes';
```

---

### ALERTA: Uso de memória > 80%

**Sintoma**: Cloud Monitoring dispara `high-memory`

**Resposta**:
1. Verificar se há leak de memória nos logs
2. Aumentar memória do Cloud Run:
```bash
gcloud run services update finance-system --memory=1Gi --region=$REGION
```

---

## Rollback de deploy

```bash
# Listar revisões
gcloud run revisions list --service=finance-system --region=$REGION

# Redirecionar tráfego para revisão anterior
gcloud run services update-traffic finance-system \
  --to-revisions=finance-system-XXXXXXX=100 \
  --region=$REGION
```

## Rollback de migração Flyway

Flyway não suporta rollback automático. Para reverter:

1. Criar uma nova migração `V7__rollback_v6.sql` com o SQL inverso
2. Nunca editar migrations já aplicadas em produção

## Rotação de secrets

Secrets são armazenados no Secret Manager. Para rotacionar:

```bash
# Criar nova versão do secret
echo -n "novo-valor" | gcloud secrets versions add finance-jwt-secret --data-file=-

# Reimplantar para carregar novo valor
gcloud run services update finance-system --region=$REGION --no-traffic
gcloud run services update-traffic finance-system --to-latest
```

O workflow `.github/workflows/rotate-secrets.yml` faz isso automaticamente de forma agendada.

## Verificação de saúde

```bash
# Health check do Cloud Run
curl https://finance-system-xxxx-uc.a.run.app/actuator/health

# Swagger UI
curl https://finance-system-xxxx-uc.a.run.app/swagger-ui/index.html
```
