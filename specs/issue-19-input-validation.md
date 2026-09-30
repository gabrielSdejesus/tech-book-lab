# Especificação Técnica: Validação Defensiva de Entrada e Tratamento Robusto de Erros (DDD & Zero-Trust)

**Issue**: [#19 - FEAT: Validação defensiva de entrada ponta a ponta e tratamento robusto de erros (DDD & Zero-Trust)](https://github.com/gabrielSdejesus/tech-book-lab/issues/19)  
**Status**: Proposta para Aprovação Humana  
**Branch**: `feat/issue-19-input-validation`  
**Worktree**: `.worktrees/feat-issue-19-input-validation`

---

## 1. Contexto e Motivação
Na arquitetura do Tech Book Lab, o backend interage diretamente com motores relacionais (PostgreSQL 16) e grafos (Neo4j 5), além de provedores externos de Inteligência Artificial (Google Gemini e Ollama local). Atualmente, diversos endpoints da API REST aceitam objetos de requisição sem validação declarativa (`@Valid`), permitindo o trânsito de campos nulos, vazios, strings excessivamente longas ou identificadores que não existem no banco de dados.

Adicionalmente, erros de sintaxe ou execução de banco nas consultas dos alunos estavam sendo mascarados sob respostas HTTP 200 com payload estruturado de erro, e não havia um tratamento global padronizado para erros de validação sintática (Bean Validation) ou falhas de parse de JSON.

Esta especificação implementa a filosofia **Zero-Trust** e **Domain-Driven Design (DDD)** em todas as fronteiras externas do sistema:
1. **Validação Sintática**: Aplicação estrita de Jakarta Bean Validation nos DTOs de entrada.
2. **Validação Semântica & Invariantes de Domínio**: Verificação de consistência e existência real no catálogo (banco de dados) antes de despachar comandos aos serviços e motores.
3. **Padronização RFC 7807 (`ProblemDetail`)**: Respostas de erro padronizadas em HTTP 400 Bad Request contendo diagnóstico claro e seguro.
4. **Consumo no Frontend**: Tratamento gracioso dos erros RFC 7807 na camada de serviços do cliente React e apresentação amigável no `LabWorkspace`.

---

## 2. Modelagem de Domínio, Invariantes e Contratos

### 2.1 Mapeamento de DTOs e Regras de Entrada

#### A. `QueryRequest` (`com.dataintensive.lab.query.QueryRequest`)
- **Invariantes Sintáticas**:
  - `query`: `@NotBlank(message = "A consulta SQL/Cypher é obrigatória")`, `@Size(max = 10000, message = "A consulta não pode exceder 10.000 caracteres")`.
  - `engineType`: `@NotNull(message = "O tipo de motor de banco (engineType) é obrigatório")`.
  - `labId`: `@NotBlank(message = "O identificador do laboratório (labId) é obrigatório")`.
- **Invariantes Semânticas**:
  - `labId` deve referenciar um laboratório existente no catálogo cadastrado (`CatalogService.getLabById(labId)`). Caso contrário, lança `DomainValidationException` (HTTP 400).
- **Semântica de Execução**:
  - Quando a consulta submetida contiver erro de sintaxe SQL/Cypher ou violar regras do banco, o serviço lança `QueryExecutionException` e o controlador responde com **HTTP 400 Bad Request** estruturado com `ProblemDetail`.

#### B. `AiAssessmentRequest` (`com.dataintensive.lab.ai.AiAssessmentRequest`)
- **Invariantes Sintáticas**:
  - `labId`: `@NotBlank(message = "O identificador do laboratório (labId) é obrigatório")`.
  - `challengeId`: `@NotBlank(message = "O identificador do desafio (challengeId) é obrigatório")`.
  - `userQuery`: `@NotBlank(message = "A consulta do usuário é obrigatória")`, `@Size(max = 10000, message = "A consulta não pode exceder 10.000 caracteres")`.
- **Invariantes Semânticas**:
  - `labId` e `challengeId` devem obrigatoriamente existir no catálogo. Elimina qualquer possibilidade de `NullPointerException` em `buildPrompt`.

#### C. `AiTestConnectionRequest` (`com.dataintensive.lab.ai.AiTestConnectionRequest`)
- **Invariantes Sintáticas**:
  - `provider`: `@NotBlank(message = "O provedor de IA é obrigatório")`.
- **Invariantes Semânticas**:
  - O valor de `provider` deve pertencer ao conjunto permitido: `gemini` ou `ollama` (ignorado caixa alta/baixa). Valores divergentes rejeitados com HTTP 400.

#### D. Parâmetros de Rota (`CatalogController`)
- `@PathVariable String bookId`: validação de não vazio e sanitização contra injeção de caracteres de controle.

---

### 2.2 Tratador Global de Exceções (`GlobalExceptionHandler`)

Criação da classe `com.dataintensive.lab.config.GlobalExceptionHandler` anotada com `@RestControllerAdvice`:

| Exceção | Status HTTP | Detalhes do RFC 7807 (`ProblemDetail`) |
| :--- | :--- | :--- |
| `MethodArgumentNotValidException` | `400 Bad Request` | `title`: "Erro de validação sintática", campo customizado `errors`: lista de `{ "field": "...", "message": "..." }` |
| `HttpMessageNotReadableException` | `400 Bad Request` | `title`: "Requisição JSON inválida", `detail`: "O corpo da requisição é nulo ou contém formato JSON inválido." |
| `DomainValidationException` | `400 Bad Request` | `title`: "Regra de negócio violada", `detail`: mensagem descritiva da inconsistência de domínio |
| `QueryExecutionException` | `400 Bad Request` | `title`: "Erro na execução da consulta", `detail`: diagnóstico da falha no banco SQL/Cypher |
| `Exception` (fallback) | `500 Internal Server Error` | `title`: "Erro interno do servidor", `detail`: mensagem segura sem vazamento de stacktrace |

---

### 2.3 Camada Frontend (`frontend/src/services/api.ts`)

- O método `post` e as funções auxiliares inspecionam o status HTTP da resposta:
  - Se status `400`: faz o parse de `ProblemDetail` (RFC 7807) e extrai `detail` ou concatena mensagens de `errors`.
  - O componente `LabWorkspace` captura o erro e renderiza o feedback no painel de resultados com status de erro e estilização adequada, sem travar a interface.

---

## 3. Estratégia de Testes (TDD Seams)

### Seam 1: Validação Sintática de Entrada (`GlobalExceptionHandler` + Bean Validation)
- **Arquivo**: `com.dataintensive.lab.api.ApiIntegrationTest`
- **Cenários**:
  1. `POST /api/query/execute` com body nulo/vazio -> retorna `400 Bad Request` com RFC 7807 e lista de erros.
  2. `POST /api/query/execute` com query superior a 10.000 caracteres -> retorna `400 Bad Request`.
  3. `POST /api/query/execute` com JSON malformado sintaticamente -> retorna `400 Bad Request` legível.
  4. `POST /api/ai/test-connection` com provider não reconhecido (`"chatgpt"`) -> retorna `400 Bad Request`.

### Seam 2: Validação Semântica de Domínio e Execução de Banco
- **Arquivos**: `QueryExecutionServiceTest.java` e `ApiIntegrationTest.java`
- **Cenários**:
  1. Execução de query apontando para `labId` inexistente -> lança `DomainValidationException` e responde `400 Bad Request`.
  2. Execução de instrução SQL inválida no PostgreSQL -> lança `QueryExecutionException` e responde `400 Bad Request` com detalhes da falha.
  3. Avaliação de IA apontando para `challengeId` que não pertence ao laboratório -> responde `400 Bad Request`.

### Seam 3: Integração no Frontend (`api.test.ts` e `LabWorkspace.test.tsx`)
- **Cenários**:
  1. `api.test.ts`: testar extração de mensagens de erro formatadas a partir de resposta RFC 7807 (`ProblemDetail`).
  2. `LabWorkspace.test.tsx`: simular retorno de HTTP 400 ao rodar query inválida e validar exibição de erro no painel.

---

## 4. Plano de Commits Atômicos

1. `test(validation): adicionar testes de integracao para validacao sintatica e erro RFC 7807`
2. `feat(validation): implementar GlobalExceptionHandler com RFC 7807 ProblemDetail e anotacoes Bean Validation nos DTOs`
3. `test(query): adicionar testes para validacao de labId inexistente e falha de execucao SQL com HTTP 400`
4. `feat(query): implementar validacao semantica de labId e lancamento de QueryExecutionException como HTTP 400`
5. `test(ai): adicionar testes para validacao de provider e consistencia de challengeId na avaliacao IA`
6. `feat(ai): implementar validacoes semanticas nos endpoints do AiAssessmentController`
7. `test(frontend): adicionar testes de tratamento de erro RFC 7807 no servico de API e LabWorkspace`
8. `feat(frontend): integrar tratamento de ProblemDetail no cliente de API e exibicao de erros (closes #19)`
9. `docs(spec): registrar especificacao tecnica de validacao defensiva e atualizar documentacao`
