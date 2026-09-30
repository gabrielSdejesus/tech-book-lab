# Especificação: Affordance Visual de Elementos Interativos, Cursor Pointer e Acessibilidade (UI/UX)

**Issue**: [#20 - FEAT: Affordance visual de elementos interativos, cursor pointer e acessibilidade (UI/UX)](https://github.com/gabrielSdejesus/tech-book-lab/issues/20)  
**Status**: Proposta para Aprovação Humana  
**Branch**: `feat/issue-20-interactive-affordance`

---

## 1. Contexto e Motivação

No Tailwind CSS v4, a folha de estilos básica (*preflight*) intencionalmente não impõe `cursor: pointer` aos elementos nativos `<button>` e links sem `href`. Isso transfere a responsabilidade da declaração do cursor para os estilos da aplicação. Como consequência, na plataforma **Tech Book Lab**, diversos botões, abas, cartões de livros e controles modais apresentam o cursor padrão de texto/seta (`default`), reduzindo drasticamente o feedback tátil e visual de **affordance** (percepção imediata de clicabilidade).

Além disso, elementos interativos que atuam como botões mas são representados por contêineres `div` (como o cartão do livro na Estante e a marca no cabeçalho) necessitam de atributos semânticos (`role="button"`, `tabIndex={0}`) e manipuladores de eventos de teclado (`Enter` e `Space`), garantindo conformidade com padrões de acessibilidade WCAG 2.1 AA.

Esta especificação define:
1. **Regras Globais na Camada Base (`@layer base`)**: Fornecimento de cursores semânticos para todos os elementos clicáveis (`cursor: pointer`) e desabilitados (`cursor: not-allowed`) no `src/index.css`.
2. **Auditoria e Padronização Componente a Componente**:
   - `Navbar`: cursor pointer em botões, alternadores e containers da marca, com ativação por teclado.
   - `Sidebar`: botões de laboratórios e botão retrátil com `cursor-pointer`, `focus-visible` e estados ativos.
   - `Bookshelf`: cartões de livros com `role="button"`, `tabIndex={0}`, affordance de hover, elevação e ativação por `Enter`/`Space`.
   - `LabWorkspace`: abas de exercícios e resultados com `cursor-pointer`, e botões de ação ("Executar", "Submeter", "Restaurar") com `disabled:cursor-not-allowed`.
   - `AiSettingsModal`: botões de teste, seleção de provedores, fechar e salvar com affordances claros e estados desabilitados.
3. **Garantia de Não-Interferência no Backend**: Preservação total dos contratos e rotas da API REST.

---

## 2. Modelagem Arquitetural & Contratos de Interface

### 2.1 Camada Base Global de CSS (`src/index.css`)
Adição de declarações obrigatórias dentro de `@layer base`:

```css
@layer base {
  /* ... variáveis e body existentes ... */

  /* Affordance Global para Elementos Interativos */
  button:not(:disabled),
  [role="button"]:not([aria-disabled="true"]),
  select,
  summary,
  a {
    cursor: pointer;
  }

  /* Affordance Global para Elementos Desabilitados */
  button:disabled,
  [role="button"][aria-disabled="true"],
  input:disabled,
  select:disabled,
  textarea:disabled {
    cursor: not-allowed;
  }
}
```

### 2.2 Componente `Navbar` (`src/components/Navbar.tsx`)
- **Brand / Masthead clicável**:
  - Quando `onNavigateBookshelf` estiver presente:
    - Atributos: `role="button"`, `tabIndex={0}`, `aria-label={t.common.goToBookshelf}`.
    - Eventos: `onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onNavigateBookshelf(); } }}`.
    - Classes de foco: `focus-visible:outline-2 focus-visible:outline-[#8f1d1d] focus-visible:outline-offset-2 dark:focus-visible:outline-[#df4444]`.
- **Botão de Refresh de Infraestrutura**:
  - Quando `loadingInfra === true` ou `disabled`: classe explícita `disabled:cursor-not-allowed disabled:opacity-50`.
- **Botões de Ação ("Retornar à Estante", "Tema", "Configurar Tutor IA", "PT | EN")**:
  - Garantia de `cursor-pointer` explícito e feedback de active via `.book-shadow-pressed`.

### 2.3 Componente `Bookshelf` (`src/components/Bookshelf.tsx`)
- **Cartão do Livro (`div` de container do livro)**:
  - Atributos semânticos: `role="button"`, `tabIndex={0}`, `aria-label={book.title}`.
  - Evento de teclado: `onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onSelectBook(book); } }}`.
  - Classes visuais: `cursor-pointer`, `hover:border-stone-900 dark:hover:border-stone-500`, `focus-visible:ring-2 focus-visible:ring-[#8f1d1d] focus-visible:outline-none dark:focus-visible:ring-[#df4444]`.
- **Botão de Ação "Abrir Caderno de Laboratório"**:
  - `cursor-pointer`, transição de clique e supressão de propagação de evento (`e.stopPropagation()`).

### 2.4 Componente `Sidebar` (`src/components/Sidebar.tsx`)
- **Itens de Laboratório (`button`)**:
  - `cursor-pointer`, `focus-visible:outline-2 focus-visible:outline-stone-900 dark:focus-visible:outline-stone-100`.
  - Diferenciação nítida de estado: item ativo possui destaque escarlate (`border-l-[#8f1d1d]`), item inativo possui hover suave (`hover:bg-[#ede7da] dark:hover:bg-[#201e1b]`).
- **Botão de Recolher/Expandir**:
  - `cursor-pointer`, hover visual e `title`/`aria-label` dinâmico.

### 2.5 Componente `LabWorkspace` (`src/components/LabWorkspace.tsx`)
- **Abas de Seleção de Exercício (`Exercício 1`, `Exercício 2`)**:
  - `cursor-pointer`, contraste no hover quando inativo.
- **Botões de Ação na Toolbar ("Recarregar Template", "Executar")**:
  - "Recarregar Template": `cursor-pointer hover:bg-[#ded7c8] dark:hover:bg-[#2a2723]`.
  - "Executar": `cursor-pointer hover:bg-[#33302e] disabled:cursor-not-allowed disabled:opacity-50`.
- **Botões no Rodapé das Instruções ("Submeter ao Tutor IA", "Restaurar")**:
  - "Submeter ao Tutor IA": `cursor-pointer hover:bg-[#771818] disabled:cursor-not-allowed disabled:opacity-50`.
  - "Restaurar": `cursor-pointer hover:bg-[#ded7c8] disabled:cursor-not-allowed disabled:opacity-50`.
- **Abas do Painel de Saída ("Resultados", "Parecer do Tutor IA", "JSON")**:
  - `cursor-pointer`, `focus-visible:outline-none`.

### 2.6 Componente `AiSettingsModal` (`src/components/AiSettingsModal.tsx`)
- **Botão Fechar (`X`)**: `cursor-pointer hover:bg-[#ded7c8]`.
- **Botões de Provedor (Gemini / Ollama)**: `cursor-pointer hover:bg-[#e6e0d3]`.
- **Botões de Teste de Conexão**: `cursor-pointer disabled:cursor-not-allowed disabled:opacity-40`.
- **Botões de Rodapé ("Cancelar", "Salvar Configurações")**: `cursor-pointer hover:opacity-90`.

---

## 3. Plano de Testes Automatizados (Ciclo TDD)

### 3.1 Testes de Estilos Globais (`src/index.css`)
- Criação de suíte de testes de affordance em `src/index.css.test.ts` (ou teste de integração DOM em `src/utils/theme.test.ts` / componente representativo):
  - Validação estática / computada de regras para `button`, `[role="button"]`, `select` e estados `:disabled`.

### 3.2 Testes de Componentes Frontend (Vitest + Testing Library)
- **`Navbar.test.tsx`**:
  - Teste de navegação e acionamento por teclado (`fireEvent.keyDown(brand, { key: 'Enter' })` e `{ key: ' ' }`).
  - Teste de `cursor-not-allowed` no botão de refresh quando desabilitado.
- **`Bookshelf.test.tsx`**:
  - Teste de `role="button"`, `tabIndex={0}` nos cartões de livro.
  - Teste de disparo de `onSelectBook` via teclado (`Enter` e `Space`).
- **`Sidebar.test.tsx`**:
  - Teste de affordance e clique nos botões de laboratório.
- **`LabWorkspace.test.tsx`**:
  - Teste de estados habilitados/desabilitados nos botões de execução e avaliação com classes de cursor.
- **`AiSettingsModal.test.tsx`**:
  - Teste de affordance no botão de teste desabilitado quando a API Key estiver vazia.

### 3.3 Testes de Não-Regressão do Backend (JUnit 5 + Spring Boot)
- Execução de `./mvnw test` garantindo 100% de sucesso nos 36 testes da suíte do backend.

---

## 4. Critérios de Aceitação (Definition of Done)

- [ ] Regras globais de `cursor: pointer` e `cursor: not-allowed` declaradas no `src/index.css`.
- [ ] Cartões de livro e containers clicáveis enriquecidos com `role="button"`, `tabIndex={0}` e ativação por `Enter`/`Space`.
- [ ] 100% dos botões e elementos interativos com feedback de `hover`, `active` e classes visuais de cursor consistentes.
- [ ] Suíte de testes frontend (`npm test`) com 100% de aprovação (mínimo de 52+ testes passando).
- [ ] Suíte de testes backend (`./mvnw test`) com 100% de aprovação (36/36 testes passando).
- [ ] `tsc -b && vite build` e `oxlint` com zero falhas e zero erros de tipo.
- [ ] Operação isolada em Git Worktree (`.worktrees/feat-issue-20-interactive-affordance`).
- [ ] Commits atômicos no padrão Conventional Commits com autor do bot (`gsj-agent-bot[bot]`).
- [ ] Pull Request aberto no GitHub referenciando `closes #20`.
