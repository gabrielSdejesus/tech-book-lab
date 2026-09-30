# Tech Book Lab (TBL) 🚀

Plataforma de laboratórios interativos para estudos práticos de livros de engenharia de software, bancos de dados e sistemas distribuídos — iniciando com **"Designing Data-Intensive Applications"** de Martin Kleppmann.

---

## 🏗️ Arquitetura do Sistema

- **Backend (Java 21 + Spring Boot 3.4):**
  - Catálogo modular de livros, capítulos e laboratórios (`/api/books`, `/api/labs`).
  - Motor de execução dinâmico de queries SQL (PostgreSQL 16) e Cypher (Neo4j 5) via conexões seguras (`/api/query/execute`).
  - Orquestrador de reset de schemas com 1 clique (`/api/query/reset/{labId}`).
  - Monitor de integridade de containers Docker (`/api/infra/status`).
  - **Tutor de IA em Tempo Real (`/api/ai/assess`):** Avaliação socrática baseada em trade-offs de Martin Kleppmann, sem gabaritos rígidos (suporta Google Gemini 2.0 Flash gratuito ou Ollama local leve).
- **Frontend (React 19 + TypeScript + Tailwind CSS v4 + Vite):**
  - Navegação fluida entre capítulos e laboratórios.
  - Workspace com editor de código (SQL/Cypher), atalhos de teclado (`Ctrl + Enter`), visualizador de dados tabulares, JSON e painel de análise do Tutor de IA.
  - Indicadores de saúde dos containers em tempo real.
- **Infraestrutura (Docker Compose):**
  - `tbl-postgres`: PostgreSQL 16 Alpine na porta `5432` (`tbl_lab`).
  - `tbl-neo4j`: Neo4j 5 Community na porta `7474` (Web) e `7687` (Bolt).

---

## ⚡ Como Executar o Projeto

### 1. Subir os Containers Docker
```bash
docker compose -f infra/docker-compose.yml up -d
```

### 2. Iniciar o Backend (Java Spring Boot)
```bash
cd backend
./mvnw spring-boot:run
```
*A API estará disponível em `http://localhost:8080`.*

### 3. Iniciar o Frontend Web (React + Vite)
```bash
cd frontend
npm install
npm run dev
```
*Acesse a interface em `http://localhost:5173`.*

---

## 🤖 Configuração do Tutor de IA

O tutor de IA avalia seu código, os dados retornados pelo banco real e a sua reflexão conceitual sobre trade-offs.

- **Online Gratuito (Recomendado):**
  - Obtenha uma chave gratuita no [Google AI Studio](https://aistudio.google.com/).
  - Insira na interface clicando no botão **"Tutor IA Config"** no canto superior direito, ou configure a variável de ambiente:
    ```bash
    export GEMINI_API_KEY="sua_chave_aqui"
    ```
- **Local (Ollama Offline):**
  - Caso queira rodar offline, inicie o Ollama com:
    ```bash
    ollama run qwen2.5-coder:1.5b
    ```
  - Selecione **"Ollama Local"** no modal de configurações.
- **Fallback Heurístico:** Se nenhuma chave for fornecida, a plataforma continuará funcionando normalmente com análise heurística arquitetural local.

---

## 📚 Laboratórios Disponíveis (Capítulo 3 - DDIA)

1. **Lab 1: Relacional vs Documentos e Localidade de Armazenamento**
   - *PostgreSQL:* Incompatibilidade objeto-relacional (impedance mismatch), 3NF com joins vs busca direta em coluna `JSONB`.
2. **Lab 2: Grafos de Propriedades vs SQL Recursivo**
   - *Neo4j & Postgres:* Hierarquias com profundidade dinâmica (Cypher `[:WITHIN*]`) versus Common Table Expression recursiva (`WITH RECURSIVE`).
3. **Lab 3: Modelagem Dimensional para Análise (Star Schema / OLAP)**
   - *PostgreSQL:* Tabelas de fatos e dimensões desnormalizadas, consultas analíticas agregadas (*slice and dice*).
4. **Lab 4: CQRS e Projeções Derivadas de um Log de Eventos**
   - *PostgreSQL:* Event Store append-only imutável e Views Materializadas para leitura veloz com consistência eventual.
