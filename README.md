# Parte 1 — Avaliação Teórica

## 1) O que realmente nasce quando executamos docker build?

Explique:
- Por que o build não cria um container
- O que são camadas
- O que significa “congelar decisões técnicas”

### Resposta:
Quando executamos o comando `docker build`,
estamos passando a instrução para o client do docker
ler um arquivo `Dockerfile` para gerar uma imagem.

#### Por que o build não cria um container
O comando build não cria um container, pois seu objetivo é unicamente
gerar uma imagem imutável que contém a aplicação e suas dependências. 
Um container é a instância de uma imagem em execução que possui seu próprio
ciclo de vida (nasce, morre, restarta). O docker separa estas duas etapas
- `docker build -> gera uma imagem`
- `docker run <options> -> cria e executa um container a partir da imagem`

---

#### O que são camadas
O `Dockerfile` contém uma sequência de instruções utilizadas para construir uma imagem.
Cada instrução executada gera uma nova camada. Uma camada é uma modificação imutável
no sistema de arquivos da imagem, que é empilhada sobre as camadas anteriores para formar
a imagem final. O Docker utiliza um mecanismo de cache baseado em hash para identificar camadas
que não sofreram alterações, reutilizando-as em builds posteriores para acelerar o processo de 
construção da imagem.

Exemplo: 
```dockerfile
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/app.jar app.jar
CMD ["java", "-jar", "app.jar"]
```

Camadas da Imagem:
````
Camada 4 → CMD ["java", "-jar", "app.jar"]
Camada 3 → COPY target/app.jar app.jar
Camada 2 → WORKDIR /app
Camada 1 → FROM eclipse-temurin:21-jdk
````

---

#### O que significa “congelar decisões técnicas”
Congelar decisões técnicas significa empacotar na imagem Docker todas as escolhas de ambiente
necessárias para executar a aplicação, garantindo que ela rode de forma idêntica em qualquer
infraestrutura. Essa expressão é frequentemente utilizada no contexto de DevOps para se referir
à padronização do ambiente de execução de uma aplicação.
---

## 2) Explique a diferença entre:
- Imagem
- Container
- Volume
- Rede Docker

### Resposta:

#### Imagem
Uma imagem é um modelo somente leitura com instruções para rodar um
container. A imagem possui toda a infraestrutura necessária para que a aplicação
seja executada. Dependências, libs, scripts, runtime e a própria aplicação em si.
Em resumo, é o empacotamento de uma aplicação com todas as dependências necessárias
para ser executada.
---
#### Container
Um container é a instância de uma imagem em execução que roda como processo
isolado no Sistema Operacional, compartilhando o kernel do host, mas com seu
próprio sistema de arquivos, rede e espaços de processos.
---
#### Volume
Volumes no Docker são mecanismos de persistência que permitem armazenar dados
fora do sistema de arquivos do container, garantindo durabilidade, isolamento
e portabilidade, independentemente do ciclo de vida do container.
---
#### Rede Docker
As redes no Docker dizem respeito à capacidade de gerir e isolar a comunicação
entre containers, host e serviços externos por meio de redes virtuais, permitindo 
conexão via resolução automática de nomes (DNS interno).
---
#### E como esses elementos se relacionam no ciclo de vida de uma aplicação.
- Imagem: permite distribuição, versionamento e reprodutibilidade de uma aplicação.
- Container: nos permite criar várias instâncias isoladas de uma imagem em único host.
- Volume: nos permite persistir os dados de maneira desacoplada ao ciclo de vida do container.
- Rede Docker: nos permite isolar todos os serviços de uma aplicação numa única rede com DNS local.

Estes quatro conceitos quando juntos tornam aplicações containerizadas portáveis, previsíveis e fáceis
de operar.

## 3) Por que scripts dentro de /docker-entrypoint-initdb.d/ no Postgres oficial rodam apenas na primeira execução?
No Postgres oficial, os scripts dentro de /docker-entrypoint-initdb.d/ rodam apenas na primeira execução porque o entrypoint
só os executa quando o diretório de dados do banco ainda não foi inicializado. Esse diretório armazena todo o estado persistente
do Postgres, geralmente em /var/lib/postgresql/data. Quando usamos um volume Docker, esse diretório continua existindo mesmo que
o container seja removido. Assim, nas próximas execuções, o Postgres detecta que já há um banco criado e não roda novamente os scripts
de inicialização.

## 4) Em um ambiente com dois containers (API + Banco), por que usar localhost na configuração da API normalmente falha?
Utilizar localhost normalmente falha porque, dentro de um container, localhost aponta para ele mesmo. Como a API e o 
banco estão em containers diferentes, a API não consegue acessar o banco dessa forma. Cada container possui seu próprio
isolamento de rede, e a comunicação entre eles ocorre através de uma network compartilhada do Docker, utilizando a resolução
de nomes pelo DNS interno (geralmente o nome do serviço ou container).

O conceito envolvido é o isolamento de rede entre containers e resolução de nomes via DNS interno do Docker

---

# Parte 2 — Avaliação Prática

## QUESTÃO 1 — Banco Containerizado 

### Dockerfile 
```dockerfile
FROM postgres:17.8-alpine3.23
COPY /api/data/data.sql /docker-entrypoint-initdb.d/
LABEL author="Porfírio"
```
---
### Comando para Build
```shell
docker build -f .\Dockerfile-DB -t ricknmorty-database:1.0 .
```
---
### Comandos de execução
```shell
docker run 
--name ricknmorty-database
-e POSTGRES_PASSWORD=admin 
-v pgdata:/var/lib/postgresql/data 
ricknmorty-database:1.0
```
---
### Comandos de validação

#### Para verificar os logs do container
```shell
docker logs -f ricknmorty-database 
```
---
#### Para acessar o container
```shell
docker exec -it ricknmorty-database /bin/bash
```
---
#### Para acessar o banco de dados
```shell
psql -U postgres
```
---
#### Para verificar os dados inseridos ao inicializar o container
```postgresql
SELECT * FROM CHARACTERS;
```
---
## QUESTÃO 2 — API + Banco na mesma rede Docker

### Comandos de execução

#### Docker Compose
```shell
docker compose up -d
```
---
#### Log container da API
```shell
docker logs -f ricknmorty-api
```
---
#### Retorno dos dados pela API
```shell
curl -X GET http://localhost:8080/api/characters
```