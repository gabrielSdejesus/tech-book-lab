# Especificação: Tela Inicial com Seleção de Livros e Capas Editoriais (Bookshelf)

**Issue**: [#3 - FEAT: Adicionar tela inicial com seleção de livros e imagens de capa](https://github.com/gabrielSdejesus/tech-book-lab/issues/3)  
**Status**: Proposta para Aprovação  
**Branch**: `feat/issue-3-home-bookshelf`

---

## 1. Contexto e Motivação
Atualmente, a aplicação web inicia diretamente no primeiro laboratório do livro *Designing Data-Intensive Applications*, sem fornecer ao usuário uma visão geral da biblioteca técnica disponível ou a possibilidade de escolher qual livro explorar.

Esta funcionalidade introduz a **Bookshelf** (tela inicial/catálogo de livros técnicos), permitindo que o leitor:
1. Visualize os livros técnicos disponíveis na plataforma em formato de estante editorial.
2. Contemple a capa clássica estilizada (estilo editorial clássico com gravuras, molduras e paleta de cores temática).
3. Selecione um livro para ingressar no seu respectivo ambiente de laboratório (*Lab Workspace*).
4. Retorne à estante a qualquer momento através da barra de navegação (*Navbar*).

---

## 2. Modelagem de Domínio & Contratos

### 2.1 Backend (Java 21 / Spring Boot / JDBC / Flyway)
- **Tabela `books`**:
  - Nova migration Flyway `V3__add_cover_image_to_books.sql`:
    ```sql
    ALTER TABLE books ADD COLUMN cover_image_url VARCHAR(500);
    UPDATE books SET cover_image_url = '/covers/ddia.svg' WHERE id = 'ddia';
    ```
- **Entidade de Domínio `Book` (`com.dataintensive.lab.domain.Book`)**:
  ```java
  public record Book(
      String id,
      String title,
      String author,
      String tagLine,
      String coverColor,
      String coverImageUrl,
      String description,
      List<Chapter> chapters
  ) {}
  ```
- **Repositório `JdbcCatalogRepository`**:
  - Atualização do mapeamento SQL para incluir `cover_image_url`.
- **API REST `GET /api/books` e `GET /api/books/{id}`**:
  - O JSON de retorno passa a expor o campo `coverImageUrl`.

### 2.2 Frontend (React 19 / TypeScript / Tailwind CSS)
- **Tipagem `Book` (`src/types/index.ts`)**:
  ```typescript
  export interface Book {
    id: string;
    title: string;
    author: string;
    tagLine: string;
    coverColor: string;
    coverImageUrl?: string;
    description: string;
    chapters: Chapter[];
  }
  ```
- **Componente `Bookshelf` (`src/components/Bookshelf.tsx`)**:
  - Props:
    ```typescript
    interface BookshelfProps {
      books: Book[];
      onSelectBook: (book: Book) => void;
    }
    ```
  - Layout e Estética:
    - Hero / Masthead com tipografia clássica serifada, boas-vindas ao laboratório e contadores do acervo (livros, capítulos, laboratórios práticos).
    - Grid de cartões de livros técnicos com elevação de papel (`book-shadow-sm` e hover `book-shadow-lg`).
    - Exibição de capa editorial clássica com suporte a imagem SVG/PNG (`ddia.svg`) e fallback gracioso de capa tipográfica com ornamentos e gravura de animal (javali/engraving).
    - Resumo dos capítulos, contagem de laboratórios, tecnologias e botão de ação primária "Abrir Caderno de Laboratório".
- **Navegação no `App.tsx` e `Navbar.tsx`**:
  - Estado de visualização `currentView`: `'bookshelf' | 'workspace'`.
  - Estado do livro selecionado `selectedBook`: `Book | null`.
  - Ao entrar na aplicação, o padrão é a `Bookshelf` (`currentView = 'bookshelf'`).
  - Ao clicar em um livro, define `selectedBook`, seleciona o primeiro laboratório disponível do livro e altera `currentView = 'workspace'`.
  - Na `Navbar`: inclusão de botão/link de acesso rápido "Estante de Livros" (ou clique no logotipo da aplicação) para alternar de volta à `Bookshelf`.

---

## 3. Estratégia de Testes (TDD Seams)

### Seams Backend:
1. `CatalogRepositoryTest`:
   - Validar que a consulta de livros persiste e recupera o novo campo `coverImageUrl`.
2. `CatalogServiceTest`:
   - Validar que o serviço repassa a entidade `Book` contendo `coverImageUrl`.
3. `ApiIntegrationTest`:
   - Endpoint `GET /api/books`: validar que o payload retornado inclui `coverImageUrl: "/covers/ddia.svg"`.
   - Endpoint `GET /api/books/{id}`: validar que o livro individual contém `coverImageUrl`.

### Seams Frontend:
1. `Bookshelf.test.tsx`:
   - Renderiza a lista de livros recebida via props.
   - Exibe títulos, autores, descrição e capa do livro.
   - Dispara `onSelectBook` com o livro correto ao clicar no card ou botão de seleção.
2. `Navbar.test.tsx`:
   - Renderiza o botão de retorno à estante quando em modo laboratório.
   - Dispara o callback `onNavigateBookshelf`.
3. `App.test.tsx` (ou testes de integração de fluxo):
   - Inicia exibindo a Bookshelf com os livros carregados da API.
   - Ao selecionar um livro, transiciona para o `LabWorkspace`.
   - Ao clicar no botão da estante na Navbar, retorna para a Bookshelf.

---

## 4. Plano de Commits Atômicos (Conventional Commits)

1. `test(catalog): adicionar testes para coverImageUrl em Book (backend)`
2. `feat(catalog): adicionar migration v3 e campo coverImageUrl no backend`
3. `test(bookshelf): criar testes unitários do componente Bookshelf e capa editorial`
4. `feat(bookshelf): implementar componente Bookshelf e arte vetorial clássica da capa`
5. `test(navigation): adicionar testes para navegação entre Bookshelf e LabWorkspace`
6. `feat(navigation): integrar Bookshelf no App e botão de estante na Navbar`
7. `docs(issue-3): registrar GLOSSARY e especificação da funcionalidade`
