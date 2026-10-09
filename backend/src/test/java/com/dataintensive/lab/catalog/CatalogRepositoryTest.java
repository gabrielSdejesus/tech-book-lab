package com.dataintensive.lab.catalog;

import com.dataintensive.lab.domain.Book;
import com.dataintensive.lab.domain.Chapter;
import com.dataintensive.lab.domain.EngineType;
import com.dataintensive.lab.domain.Lab;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CatalogRepositoryTest {

    @Autowired
    private CatalogRepository catalogRepository;

    @Test
    @DisplayName("Deve carregar todos os livros do banco de dados com capítulos e laboratórios aninhados")
    void shouldFindAllBooksWithNestedHierarchy() {
        List<Book> books = catalogRepository.findAllBooks();

        assertThat(books).isNotEmpty();
        Book ddia = books.stream().filter(b -> b.id().equals("ddia")).findFirst().orElseThrow();
        assertThat(ddia.title()).isEqualTo("Designing Data-Intensive Applications");
        assertThat(ddia.author()).isEqualTo("Martin Kleppmann");
        assertThat(ddia.coverImageUrl()).isEqualTo("/covers/ddia.svg");
        assertThat(ddia.chapters()).isNotEmpty();

        Chapter ch3 = ddia.chapters().get(0);
        assertThat(ch3.number()).isEqualTo(3);
        assertThat(ch3.title()).isEqualTo("Modelos de Dados e Linguagens de Consulta");
        assertThat(ch3.labs()).hasSize(4);
    }

    @Test
    @DisplayName("Deve buscar livro por ID existente no banco de dados")
    void shouldFindBookById() {
        Optional<Book> book = catalogRepository.findBookById("ddia");

        assertThat(book).isPresent();
        assertThat(book.get().id()).isEqualTo("ddia");
        assertThat(book.get().chapters()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar livro inexistente")
    void shouldReturnEmptyForUnknownBook() {
        Optional<Book> book = catalogRepository.findBookById("livro-fantasma");

        assertThat(book).isEmpty();
    }

    @Test
    @DisplayName("Deve buscar laboratório por ID com desafios, guidelines e conceitos")
    void shouldFindLabById() {
        Optional<Lab> lab = catalogRepository.findLabById("ddia-cap-03-lab-01");

        assertThat(lab).isPresent();
        assertThat(lab.get().engineType()).isEqualTo(EngineType.POSTGRES);
        assertThat(lab.get().databaseName()).isEqualTo("tbl_lab");
        assertThat(lab.get().keyConcepts()).contains("Impedance Mismatch");
        assertThat(lab.get().challenges()).hasSize(2);
        assertThat(lab.get().challenges().get(0).guidelines()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve buscar laboratório por slug")
    void shouldFindLabBySlug() {
        Optional<Lab> lab = catalogRepository.findLabById("relacional-vs-documentos");

        assertThat(lab).isPresent();
        assertThat(lab.get().id()).isEqualTo("ddia-cap-03-lab-01");
    }

    @Test
    @DisplayName("Deve garantir que todos os templates SQL de criação de tabela contenham a cláusula IF NOT EXISTS")
    void shouldContainIdempotentIfNotExistsInStarterTemplates() {
        List<Book> books = catalogRepository.findAllBooks();
        assertThat(books).isNotEmpty();

        List<com.dataintensive.lab.domain.Challenge> allChallenges = books.stream()
                .flatMap(b -> b.chapters().stream())
                .flatMap(c -> c.labs().stream())
                .flatMap(l -> l.challenges().stream())
                .toList();

        // Desafio 1 Lab 1: usuarios
        var ch1 = allChallenges.stream().filter(c -> c.id().equals("lab-01-ch-1")).findFirst().orElseThrow();
        assertThat(ch1.starterTemplate()).contains("CREATE TABLE IF NOT EXISTS usuarios");

        // Desafio 2 Lab 1: usuarios_documento
        var ch2 = allChallenges.stream().filter(c -> c.id().equals("lab-01-ch-2")).findFirst().orElseThrow();
        assertThat(ch2.starterTemplate()).contains("CREATE TABLE IF NOT EXISTS usuarios_documento");

        // Desafio 2 Lab 2: locais
        var ch3 = allChallenges.stream().filter(c -> c.id().equals("lab-02-ch-2")).findFirst().orElseThrow();
        assertThat(ch3.starterTemplate()).contains("CREATE TABLE IF NOT EXISTS locais");

        // Desafio 1 Lab 3: dim_tempo
        var ch4 = allChallenges.stream().filter(c -> c.id().equals("lab-03-ch-1")).findFirst().orElseThrow();
        assertThat(ch4.starterTemplate()).contains("CREATE TABLE IF NOT EXISTS dim_tempo");

        // Desafio 1 Lab 4: pedidos_eventos
        var ch5 = allChallenges.stream().filter(c -> c.id().equals("lab-04-ch-1")).findFirst().orElseThrow();
        assertThat(ch5.starterTemplate()).contains("CREATE TABLE IF NOT EXISTS pedidos_eventos");

        // Desafio 2 Lab 4: pedidos_resumo_leitura
        var ch6 = allChallenges.stream().filter(c -> c.id().equals("lab-04-ch-2")).findFirst().orElseThrow();
        assertThat(ch6.starterTemplate()).contains("CREATE TABLE IF NOT EXISTS pedidos_resumo_leitura");
    }

    @Test
    @DisplayName("Deve carregar catálogo com traduções em inglês quando locale for 'en'")
    void shouldFindAllBooksWithEnglishTranslationsWhenLocaleIsEn() {
        List<Book> books = catalogRepository.findAllBooks("en");

        assertThat(books).isNotEmpty();
        Book ddia = books.stream().filter(b -> b.id().equals("ddia")).findFirst().orElseThrow();
        assertThat(ddia.tagLine()).isEqualTo("The definitive guide to architecting distributed, reliable, and scalable systems.");
        assertThat(ddia.description()).isEqualTo("Learn in practice the fundamental trade-offs behind database engines, indexes, replication, partitioning, and consistency.");

        Chapter ch3 = ddia.chapters().get(0);
        assertThat(ch3.title()).isEqualTo("Data Models and Query Languages");
        assertThat(ch3.subtitle()).isEqualTo("Data Models, Graphs, OLAP and CQRS");
        assertThat(ch3.summary()).contains("Explore the foundational structures");

        Lab lab1 = ch3.labs().get(0);
        assertThat(lab1.title()).isEqualTo("Relational vs Document and Storage Locality");
        assertThat(lab1.summary()).contains("Analyze object-relational impedance mismatch");
        assertThat(lab1.keyConcepts()).contains("3NF Normalization", "Storage Locality");

        var ch1 = lab1.challenges().get(0);
        assertThat(ch1.title()).isEqualTo("3NF Modeling (Strict Relational)");
        assertThat(ch1.description()).contains("Create the 3NF model for a professional profile");
        assertThat(ch1.scenario()).contains("Each user has a name, bio");
        assertThat(ch1.reflectionPrompt()).contains("If a company changes its legal name");
        assertThat(ch1.expectedReflection()).contains("In the 3NF model");
        assertThat(ch1.guidelines()).contains("Model coherent primary and foreign keys");
    }

    @Test
    @DisplayName("Deve manter textos em português quando locale for 'pt' ou nulo")
    void shouldKeepPortugueseWhenLocaleIsPtOrNull() {
        List<Book> books = catalogRepository.findAllBooks("pt");
        Book ddia = books.stream().filter(b -> b.id().equals("ddia")).findFirst().orElseThrow();
        assertThat(ddia.tagLine()).isEqualTo("O guia definitivo para arquitetar sistemas distribuídos, confiáveis e escaláveis.");

        Chapter ch3 = ddia.chapters().get(0);
        assertThat(ch3.title()).isEqualTo("Modelos de Dados e Linguagens de Consulta");

        Lab lab1 = ch3.labs().get(0);
        var ch1 = lab1.challenges().get(0);
        assertThat(ch1.expectedReflection()).contains("No modelo 3NF");
    }

    @Test
    @DisplayName("Deve buscar laboratório com textos traduzidos em inglês")
    void shouldFindLabByIdInEnglish() {
        Optional<Lab> labOpt = catalogRepository.findLabById("ddia-cap-03-lab-01", "en");

        assertThat(labOpt).isPresent();
        Lab lab = labOpt.get();
        assertThat(lab.title()).isEqualTo("Relational vs Document and Storage Locality");
        assertThat(lab.challenges().get(0).title()).isEqualTo("3NF Modeling (Strict Relational)");
    }

    @Test
    @DisplayName("Deve garantir que todos os desafios retornados por findAllBooks e findLabById contenham expectedReflection populado em PT e EN")
    void shouldEnsureAllChallengesHaveExpectedReflectionPopulatedInPtAndEn() {
        for (String locale : List.of("pt", "en")) {
            List<Book> books = catalogRepository.findAllBooks(locale);
            assertThat(books).isNotEmpty();

            List<com.dataintensive.lab.domain.Challenge> challenges = books.stream()
                    .flatMap(b -> b.chapters().stream())
                    .flatMap(c -> c.labs().stream())
                    .flatMap(l -> l.challenges().stream())
                    .toList();

            assertThat(challenges).isNotEmpty();
            for (var ch : challenges) {
                assertThat(ch.expectedReflection())
                        .as("Challenge %s deve possuir expectedReflection não nulo e não em branco no locale %s", ch.id(), locale)
                        .isNotNull()
                        .isNotBlank();
            }

            Optional<Lab> labOpt = catalogRepository.findLabById("ddia-cap-03-lab-01", locale);
            assertThat(labOpt).isPresent();
            for (var ch : labOpt.get().challenges()) {
                assertThat(ch.expectedReflection())
                        .as("Challenge %s de findLabById deve possuir expectedReflection no locale %s", ch.id(), locale)
                        .isNotNull()
                        .isNotBlank();
            }
        }
    }

    @Test
    @DisplayName("Deve garantir que todos os starter templates contenham apenas código executável e nenhum comentário")
    void shouldNotContainCommentsInStarterTemplates() {
        List<Book> books = catalogRepository.findAllBooks();
        assertThat(books).isNotEmpty();

        List<com.dataintensive.lab.domain.Challenge> allChallenges = books.stream()
                .flatMap(b -> b.chapters().stream())
                .flatMap(c -> c.labs().stream())
                .flatMap(l -> l.challenges().stream())
                .toList();

        for (var challenge : allChallenges) {
            String template = challenge.starterTemplate();
            assertThat(template)
                    .as("Desafio %s não deve conter comentários SQL ou Cypher", challenge.id())
                    .doesNotContain("--")
                    .doesNotContain("//")
                    .doesNotContain("/*")
                    .doesNotContain("*/");
        }
    }

    @Test
    @DisplayName("Deve salvar, atualizar e recuperar a solução do usuário em um desafio preservando starterTemplate")
    void shouldSaveAndUpdateAndRetrieveChallengeSolution() {
        String challengeId = "lab-01-ch-1";
        String mySql1 = "SELECT id, nome FROM usuarios WHERE id = 1;";
        catalogRepository.saveChallengeSolution(challengeId, mySql1);

        Optional<Lab> labOpt = catalogRepository.findLabById("ddia-cap-03-lab-01");
        assertThat(labOpt).isPresent();
        var ch1 = labOpt.get().challenges().stream()
                .filter(c -> c.id().equals(challengeId))
                .findFirst()
                .orElseThrow();

        assertThat(ch1.savedCode()).isEqualTo(mySql1);
        assertThat(ch1.starterTemplate()).contains("CREATE TABLE IF NOT EXISTS usuarios");

        // Atualizar solução
        String mySql2 = "SELECT * FROM usuarios WHERE ativo = true;";
        catalogRepository.saveChallengeSolution(challengeId, mySql2);

        Optional<Lab> labOpt2 = catalogRepository.findLabById("ddia-cap-03-lab-01");
        var ch1Updated = labOpt2.orElseThrow().challenges().stream()
                .filter(c -> c.id().equals(challengeId))
                .findFirst()
                .orElseThrow();
        assertThat(ch1Updated.savedCode()).isEqualTo(mySql2);
    }

    @Test
    @DisplayName("Deve remover a solução salva do desafio mantendo o starterTemplate intacto")
    void shouldDeleteChallengeSolutionAndRestoreNullSavedCode() {
        String challengeId = "lab-01-ch-2";
        catalogRepository.saveChallengeSolution(challengeId, "SELECT * FROM usuarios_documento;");

        catalogRepository.deleteChallengeSolution(challengeId);

        Optional<Lab> labOpt = catalogRepository.findLabById("ddia-cap-03-lab-01");
        var ch2 = labOpt.orElseThrow().challenges().stream()
                .filter(c -> c.id().equals(challengeId))
                .findFirst()
                .orElseThrow();

        assertThat(ch2.savedCode()).isNull();
        assertThat(ch2.starterTemplate()).contains("CREATE TABLE IF NOT EXISTS usuarios_documento");
    }

    @Test
    @DisplayName("Deve carregar savedCode através de findAllBooks quando existir solução salva")
    void shouldLoadSavedCodeInFindAllBooks() {
        String challengeId = "lab-02-ch-2";
        String mySql = "SELECT * FROM locais;";
        catalogRepository.saveChallengeSolution(challengeId, mySql);

        List<Book> books = catalogRepository.findAllBooks();
        var ch = books.stream()
                .flatMap(b -> b.chapters().stream())
                .flatMap(c -> c.labs().stream())
                .flatMap(l -> l.challenges().stream())
                .filter(c -> c.id().equals(challengeId))
                .findFirst()
                .orElseThrow();

        assertThat(ch.savedCode()).isEqualTo(mySql);

        // cleanup
        catalogRepository.deleteChallengeSolution(challengeId);
    }

    @Test
    @DisplayName("Deve salvar e recuperar resposta do questionário de trade-off (userReflection) do desafio")
    void shouldSaveAndLoadUserReflectionInChallengeSolution() {
        String challengeId = "lab-01-ch-1";
        String mySql = "SELECT * FROM usuarios;";
        String myReflection = "No modelo 3NF, a consistência é preservada por chaves estrangeiras com baixo custo de escrita.";

        catalogRepository.saveChallengeSolution(challengeId, mySql, myReflection);

        Optional<Lab> labOpt = catalogRepository.findLabById("ddia-cap-03-lab-01");
        var ch = labOpt.orElseThrow().challenges().stream()
                .filter(c -> c.id().equals(challengeId))
                .findFirst()
                .orElseThrow();

        assertThat(ch.savedCode()).isEqualTo(mySql);
        assertThat(ch.savedReflection()).isEqualTo(myReflection);

        // cleanup
        catalogRepository.deleteChallengeSolution(challengeId);
    }
}

