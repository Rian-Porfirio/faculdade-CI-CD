# 🛸 UfoTracker — Ufology Investigation Unit

Projeto desenvolvido para a disciplina de DevOps, com o objetivo de dockerizar e implantar uma aplicação Spring Boot no Kubernetes, integrando PostgreSQL e Redis como serviços de suporte.

---

## 📁 Estrutura do Repositório

```
devops2026-ufoTracker/
├── src/                        # Código-fonte da aplicação Spring Boot
├── .mvn/                       # Wrapper do Maven
├── .github/
│   └── workflows/              # Pipelines GitHub Actions
│       ├── hello.yml
│       ├── tests.yml
│       ├── gradle-ci.yml
│       ├── env-demo.yml
│       └── secret-demo.yml
├── Dockerfile                  # Dockerização da aplicação
├── pom.xml
└── README.md
```

> ⚙️ Os manifestos Kubernetes estão na branch **`k8s`**, organizados por componente:
> ```
> k8s/
> ├── kustomization.yaml
> ├── postgres/
> │   ├── 00-namespace.yaml
> │   ├── 01-secret.yaml
> │   ├── 02-deployment.yaml
> │   └── 03-service.yaml
> ├── redis/
> │   ├── 04-redis-deployment.yaml
> │   └── 05-redis-service.yaml
> └── app/
>     ├── 06-configmap.yaml
>     ├── 07-secret.yaml
>     ├── 08-deployment.yaml
>     ├── 09-service.yaml
>     └── 10-pdb.yaml
> ```

---

## 🚀 Missão 1 — Banco de Dados PostgreSQL

Provisionamento do banco de dados oficial da operação dentro do cluster Kubernetes.

**Recursos criados:**
- Namespace `ufology`
- Secret com credenciais do banco (`ufodb-secret`)
- Deployment com 1 réplica usando a imagem `leogloriainfnet/ufodb:1.0-win`
- Service ClusterIP expondo a porta `5432`

**Variáveis de ambiente configuradas:**
| Variável | Valor |
|---|---|
| `POSTGRES_USER` | `postgres` |
| `POSTGRES_PASSWORD` | `devops2025!` |
| `POSTGRES_DB` | `ufology` |

---

## ⚡ Missão 2 — Cache Redis

Camada de cache em memória para reduzir consultas repetidas ao banco.

**Recursos criados:**
- Deployment com 1 réplica usando a imagem `redis:7-alpine`
- Service ClusterIP expondo a porta `6379`

---

## 🐳 Missão 3 — Dockerização da Aplicação

Transformação da aplicação Spring Boot em uma imagem Docker pronta para uso em containers.

### Dockerfile

O Dockerfile utiliza **multi-stage build** para manter a imagem final enxuta:

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Comandos

**Build da imagem:**
```bash
docker build -t rianporfirio/ufo-tracker:1.0 .
```

**Push para o Docker Hub:**
```bash
docker push rianporfirio/ufo-tracker:1.0
```

**Imagem pública:** `https://hub.docker.com/r/rianporfirio/ufo-tracker`

---

## ☸️ Missão 4 — Implantação no Cluster Kubernetes

Deploy da aplicação no cluster conectando-a ao PostgreSQL e ao Redis já provisionados.

**Recursos criados:**
- ConfigMap `app-config` com o nome do banco (`DB_NAME=ufology`)
- Secret `db-secret` com a senha do banco (`DB_PASSWORD`)
- Deployment com **2 réplicas** da aplicação
- Service ClusterIP expondo a porta `8080`
- PodDisruptionBudget garantindo mínimo de 1 réplica disponível

**Variáveis de ambiente injetadas no container:**
| Variável | Origem | Valor |
|---|---|---|
| `SPRING_DATA_REDIS_HOST` | inline | `ufocache-service` |
| `SPRING_DATA_REDIS_PORT` | inline | `6379` |
| `DB_NAME` | ConfigMap `app-config` | `ufology` |
| `SPRING_DATASOURCE_URL` | inline (interpolado) | `jdbc:postgresql://ufodb-service:5432/$(DB_NAME)` |
| `POSTGRES_USERNAME` | inline | `postgres` |
| `POSTGRES_PASSWORD` | Secret `db-secret` | `devops2025!` |

### Como aplicar no cluster

```bash
# Certifique-se de estar na branch k8s
git checkout k8s

# Aplicar todos os recursos
kubectl apply -k .

# Verificar o estado
kubectl get all -n ufology
```

---

## ⚙️ GitHub Actions — Workflows

Os workflows estão em `.github/workflows/` e automatizam o ciclo de vida da aplicação.

### Parte 2 — Workflows Básicos

| Arquivo | Gatilho | Descrição |
|---|---|---|
| `hello.yml` | qualquer `push` | Exibe "Hello CI/CD" no log |
| `tests.yml` | `pull_request` | Checkout + echo "Rodando testes" |
| `gradle-ci.yml` | `push` na branch `AT` | Checkout + Java 21 + `./mvnw package` |

### Parte 3 — Variáveis e Segredos

| Arquivo | Gatilho | Descrição |
|---|---|---|
| `env-demo.yml` | qualquer `push` | Exibe variável `DEPLOY_ENV=staging` no log |
| `secret-demo.yml` | qualquer `push` | Verifica o secret `API_KEY` e exibe "API_KEY configurado" |

> 🔐 O secret `API_KEY` foi cadastrado em **Settings → Secrets and variables → Actions** no repositório antes de executar o workflow.

---

## 🖥️ Runners: GitHub-hosted vs Self-hosted

### Runners hospedados pelo GitHub (GitHub-hosted)

São máquinas virtuais gerenciadas pela própria plataforma, disponibilizadas automaticamente para cada execução de workflow. Cada job recebe um ambiente completamente limpo e isolado.

**Vantagens:**
- Zero configuração — prontos para uso imediato
- Sempre atualizados e mantidos pelo GitHub
- Isolamento total entre execuções
- Integração nativa com o ecossistema GitHub

**Desvantagens:**
- Tempo máximo de execução de 6 horas por job
- Hardware fixo, sem customização de recursos
- Sem acesso direto a redes privadas ou serviços internos
- Consumo de minutos pagos em repositórios privados além da cota gratuita

---

### Runners auto-hospedados (Self-hosted)

São máquinas próprias — físicas, VMs ou containers — registradas no repositório para executar os workflows no ambiente controlado pela equipe.

**Vantagens:**
- Acesso total a recursos internos (bancos de dados, VPNs, registries privados)
- Hardware e sistema operacional totalmente customizáveis
- Sem limite de tempo de execução
- Sem custo adicional de minutos de execução

**Desvantagens:**
- Exige instalação, manutenção e atualização contínua do runner
- Risco de segurança se exposto em repositórios públicos
- A equipe assume total responsabilidade pelo ambiente e disponibilidade

---

## 🛠️ Tecnologias Utilizadas

- **Java 21** + **Spring Boot 3.5.4**
- **PostgreSQL** — banco de dados relacional
- **Redis** — cache em memória
- **Docker** — containerização
- **Kubernetes** (Minikube) — orquestração de containers
- **Kustomize** — gerenciamento de manifestos Kubernetes
- **GitHub Actions** — CI/CD
