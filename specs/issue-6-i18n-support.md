# Especificação Técnica: Internacionalização (i18n) com PT-BR Nativo e Suporte a Inglês (EN)

**Issue**: [#6 - FEAT: Internacionalização (i18n) com PT-BR como padrão e suporte a tradutor para Inglês](https://github.com/gabrielSdejesus/tech-book-lab/issues/6)  
**Status**: Proposta Consolidada  
**Branch**: `feat/issue-6-i18n-support`  
**Padrão Arquitetural Escolhido**: **Type-Safe Native Context Architecture** (Arquitetura de Contexto Nativo Tipado)

---

## 1. Contexto e Motivação
A aplicação *Tech Book Lab* foi projetada sob uma forte identidade editorial clássica para engenharia de software e sistemas intensivos em dados.

Atualmente:
1. Alguns termos técnicos, dicas, feedbacks e seletores estão mesclados entre português e inglês ou sem centralização de dicionário.
2. Não existe controle visual na interface para chavear entre **Português (`pt-BR`)** e **Inglês (`en-US`)**.
3. O Tutor de IA gera prompts e validações exclusivamente em português, sem adaptar suas orientações pedagógicas quando o usuário prefere interagir em inglês.

Esta especificação define o modelo arquitetural único e definitivo que será seguido na implementação.

---

## 2. Decisão Arquitetural Consolidada

Adotaremos exclusivamente o modelo **Type-Safe Native Context Architecture** no frontend e **Contextual Locale Parameter** no backend.

### 2.1 Por que este modelo foi o escolhido?
1. **Zero Novas Dependências:** Não adiciona bibliotecas externas pesadas (como `i18next`, `react-i18next` ou plugins de terceiros).
2. **Segurança Estrita em Tempo de Compilação:** O TypeScript garante que qualquer chave criada no dicionário `pt` DEVE obrigatoriamente existir em `en`. Se faltar uma tradução, a compilação (`tsc -b`) falha imediatamente.
3. **Ergonomia e Autocomplete Nativo:** O acesso às traduções nos componentes é feito diretamente via objeto tipado (`t.bookshelf.title`, `t.common.save`, `t.lab.executeQuery`), eliminando "strings mágicas" propensas a erros de digitação (como `t('bookshelf.title')`).
4. **Desempenho Instantâneo:** Sem parsing de strings em runtime ou carregamento assíncrono de arquivos remotos.

---

## 3. Especificação do Modelo Técnico

### 3.1 Frontend (React 19 / TypeScript)

#### 3.1.1 Contrato de Tipagem (`src/i18n/types.ts`)
```typescript
export type Locale = 'pt' | 'en';

export interface TranslationSchema {
  common: {
    bookshelf: string;
    goToBookshelf: string;
    returnToBookshelf: string;
    toggleTheme: string;
    configureAi: string;
    connected: string;
    offline: string;
    cancel: string;
    save: string;
    close: string;
    loading: string;
    error: string;
  };
  bookshelf: {
    catalogTag: string;
    version: string;
    title: string;
    subtitle: string;
    booksAvailable: (count: number) => string;
    chaptersCount: (count: number) => string;
    labsActive: (count: number) => string;
    classicCollection: string;
    clickToEnter: string;
    notebookStructure: (chapters: number, labs: number) => string;
    openNotebook: string;
    emptyCatalog: string;
  };
  sidebar: {
    tableOfContents: string;
    notebookStructure: string;
    chapter: string;
    practicalLabs: string;
    keyConcepts: string;
  };
  lab: {
    laboratory: string;
    labWorksheet: string;
    engineeringScenario: string;
    practicalChallenge: string;
    requirementsAndConstraints: string;
    reflectiveQuestion: string;
    conceptualReflection: string;
    reflectionPlaceholder: string;
    queryEditor: string;
    executeQuery: string;
    resetDatabase: string;
    evaluateWithAi: string;
    evaluating: string;
    executing: string;
    resetting: string;
    queryResult: string;
    rowsInMs: (count: number, ms: number) => string;
    noResult: string;
    aiEvaluation: string;
    approved: string;
    needsRevision: string;
    discussion: string;
    tabs: {
      diagnosis: string;
      tradeOffs: string;
      performance: string;
      alternatives: string;
    };
    confirmReset: string;
    executionError: string;
  };
  aiModal: {
    title: string;
    provider: string;
    apiKey: string;
    model: string;
    testConnection: string;
    saveSettings: string;
    testing: string;
    geminiDescription: string;
    ollamaDescription: string;
    modelPlaceholder: string;
  };
}
```

#### 3.1.2 Dicionário Tipado (`src/i18n/translations.ts`)
```typescript
import type { Locale, TranslationSchema } from './types';

export const translations: Record<Locale, TranslationSchema> = {
  pt: { /* Objeto com 100% das chaves em Português */ },
  en: { /* Objeto com 100% das chaves em Inglês */ },
};
```

#### 3.1.3 Contexto e Hook Oficial (`src/i18n/LanguageContext.tsx`)
- Define `LanguageProvider` que envolve a aplicação no `App.tsx`.
- Lê e persiste a preferência em `localStorage.getItem('tbl_locale')`. Caso não exista, assume `'pt'` como padrão nativo soberano.
- Fornece o hook oficial `useLanguage()`:
  ```typescript
  export interface LanguageContextValue {
    locale: Locale;
    setLocale: (locale: Locale) => void;
    t: TranslationSchema;
  }
  ```

#### 3.1.4 Seletor na Navbar (`src/components/Navbar.tsx`)
- Exibe o botão de alternância `PT | EN` entre o botão de tema e o botão de configuração de IA.
- Ao clicar em `EN`, aciona `setLocale('en')` e re-renderiza instantaneamente a interface com o dicionário inglês.

### 3.2 Backend (Java 21 / Spring Boot)

#### 3.2.1 Domínio & Invariantes: Enum `AssessmentLanguage` (`com.dataintensive.lab.domain.AssessmentLanguage`)
Para blindar o contrato de borda da API e impedir que valores arbitrários sejam tolerados pelo backend, definimos um Enum canônico com validação estrita:
```java
package com.dataintensive.lab.domain;

public enum AssessmentLanguage {
    PT,
    EN;

    public static AssessmentLanguage from(String raw) {
        if (raw == null || raw.isBlank()) {
            return PT; // fallback padrão soberano
        }
        String clean = raw.trim().toLowerCase();
        if (clean.equals("pt") || clean.equals("pt-br") || clean.equals("pt_br")) {
            return PT;
        }
        if (clean.equals("en") || clean.equals("en-us") || clean.equals("en_us")) {
            return EN;
        }
        throw new IllegalArgumentException("Idioma '" + raw + "' não suportado. Idiomas válidos permitidos: 'pt', 'en'.");
    }
}
```

#### 3.2.2 Contrato de Requisição (`AiAssessmentRequest.java`)
```java
public record AiAssessmentRequest(
    String labId,
    String challengeId,
    String userQuery,
    String executionSummary,
    String userReflection,
    String apiKeyOverride,
    String providerOverride,
    String modelOverride,
    String language // estritamente validado contra AssessmentLanguage
) {}
```

#### 3.2.3 Blindagem na Camada de Controle (`AiAssessmentController.java`)
- O controller valida o campo `request.language()` através de `AssessmentLanguage.from(request.language())`.
- Caso seja enviado um valor inválido (ex: `"es"`, `"fr"`, `"xyz"`), a API recusa a requisição respondendo **HTTP 400 Bad Request**:
  ```json
  {
    "error": "BAD_REQUEST",
    "message": "Idioma 'es' não suportado. Idiomas válidos permitidos: 'pt', 'en'."
  }
  ```

#### 3.2.4 Serviço do Tutor de IA (`AiAssessmentService.java`)
- O serviço recebe `AssessmentLanguage language`:
  - Se `AssessmentLanguage.EN`, utiliza a diretriz pedagógica em inglês:
    `"You are an Expert Data Engineering and Distributed Systems Tutor evaluating a practical exercise based on the book 'Designing Data-Intensive Applications' by Martin Kleppmann..."` e instrui a geração do JSON com `feedback`, `tradeOffAnalysis`, `efficiencyNotes` e `alternativeApproaches` em inglês.
  - Se `AssessmentLanguage.PT`, mantém a diretriz existente em português.
- As mensagens de validação (ex: submissão vazia ou template inalterado) retornam no idioma solicitado.


---

## 4. Ciclo TDD Estrito (Red -> Green -> Refactor)

### Etapa 1: Worktree Isolada
- Criação e checkout na worktree `.worktrees/feat-issue-6-i18n-support` a partir da `origin/main` atualizada.

### Etapa 2: Red Stage (Testes automatizados que falham comprovando o seam)
1. **Frontend:**
   - `src/i18n/LanguageContext.test.tsx`: Testar inicialização com `pt`, alternância para `en` e persistência no `localStorage`.
   - `src/components/Navbar.test.tsx`: Testar a renderização do botão `PT | EN` e clique de alternância.
   - `src/components/Bookshelf.test.tsx` e `src/components/LabWorkspace.test.tsx`: Testar que os textos mudam para inglês quando o contexto está em `en`.
2. **Backend:**
   - `AiAssessmentControllerTest` / `ApiIntegrationTest`: Testar que submeter `language = "es"` ou qualquer valor não suportado retorna **HTTP 400 Bad Request** com mensagem descritiva de erro.
   - `AiAssessmentServiceTest.java`: Testar que uma requisição com `language = "en"` gera prompt e resposta de validação em inglês.
3. **Commit Red:**
   `git commit --author="gsj-agent-bot[bot] <5128871+gsj-agent-bot[bot]@users.noreply.github.com>" -m "test(i18n): adicionar testes automatizados de traducao, chaveamento, validacao de idioma e prompt em ingles"`

### Etapa 3: Green Stage (Implementação mínima para fazer passar)
1. Criar arquivos `src/i18n/types.ts`, `src/i18n/translations.ts` e `src/i18n/LanguageContext.tsx`.
2. Envolver `App.tsx` com `LanguageProvider`.
3. Adicionar o seletor `PT | EN` no `Navbar.tsx`.
4. Substituir os textos estáticos nos componentes pelo uso de `t.*`.
5. Criar `AssessmentLanguage.java` no backend e adicionar validação de borda no `AiAssessmentController.java` (retornando HTTP 400 se inválido).
6. Atualizar `AiAssessmentRequest.java` e `AiAssessmentService.java` para tratar `language`.
7. Enviar `language: locale` na chamada de avaliação em `LabWorkspace.tsx`.

7. **Commit Green:**
   `git commit --author="gsj-agent-bot[bot] <5128871+gsj-agent-bot[bot]@users.noreply.github.com>" -m "feat(i18n): implementar internacionalizacao completa com PT-BR padrao e EN (closes #6)"`

### Etapa 4: Validação de Qualidade
- `./mvnw test` (Backend): 100% de sucesso.
- `npm test` (Frontend): 100% de sucesso.
- `oxlint` e `tsc -b`: 0 avisos e 0 erros.

### Etapa 5: Push, PR e Desmontagem da Worktree
- Push da branch `feat/issue-6-i18n-support`.
- Gerar token com `gerar-token.cjs` e abrir Pull Request.
- Desmontar e podar a worktree.

---

## 5. Critérios de Aceitação (DoD)
- [ ] Interface 100% em Português do Brasil por padrão sem regressões visuais.
- [ ] Controle visual de alternância `PT | EN` na barra de navegação.
- [ ] Persistência da preferência de idioma do usuário no `localStorage`.
- [ ] Rótulos da estante, menu lateral, folha de laboratório e modais traduzidos para Inglês quando selecionado `EN`.
- [ ] Prompts e validações do Tutor de IA gerados no idioma selecionado.
- [ ] 100% dos testes de backend e frontend aprovados.
