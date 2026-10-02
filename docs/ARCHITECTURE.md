# Arquitetura do Sistema — Tech Book Lab (TBL)

O **Tech Book Lab (TBL)** é uma plataforma educacional interativa projetada para o aprendizado prático e aprofundado de engenharia de software, modelagem de dados, arquiteturas de armazenamento e sistemas distribuídos — fundamentada em obras canônicas como *"Designing Data-Intensive Applications"* (Martin Kleppmann).

Este documento detalha o mapa arquitetural completo da aplicação, as decisões de engenharia adotadas (ADRs), a modelagem de dados, os fluxos de ciclo de vida e os padrões de projeto que sustentam o sistema.

---

## 1. Visão Geral e Mapa de Componentes (C4 Nível 2)

O Tech Book Lab opera em uma arquitetura desacoplada, orientada a componentes, combinando uma interface web reativa, uma API intermediária de governança e orquestração, bancos de dados reais isolados em contêineres e provedores plugáveis de inteligência artificial.

```mermaid
graph TD
    subgraph Client["Cliente / Navegador Web"]
        UI["Frontend SPA (React 19 + Vite + Tailwind CSS)"]
        State["LanguageContext & Workbench State"]
        EngineConfig["engineConfig.ts (Metadados Dinâmicos)"]
        UI --> State
        UI --> EngineConfig
    end

    subgraph BackendApp["Backend (Spring Boot 3.4 / Java 21)"]
        API["REST Controllers (Catalog, Query, Infra, AI)"]
        
        subgraph CatalogModule["Módulo de Catálogo"]
            CatService["CatalogService"]
            JdbcRepo["JdbcCatalogRepository"]
            CatService --> JdbcRepo
        end

        subgraph ProvisioningModule["Módulo de Provisionamento & Ciclo de Vida"]
            ProvService["LabProvisioningService"]
            DockerMgr["DockerComposeLabManager"]
            InactivityMon["LabInactivityMonitor (@Scheduled)"]
            ProvService --> DockerMgr
            InactivityMon --> ProvService
        end

        subgraph QueryModule["Módulo de Execução de Consultas"]
            QRegistry["QueryEngineRegistry (Strategy)"]
            PgEngine["PostgresQueryEngine (JDBC)"]
            NeoEngine["Neo4jQueryEngine (Bolt Driver)"]
            QRegistry --> PgEngine
            QRegistry --> NeoEngine
        end

        subgraph AiModule["Módulo de Avaliação Socrática"]
            AiRegistry["AiProviderRegistry (Strategy)"]
            GeminiClient["GeminiAiClient (Google SDK)"]
            OllamaClient["OllamaAiClient (REST Local)"]
            HeuristicClient["HeuristicAiClient (Regras Locais)"]
            AiRegistry --> GeminiClient
            AiRegistry --> OllamaClient
            AiRegistry --> HeuristicClient
        end

        API --> CatService
        API --> ProvService
        API --> QRegistry
        API --> AiRegistry
    end

    subgraph Storage["Persistência de Metadados"]
        SQLiteDB[("SQLite Catalog (tbl_catalog.db)<br/>WAL Mode + Flyway")]
        JdbcRepo --> SQLiteDB
    end

    subgraph EphemeralInfra["Infraestrutura Docker sob Demanda"]
        DockerCLI["Docker Compose CLI"]
        DockerMgr --> DockerCLI
        DockerCLI --> PostgresCont["tbl-lab-postgres (Porta 5432)"]
        DockerCLI --> Neo4jCont["tbl-lab-neo4j (Portas 7474 / 7687)"]
        PgEngine -.->|JDBC| PostgresCont
        NeoEngine -.->|Bolt Protocol| Neo4jCont
    end

    subgraph ExternalAI["Provedores de IA"]
        GeminiAPI["Google Gemini 2.0 Flash API"]
        LocalOllama["Ollama Daemon (localhost:11434)"]
        GeminiClient -.->|HTTPS| GeminiAPI
        OllamaClient -.->|HTTP| LocalOllama
    end

    UI -->|HTTP / JSON| API
```

---

## 2. Decisões Arquiteturais Fundamentais (ADRs)

### ADR 01: Arquitetura Single-User Local-First e Portas Determinísticas
- **Contexto**: A plataforma destina-se a estudos individuais onde cada desenvolvedor roda o laboratório diretamente em seu computador pessoal.
- **Decisão**:
  - Evitar a sobrecarga de orquestração multi-tenant (Kubernetes, namespaces dinâmicos, proxies reversos complexos).
  - Alocar portas estáticas previsíveis para os contêineres: PostgreSQL (`5432`) e Neo4j (`7687` Bolt / `7474` Web).
  - Permitir que o aluno inspecione diretamente o banco com ferramentas do seu dia a dia (DBeaver, TablePlus, psql, Neo4j Browser) sem intermediários.
- **Consequências**: Setup trivial, consumo mínimo de recursos e experiência transparente para o desenvolvedor.

### ADR 02: Catálogo Dinâmico em SQLite com WAL Mode e Estratégia de Dupla Inserção (Multilingual)
- **Contexto**: O catálogo de livros, capítulos e laboratórios precisava ser dinâmico, portável, versionável e suportar internacionalização (PT-BR e EN) sem depender de um contêiner de banco adicional rodando permanentemente.
- **Decisão**:
  - Utilizar **SQLite** embarcado com **WAL Mode (Write-Ahead Logging)** gerenciado por migrações versionadas com **Flyway** (V1 a V6).
  - Adotar a **Estratégia de Dupla Inserção** para internacionalização:
    1. *Inserção Canônica*: O conteúdo base é inserido nas tabelas de entidade normalizadas (`books`, `chapters`, `labs`, `challenges`) no idioma canônico padrão (Português).
    2. *Inserção de Tradução*: A tabela `catalog_translations` armazena pares chave-valor traduzidos indexados por `(locale, entity_type, entity_id, field_name)`.
  - A resolução de traduções no `JdbcCatalogRepository` ocorre via *lookup* em memória com fallback automático para o valor canônico caso a chave não possua tradução cadastrada.
- **Consequências**:
  - Schema relacional normalizado e limpo, sem multiplicação de colunas como `title_pt`, `title_en`.
  - Consultas rápidas sem necessidade de explosão de `LEFT JOINs` complexos no banco.
  - Concorrência de leitura totalmente livre de locks devido ao modo WAL.
  - Adição de novos idiomas sem nenhuma alteração estrutural no banco de dados.

### ADR 03: Provisionamento sob Demanda e Ciclo de Vida por Inatividade
- **Contexto**: Manter instâncias de PostgreSQL e Neo4j em execução contínua consome centenas de megabytes de memória RAM e ciclos de CPU desnecessariamente.
- **Decisão**:
  - Contêineres são iniciados sob demanda quando o usuário acessa um laboratório específico (`LabProvisioningService`).
  - Troca dinâmica de motor: se o desafio atual requer Neo4j e o anterior usava Postgres, o contêiner anterior é pausado e o novo é ativado.
  - **Monitoramento de Inatividade (`LabInactivityMonitor`)**:
    - Tarefa em background agendada (`@Scheduled`) verifica a cada 15 segundos o tempo decorrido desde o último acesso.
    - TTL padrão configurável de 15 minutos (`lab.container.inactivity-ttl`).
    - Frontend envia *heartbeat* a cada 30 segundos enquanto a aba do laboratório estiver ativa.
    - **Teardown Imediato**: Quando o usuário desmonta a interface do laboratório ou sai da página (`beforeunload` ou troca de rota), o frontend aciona `POST /api/infra/teardown`, desligando imediatamente os contêineres e liberando os recursos locais.
- **Consequências**: Uso responsável de hardware, viabilizando execução suave mesmo em computadores com recursos limitados.

### ADR 04: Padrão Strategy Desacoplado para Motores de Banco e Provedores de IA
- **Contexto**: A plataforma precisava suportar múltiplos motores de banco de dados heterogêneos (SQL relacional, grafos Cypher, e futuros motores NoSQL/chave-valor) e diferentes provedores de IA (cloud e local).
- **Decisão**:
  - Implementar o padrão **Strategy com Registry Lookup**:
    - `QueryEngineExecutor` + `QueryEngineRegistry`: dispatch em tempo constante `O(1)` para o executor responsável (`PostgresQueryEngine`, `Neo4jQueryEngine`).
    - `AiProviderClient` + `AiProviderRegistry`: dispatch dinâmico para `GeminiAiClient`, `OllamaAiClient` ou `HeuristicAiClient`.
- **Consequências**: Princípio Aberto/Fechado (OCD/SOLID) preservado. Novos bancos de dados ou novos modelos de IA podem ser adicionados sem tocar na lógica dos controladores ou serviços orquestradores.

---

## 3. Modelo de Dados e Catálogo (SQLite + Flyway)

O catálogo técnico é armazenado no arquivo SQLite local `tbl_catalog.db`.

```mermaid
erDiagram
    books ||--o{ chapters : "possui"
    chapters ||--o{ labs : "possui"
    labs ||--o{ challenges : "contém"
    labs ||--o{ lab_key_concepts : "apresenta"
    challenges ||--o{ challenge_guidelines : "define"
    catalog_translations }|--|| books : "traduz"
    catalog_translations }|--|| chapters : "traduz"
    catalog_translations }|--|| labs : "traduz"

    books {
        VARCHAR id PK
        VARCHAR title
        VARCHAR author
        VARCHAR tag_line
        VARCHAR cover_color
        VARCHAR cover_image_url
        TEXT description
        TIMESTAMP created_at
    }

    chapters {
        VARCHAR id PK
        VARCHAR book_id FK
        INT number
        VARCHAR title
        VARCHAR subtitle
        TEXT summary
    }

    labs {
        VARCHAR id PK
        VARCHAR chapter_id FK
        INT number
        VARCHAR slug UK
        VARCHAR title
        TEXT summary
        VARCHAR engine_type
        VARCHAR database_name
        TEXT reset_schema_sql
    }

    challenges {
        VARCHAR id PK
        VARCHAR lab_id FK
        INT order_index
        VARCHAR title
        VARCHAR engine_type
        TEXT description
        TEXT scenario
        TEXT starter_template
        TEXT reflection_prompt
    }

    catalog_translations {
        VARCHAR entity_type PK
        VARCHAR entity_id PK
        VARCHAR locale PK
        VARCHAR field_name PK
        TEXT translation_text
    }
```

---

## 4. Ciclo de Vida e Provisionamento de Contêineres

O diagrama abaixo ilustra o fluxo completo de inicialização, envio de heartbeat, detecção de inatividade e encerramento antecipado de contêineres:

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Usuário / Desenvolvedor
    participant UI as Frontend (LabWorkspace)
    participant ProvCtrl as LabProvisioningController
    participant ProvService as LabProvisioningService
    participant Monitor as LabInactivityMonitor (@Scheduled)
    participant DockerMgr as DockerComposeLabManager
    participant Docker as Docker Daemon

    Note over Dev,Docker: 1. Acesso ao Laboratório & Provisionamento
    Dev->>UI: Seleciona Laboratório / Desafio
    UI->>ProvCtrl: POST /api/infra/provision (sessionId, labId, challengeId)
    ProvCtrl->>ProvService: provisionLab(...)
    ProvService->>DockerMgr: startEngine(engineType)
    DockerMgr->>Docker: docker compose up -d (tbl-lab-postgres / neo4j)
    Docker-->>DockerMgr: Contêiner iniciado
    ProvService-->>UI: LabSession (status: READY, porta: 5432/7687)

    Note over Dev,Docker: 2. Manutenção de Sessão Ativa (Heartbeat a cada 30s)
    loop Heartbeat Periódico
        UI->>ProvCtrl: POST /api/infra/heartbeat (sessionId, labId)
        ProvCtrl->>ProvService: heartbeat(sessionId, labId)
        ProvService-->>UI: ACK (ttlRemaining: 900s)
    end

    Note over Dev,Docker: 3. Cenário A: Encerramento Imediato (Saída do Lab)
    Dev->>UI: Sai do Laboratório ou Fecha a Aba
    UI->>ProvCtrl: POST /api/infra/teardown (sessionId, labId)
    ProvCtrl->>ProvService: teardown(sessionId, labId)
    ProvService->>DockerMgr: stopEngine(engineType)
    DockerMgr->>Docker: docker compose stop (contêiner)
    ProvService-->>UI: LabSession (status: STOPPED)

    Note over Dev,Docker: 4. Cenário B: Timeout de Inatividade (TTL de 15m sem Heartbeat)
    Monitor->>ProvService: monitorInactivity()
    Note over ProvService: Detecta tempo decorrido > 15m
    ProvService->>DockerMgr: stopEngine(engineType)
    DockerMgr->>Docker: docker compose stop
```

---

## 5. Execução de Consultas (Pluggable Query Engine)

O fluxo de execução de consultas suporta queries em SQL padrão (PostgreSQL) e consultas em Cypher (Neo4j):

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Usuário
    participant UI as Editor de Queries
    participant QC as QueryController
    participant Registry as QueryEngineRegistry
    participant Engine as QueryEngineExecutor (Pg / Neo4j)
    participant DB as Banco de Dados Real (Postgres / Neo4j)

    Dev->>UI: Digita query e aciona Ctrl+Enter
    UI->>QC: POST /api/query/execute (engine, database, query)
    QC->>Registry: getExecutor(engine)
    Registry-->>QC: Instância do QueryEngineExecutor registrado
    QC->>Engine: execute(database, query)
    Engine->>DB: Executa query nativa via Driver seguro
    DB-->>Engine: ResultSet / Record Stream
    Engine->>Engine: Mapeia colunas, tipos e linhas em memória
    Engine-->>QC: QueryResult (columns, rows, executionTimeMs)
    QC-->>UI: HTTP 200 OK (QueryResult JSON)
    UI-->>Dev: Renderiza grid de dados tabulares ou JSON
```

---

## 6. Avaliação Socrática com Tutor de IA (Pluggable AI Strategy)

Ao submeter a query e uma reflexão conceitual, o usuário recebe uma avaliação baseada nos trade-offs reais abordados pelos livros de engenharia:

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Usuário
    participant UI as Painel do Tutor de IA
    participant AiCtrl as AiAssessmentController
    participant AiService as AiAssessmentService
    participant Registry as AiProviderRegistry
    participant Client as AiProviderClient (Gemini / Ollama / Heuristic)
    participant ExternalLLM as LLM (Google Gemini / Ollama)

    Dev->>UI: Submete Reflexão Conceitual e Resultados
    UI->>AiCtrl: POST /api/ai/assess (challengeId, query, userReflection, provider)
    AiCtrl->>AiService: assess(...)
    AiService->>Registry: getClient(providerId)
    Registry-->>AiService: AiProviderClient selecionado
    AiService->>Client: assess(request, challengeGuidelines, language)
    alt Gemini Cloud
        Client->>ExternalLLM: Chamada com Prompt Socrático (Google GenAI SDK)
        ExternalLLM-->>Client: Resposta estruturada de trade-offs
    else Ollama Local
        Client->>ExternalLLM: Chamada REST em localhost:11434
        ExternalLLM-->>Client: Resposta do modelo local
    else Heurística Offline
        Client->>Client: Avaliação analítica local por regras estruturadas
    end
    Client-->>AiService: AiAssessmentResult (status, feedback, tradeOffAnalysis)
    AiService-->>UI: HTTP 200 OK
    UI-->>Dev: Apresenta feedback socrático e sugestões de aprofundamento
```

---

## 7. Arquitetura Frontend (React 19 + TypeScript + Vite)

A interface foi projetada para máxima ergonomia técnica e clareza didática:

- **Gerenciamento de Estado**:
  - `LanguageContext`: provê estado de internacionalização reativo (`pt` / `en`) para textos estáticos da UI e parâmetros de consulta à API.
  - Seleção contextual de Livro, Capítulo e Desafio em nível de raiz (`App.tsx`).
- **Desacoplamento de Configuração de Motores (`engineConfig.ts`)**:
  - Centraliza os metadados visuais (ícones Lucide, títulos, tags de badges e esquemas de cores Tailwind) para cada banco de dados suportado (`POSTGRES`, `NEO4J`), eliminando regras de renderização codificadas nos componentes de tela.
- **Componentes do Workbench**:
  - `Navbar`: alternância de idioma, seletor de IA, tema e status de prontidão dos contêineres.
  - `Sidebar`: árvore hierárquica navegável de capítulos e laboratórios.
  - `Bookshelf`: vitrine visual com cartões dos livros catalogados.
  - `LabWorkspace`: workbench principal contendo editor com syntax highlight, atalho de teclado (`Ctrl + Enter`), visualizador de resultados de query, botão de reset de schema e painel socrático do tutor de IA.
