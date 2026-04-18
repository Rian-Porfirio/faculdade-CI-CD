# ☸️ UfoTracker — Infraestrutura Kubernetes

Manifestos Kubernetes para provisionamento completo da infraestrutura da operação **Ufology Investigation Unit** no cluster.

---

## 📁 Estrutura

```
k8s/
├── kustomization.yaml
├── postgres/
│   ├── 00-namespace.yaml       # Namespace ufology
│   ├── 01-secret.yaml          # Credenciais do PostgreSQL
│   ├── 02-deployment.yaml      # Deployment do PostgreSQL
│   └── 03-service.yaml         # Service ClusterIP porta 5432
├── redis/
│   ├── 04-redis-deployment.yaml  # Deployment do Redis
│   └── 05-redis-service.yaml     # Service ClusterIP porta 6379
└── app/
    ├── 06-configmap.yaml       # ConfigMap com DB_NAME
    ├── 07-secret.yaml          # Secret com DB_PASSWORD
    ├── 08-deployment.yaml      # Deployment da aplicação (2 réplicas)
    ├── 09-service.yaml         # Service ClusterIP porta 8080
    └── 10-pdb.yaml             # PodDisruptionBudget
```

---

## 🧩 Componentes

### Namespace
Todos os recursos são criados dentro do namespace `ufology`, garantindo isolamento completo dos demais sistemas do cluster.

### PostgreSQL
- **Imagem:** `leogloriainfnet/ufodb:1.0-win`
- **Réplicas:** 1
- **Porta:** 5432
- **Credenciais:** gerenciadas via Secret `ufodb-secret`
- **Host interno:** `ufodb-service.ufology.svc.cluster.local`

### Redis
- **Imagem:** `redis:7-alpine`
- **Réplicas:** 1
- **Porta:** 6379
- **Host interno:** `ufocache-service.ufology.svc.cluster.local`

### Aplicação (UfoTracker)
- **Imagem:** `rianporfirio/ufo-tracker:1.0`
- **Réplicas:** 2
- **Porta:** 8080
- **Host interno:** `ufotracker-service.ufology.svc.cluster.local`
- Conecta ao PostgreSQL e Redis via variáveis de ambiente
- Protegida por PodDisruptionBudget com mínimo de 1 réplica disponível

---

## ⚙️ Pré-requisitos

- [kubectl](https://kubernetes.io/docs/tasks/tools/) instalado
- Cluster Kubernetes em execução (Minikube, Docker Desktop, etc.)
- [Kustomize](https://kustomize.io/) — já embutido no kubectl v1.14+

---

## 🚀 Como aplicar

```bash
# Aplicar toda a infraestrutura de uma vez
kubectl apply -k .

# Verificar os recursos criados
kubectl get all -n ufology
```

### Resultado esperado

```
NAME                                        READY   STATUS    RESTARTS   AGE
pod/ufocache-deployment-xxx                 1/1     Running   0          30s
pod/ufodb-deployment-xxx                    1/1     Running   0          30s
pod/ufotracker-deployment-xxx               1/1     Running   0          30s
pod/ufotracker-deployment-xxx               1/1     Running   0          30s

NAME                         TYPE        CLUSTER-IP   PORT(S)    AGE
service/ufocache-service     ClusterIP   ...          6379/TCP   30s
service/ufodb-service        ClusterIP   ...          5432/TCP   30s
service/ufotracker-service   ClusterIP   ...          8080/TCP   30s
```

---

## 🔌 Acessar serviços localmente

Para conectar a serviços do cluster a partir da máquina local, use port-forward:

```bash
# Aplicação
kubectl port-forward svc/ufotracker-service 8080:8080 -n ufology

# PostgreSQL (ex: DBeaver)
kubectl port-forward svc/ufodb-service 5432:5432 -n ufology

# Redis
kubectl port-forward svc/ufocache-service 6379:6379 -n ufology
```

---

## 🔐 Credenciais

| Serviço | Usuário | Senha | Banco |
|---|---|---|---|
| PostgreSQL | `postgres` | `devops2025!` | `ufology` |

> ⚠️ Os Secrets estão em texto puro com `stringData` — adequado para ambiente de estudo. Em produção, utilize ferramentas como **Sealed Secrets**, **External Secrets Operator** ou **Vault**.

---

## 🗑️ Remover a infraestrutura

```bash
# Remover todos os recursos via kustomize
kubectl delete -k .

# Ou deletar o namespace inteiro (mais rápido)
kubectl delete namespace ufology
```
