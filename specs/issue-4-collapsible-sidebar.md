# Especificação: Menu Lateral Retrátil (Sidebar Toggle)

**Issue**: [#4 - FEAT: Menu lateral retrátil ao clicar no ícone do livro para expandir área de leitura](https://github.com/gabrielSdejesus/tech-book-lab/issues/4)  
**Status**: Proposta para Aprovação  
**Branch**: `feat/issue-4-collapsible-sidebar`

---

## 1. Contexto e Motivação
No ambiente de estudos práticos (*Lab Workspace*), os usuários analisam enunciados teóricos, escrevem consultas em SQL/Cypher e inspecionam tabelas de resultados de dados. Atualmente, a barra lateral de capítulos (*Sidebar / Tábua de Matérias*) ocupa uma largura fixa de `20rem` (`w-80`), limitando o espaço horizontal para o editor e para a visualização de colunas de resultados.

Esta funcionalidade introduz a capacidade de recolher e expandir o menu lateral tanto pelo ícone do livro no cabeçalho (*Navbar*) quanto por botão dedicado de recolhimento no topo da própria *Sidebar*, maximizando a área de leitura e estudo (*Reading Focus Mode*).

---

## 2. Contratos de Componentes e Interfaces

### 2.1 Componente `Navbar` (`src/components/Navbar.tsx`)
- **Novas Props**:
  ```typescript
  interface Props {
    // ... props existentes ...
    isSidebarOpen?: boolean;
    onToggleSidebar?: () => void;
  }
  ```
- **Comportamento**:
  - Quando no modo de estudos (`!isBookshelfActive`), o ícone do livro no cabeçalho funciona como um botão acessível de alternância da barra lateral:
    - `title`: "Recolher tábua de matérias" quando aberta, "Expandir tábua de matérias" quando recolhida.
    - `aria-label`: "Alternar tábua de matérias".
    - `aria-expanded`: valor booleano de `isSidebarOpen`.
  - Ao clicar, invoca `onToggleSidebar`.
  - O texto "Tech Book Lab" e o botão "ESTANTE DE LIVROS" continuam navegando para a tela de catálogo inicial.

### 2.2 Componente `Sidebar` (`src/components/Sidebar.tsx`)
- **Novas Props**:
  ```typescript
  interface Props {
    books: Book[];
    selectedLab: Lab | null;
    onSelectLab: (lab: Lab) => void;
    isOpen?: boolean;
    onToggle?: () => void;
  }
  ```
- **Comportamento & Estilo**:
  - Quando `isOpen === true`: largura normal (`w-80`), borda visível e conteúdo renderizado.
  - Quando `isOpen === false`: largura recolhida (`w-0`), `overflow-hidden`, borda zerada ou oculta, permitindo que o `LabWorkspace` ocupe 100% da largura útil da tela sem quebras de layout.
  - Transição suave via CSS (`transition-all duration-300 ease-in-out`).
  - Botão de recolhimento dedicado no cabeçalho da barra lateral ao lado de "TÁBUA DE MATÉRIAS" (`PanelLeftClose` / ícone de chevron).

### 2.3 Estado e Persistência no `App.tsx` (`src/App.tsx`)
- **Estado**:
  - `isSidebarOpen: boolean`, inicializado verificando `localStorage.getItem('tbl_sidebar_open') !== 'false'` (padrão: expandido/aberto).
- **Função de Alternância**:
  - `toggleSidebar()`: inverte o estado e persiste `localStorage.setItem('tbl_sidebar_open', String(novoEstado))`.
- Passagem do estado e do manipulador para a `Navbar` e para a `Sidebar`.

---

## 3. Estratégia de Testes (TDD Seams)

### Seam 1: `Sidebar.test.tsx`
1. Validar que quando `isOpen={false}`, a `Sidebar` aplica as classes ou atributos de recolhimento (`w-0`, `overflow-hidden`).
2. Validar que o botão dedicado de alternância na barra lateral chama `onToggle`.

### Seam 2: `Navbar.test.tsx`
1. Validar que em modo workspace, o ícone do livro dispara `onToggleSidebar` ao ser clicado.
2. Validar acessibilidade (`aria-expanded` reflete `isSidebarOpen`).

### Seam 3: `App.test.tsx`
1. Validar que o usuário pode alternar o menu lateral ao clicar no botão do ícone do livro no cabeçalho.
2. Validar que a preferência do usuário é persistida no `localStorage`.

---

## 4. Plano de Commits Atômicos

1. `test(sidebar): adicionar testes para estado recolhido e botão de alternancia na Sidebar`
2. `feat(sidebar): implementar suporte a estado recolhido e botao de alternancia na Sidebar`
3. `test(navbar): adicionar testes para alternancia do menu pelo icone do livro na Navbar`
4. `feat(navbar): implementar botao de alternancia no icone do livro da Navbar`
5. `test(app): adicionar testes de integracao para alternancia do menu e persistencia no localStorage`
6. `feat(app): integrar alternancia do menu lateral e persistencia no localStorage (closes #4)`
7. `docs(issue-4): registrar especificacao e atualizar glossario`
